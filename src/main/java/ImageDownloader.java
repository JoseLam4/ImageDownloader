import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ImageDownloader
 * Aplicación de consola en Java para la descarga de imágenes de páginas web
 * mediante URL, URLConnection y HttpURLConnection de la API estándar de Java.
 *
 * Asignatura: Desarrollo de Sistemas III
 * Estudiante: José Carlos Tirado Lam (Matrícula: 225213509)
 * Licenciatura: Ingeniería en Sistemas de Información
 * División: Ciencias e Ingeniería - Universidad de Sonora
 *
 * @author José Carlos Tirado Lam
 * @version 1.0
 */
public class ImageDownloader {

    public static final int CONNECT_TIMEOUT_MS = 10000;
    public static final int READ_TIMEOUT_MS = 15000;
    public static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB máximo por imagen
    public static final String DEFAULT_USER_AGENT = "ImageDownloader/1.0 (Java Standard Library; UNISON)";
    public static final String DEFAULT_OUTPUT_DIR = "./imagenes";
    public static final String CSV_FILENAME = "descargas.csv";

    /**
     * Registro con el resultado de la descarga de cada imagen para el reporte CSV.
     */
    public static class RegistroDescarga {
        private final String urlImagen;
        private final String archivoLocal;
        private final String estado;
        private final long tamanoBytes;

        public RegistroDescarga(String urlImagen, String archivoLocal, String estado, long tamanoBytes) {
            this.urlImagen = urlImagen;
            this.archivoLocal = archivoLocal;
            this.estado = estado;
            this.tamanoBytes = tamanoBytes;
        }

        public String getUrlImagen() { return urlImagen; }
        public String getArchivoLocal() { return archivoLocal; }
        public String getEstado() { return estado; }
        public long getTamanoBytes() { return tamanoBytes; }
    }

    /**
     * Valida el argumento recibido y construye un objeto URL verificando
     * protocolo permitido (HTTP/HTTPS) y existencia de un host válido.
     *
     * @param argumento Cadena con la URL a validar
     * @return Objeto URL listo para su uso
     * @throws IllegalArgumentException si la URL es nula, vacía, con protocolo no admitido o sin host
     * @throws MalformedURLException si no es posible convertir la URI a URL
     */
    public static URL validarURL(String argumento) throws IllegalArgumentException, MalformedURLException {
        if (argumento == null || argumento.isBlank()) {
            throw new IllegalArgumentException("La URL proporcionada no puede estar vacía.");
        }

        URI uri = URI.create(argumento.trim());
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("Protocolo no admitido: " + scheme + ". Solo se admiten protocolos HTTP y HTTPS.");
        }

        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("La URL debe especificar un servidor/host válido.");
        }

        return uri.toURL();
    }

    /**
     * Establece una conexión HttpURLConnection con la URL provista, verifica el código de
     * estado HTTP, extrae la codificación de caracteres y recupera el documento HTML completo.
     *
     * @param url Dirección de la página web a consultar
     * @return Contenido HTML de la página en formato String
     * @throws IOException si ocurre un error de conexión, lectura o código de estado HTTP no exitoso
     */
    public static String obtenerHTML(URL url) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", DEFAULT_USER_AGENT);

            int codigo = connection.getResponseCode();
            System.out.println("Código HTTP: " + codigo);

            String contentType = connection.getContentType();
            System.out.println("Tipo de contenido: " + (contentType != null ? contentType : "desconocido"));

            if (codigo != HttpURLConnection.HTTP_OK) {
                throw new IOException("El servidor respondió con código HTTP de error: " + codigo + " (" + connection.getResponseMessage() + ")");
            }

            if (contentType != null && !contentType.toLowerCase().contains("text/html") && !contentType.toLowerCase().contains("application/xhtml")) {
                System.err.println("Advertencia: El tipo de contenido (" + contentType + ") no es 'text/html'.");
            }

            Charset charset = extraerCharset(contentType);

            StringBuilder htmlContent = new StringBuilder();
            try (InputStream input = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(input, charset))) {
                String linea;
                while ((linea = reader.readLine()) != null) {
                    htmlContent.append(linea).append(System.lineSeparator());
                }
            }

            return htmlContent.toString();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Extrae el juego de caracteres (charset) declarado en la cabecera Content-Type,
     * o retorna StandardCharsets.UTF_8 por defecto si no está especificado o no es válido.
     */
    private static Charset extraerCharset(String contentType) {
        if (contentType != null) {
            String[] partes = contentType.split(";");
            for (String parte : partes) {
                String parteTrim = parte.trim().toLowerCase();
                if (parteTrim.startsWith("charset=")) {
                    String nombreCharset = parteTrim.substring("charset=".length()).trim().replace("\"", "").replace("'", "");
                    try {
                        return Charset.forName(nombreCharset);
                    } catch (Exception ignored) {
                        // Fallback a UTF-8
                    }
                }
            }
        }
        return StandardCharsets.UTF_8;
    }

    /**
     * Identifica etiquetas <img> dentro del documento HTML y extrae los valores
     * de los atributos src utilizando expresiones regulares.
     * Utiliza LinkedHashSet para mantener el orden de aparición y descartar referencias duplicadas.
     *
     * @param html Contenido del documento HTML
     * @return Conjunto ordenado de referencias únicas a imágenes
     */
    public static Set<String> extraerImagenes(String html) {
        Set<String> imagenes = new LinkedHashSet<>();
        if (html == null || html.isBlank()) {
            return imagenes;
        }

        // Expresión para identificar etiquetas <img>
        Pattern imgPattern = Pattern.compile("<img\\b[^>]*>", Pattern.CASE_INSENSITIVE);
        // Expresión para extraer el atributo src con comillas dobles, simples o sin comillas
        Pattern srcPattern = Pattern.compile("\\bsrc\\s*=\\s*(?:([\"'])(.*?)\\1|([^\\s>]+))", Pattern.CASE_INSENSITIVE);

        Matcher imgMatcher = imgPattern.matcher(html);
        while (imgMatcher.find()) {
            String etiqueta = imgMatcher.group();
            Matcher srcMatcher = srcPattern.matcher(etiqueta);
            if (srcMatcher.find()) {
                String src = srcMatcher.group(2) != null ? srcMatcher.group(2) : srcMatcher.group(3);
                if (src != null && !src.isBlank()) {
                    imagenes.add(src.trim());
                }
            }
        }

        return imagenes;
    }

    /**
     * Resuelve una referencia de imagen (relativa o absoluta) con base en la URL de la página.
     * Rechaza esquemas no soportados (como data: o javascript:) y asegura protocolos HTTP/HTTPS.
     *
     * @param urlPagina URL base del documento HTML
     * @param referencia Cadena encontrada en el atributo src
     * @return Objeto URL absoluto y validado
     * @throws MalformedURLException si la dirección resultante es inválida
     * @throws IllegalArgumentException si la referencia utiliza un esquema no permitido o no tiene protocolo HTTP/HTTPS
     */
    public static URL resolverImagen(URL urlPagina, String referencia) throws MalformedURLException, IllegalArgumentException {
        if (referencia == null || referencia.isBlank()) {
            throw new IllegalArgumentException("La referencia de imagen está vacía.");
        }

        String refTrim = referencia.trim();
        if (refTrim.startsWith("data:") || refTrim.startsWith("javascript:") || refTrim.startsWith("mailto:") || refTrim.startsWith("#")) {
            throw new IllegalArgumentException("Esquema no admitido para descarga: " + (refTrim.length() > 30 ? refTrim.substring(0, 30) + "..." : refTrim));
        }

        @SuppressWarnings("deprecation")
        URL urlResuelta = new URL(urlPagina, refTrim);

        String protocolo = urlResuelta.getProtocol();
        if (protocolo == null || (!protocolo.equalsIgnoreCase("http") && !protocolo.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("Protocolo de imagen no admitido: " + protocolo);
        }

        return urlResuelta;
    }

    /**
     * Descarga un recurso de imagen binario desde la URL indicada y lo almacena
     * en el directorio especificado con nombre consecutivo.
     *
     * @param urlImagen URL remota de la imagen
     * @param directorio Carpeta local de almacenamiento
     * @param numero Índice consecutivo para la nomenclatura del archivo
     * @return true si la descarga fue exitosa, false en caso contrario
     */
    public static boolean descargarImagen(URL urlImagen, Path directorio, int numero) {
        return descargarImagen(urlImagen, directorio, numero, null);
    }

    /**
     * Sobrecarga de descargarImagen que registra el resultado en una lista para el reporte CSV.
     *
     * @param urlImagen URL remota de la imagen
     * @param directorio Carpeta local de almacenamiento
     * @param numero Índice consecutivo para la nomenclatura del archivo
     * @param registros Lista acumuladora de registros para exportación CSV (opcional)
     * @return true si la descarga fue exitosa, false en caso contrario
     */
    public static boolean descargarImagen(URL urlImagen, Path directorio, int numero, List<RegistroDescarga> registros) {
        HttpURLConnection connection = null;
        Path destino = null;
        String nombreOriginal = obtenerNombreRecurso(urlImagen);

        try {
            connection = (HttpURLConnection) urlImagen.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", DEFAULT_USER_AGENT);

            int codigo = connection.getResponseCode();
            if (codigo != HttpURLConnection.HTTP_OK) {
                String errorMsg = "Error HTTP " + codigo;
                System.out.printf("[%d] %-28s ... ERROR (%s)%n", numero, nombreOriginal, errorMsg);
                if (registros != null) {
                    registros.add(new RegistroDescarga(urlImagen.toString(), "—", errorMsg, 0));
                }
                return false;
            }

            String contentType = connection.getContentType();
            if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                String errorMsg = "Rechazado: no es imagen (" + (contentType != null ? contentType : "desconocido") + ")";
                System.out.printf("[%d] %-28s ... ERROR (%s)%n", numero, nombreOriginal, errorMsg);
                if (registros != null) {
                    registros.add(new RegistroDescarga(urlImagen.toString(), "—", errorMsg, 0));
                }
                return false;
            }

            long contentLength = connection.getContentLengthLong();
            if (contentLength > MAX_FILE_SIZE_BYTES) {
                String errorMsg = "Cancelado: supera límite de 10 MB (" + (contentLength / (1024 * 1024)) + " MB)";
                System.out.printf("[%d] %-28s ... ERROR (%s)%n", numero, nombreOriginal, errorMsg);
                if (registros != null) {
                    registros.add(new RegistroDescarga(urlImagen.toString(), "—", errorMsg, 0));
                }
                return false;
            }

            String extension = obtenerExtensionMime(contentType, urlImagen);
            String nombreArchivo = String.format("imagen_%03d%s", numero, extension);
            destino = directorio.resolve(nombreArchivo);

            // Requisito: No sobrescribir archivos existentes
            if (Files.exists(destino)) {
                String errorMsg = "Omitido: archivo existente (evitar sobrescritura)";
                System.out.printf("[%d] %-28s ... AVISO (%s: %s)%n", numero, nombreOriginal, errorMsg, nombreArchivo);
                if (registros != null) {
                    registros.add(new RegistroDescarga(urlImagen.toString(), nombreArchivo, errorMsg, Files.size(destino)));
                }
                return false;
            }

            // Descarga binaria con control de flujo y límite estricto de 10 MB
            long bytesLeidosTotal = 0;
            try (InputStream input = connection.getInputStream();
                 OutputStream output = Files.newOutputStream(destino, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                
                byte[] buffer = new byte[8192];
                int leidos;
                while ((leidos = input.read(buffer)) != -1) {
                    bytesLeidosTotal += leidos;
                    if (bytesLeidosTotal > MAX_FILE_SIZE_BYTES) {
                        throw new IOException("El archivo superó en transmisión el límite de 10 MB (" + bytesLeidosTotal + " bytes)");
                    }
                    output.write(buffer, 0, leidos);
                }
            } catch (Exception e) {
                // Eliminar archivo parcial si ocurrió algún fallo
                if (destino != null) {
                    try {
                        Files.deleteIfExists(destino);
                    } catch (IOException ignored) {}
                }
                throw e;
            }

            double kb = bytesLeidosTotal / 1024.0;
            System.out.printf("[%d] %-28s ... OK (%.1f KB) -> %s%n", numero, nombreOriginal, kb, nombreArchivo);

            if (registros != null) {
                registros.add(new RegistroDescarga(urlImagen.toString(), nombreArchivo, "Correcto", bytesLeidosTotal));
            }

            return true;

        } catch (Exception e) {
            System.out.printf("[%d] %-28s ... ERROR (%s)%n", numero, nombreOriginal, e.getMessage());
            if (registros != null) {
                registros.add(new RegistroDescarga(urlImagen.toString(), "—", "Error: " + e.getMessage(), 0));
            }
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Infiere la extensión del archivo a partir del Content-Type MIME o de la URL.
     */
    public static String obtenerExtensionMime(String contentType, URL url) {
        if (contentType != null) {
            String mime = contentType.toLowerCase().split(";")[0].trim();
            switch (mime) {
                case "image/png":
                    return ".png";
                case "image/jpeg":
                case "image/jpg":
                    return ".jpg";
                case "image/gif":
                    return ".gif";
                case "image/webp":
                    return ".webp";
                case "image/svg+xml":
                    return ".svg";
                case "image/bmp":
                case "image/x-ms-bmp":
                    return ".bmp";
                case "image/x-icon":
                case "image/vnd.microsoft.icon":
                    return ".ico";
                case "image/tiff":
                    return ".tiff";
                case "image/avif":
                    return ".avif";
            }
        }

        // Respaldo: intentar extraer la extensión desde la ruta de la URL
        String ruta = url.getPath();
        int punto = ruta.lastIndexOf('.');
        if (punto != -1 && punto > ruta.lastIndexOf('/')) {
            String sub = ruta.substring(punto).toLowerCase();
            if (sub.matches("\\.(png|jpe?g|gif|webp|svg|bmp|ico|tiff|avif)")) {
                return sub.equals(".jpeg") ? ".jpg" : sub;
            }
        }

        return ".img";
    }

    /**
     * Obtiene el nombre del recurso de la URL para fines informativos en consola.
     */
    public static String obtenerNombreRecurso(URL url) {
        String path = url.getPath();
        if (path == null || path.isEmpty() || path.endsWith("/")) {
            return "recurso_remoto";
        }
        int ultimoSlash = path.lastIndexOf('/');
        return (ultimoSlash != -1) ? path.substring(ultimoSlash + 1) : path;
    }

    /**
     * Genera el archivo descargas.csv con el reporte de la sesión de descargas.
     */
    public static void generarReporteCSV(Path rutaArchivoCsv, List<RegistroDescarga> registros) {
        try (BufferedWriter writer = Files.newBufferedWriter(rutaArchivoCsv, StandardCharsets.UTF_8)) {
            writer.write("URL de imagen,Archivo local,Estado,Tamaño (bytes)");
            writer.newLine();

            for (RegistroDescarga reg : registros) {
                String linea = String.format("\"%s\",\"%s\",\"%s\",%d",
                        reg.getUrlImagen().replace("\"", "\"\""),
                        reg.getArchivoLocal().replace("\"", "\"\""),
                        reg.getEstado().replace("\"", "\"\""),
                        reg.getTamanoBytes());
                writer.write(linea);
                writer.newLine();
            }
            System.out.println("Reporte CSV generado exitosamente en: " + rutaArchivoCsv.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error al generar el archivo descargas.csv: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("ImageDownloader");
        System.out.println("---------------------------------------");

        if (args.length < 1 || args.length > 2) {
            System.err.println("Uso: java ImageDownloader <URL> [directorio]");
            System.err.println("Ejemplo: java ImageDownloader https://ejemplo.org/galeria.html ./imagenes");
            return;
        }

        Path directorioDestino = Path.of(args.length == 2 ? args[1] : DEFAULT_OUTPUT_DIR);

        try {
            // Fase 2: Validar URL
            URL urlBase = validarURL(args[0]);
            System.out.println("Conectando con: " + urlBase + "\n");

            // Fase 3: Conexión y recuperación de HTML
            String html = obtenerHTML(urlBase);

            // Fase 4: Extracción de imágenes con expresiones regulares
            System.out.println("\nAnalizando documento HTML...");
            Set<String> referencias = extraerImagenes(html);
            System.out.println("\nImágenes encontradas: " + referencias.size());

            if (referencias.isEmpty()) {
                System.out.println("No se encontraron referencias a imágenes en la página.");
                System.out.println("---------------------------------------");
                System.out.println("Descargas exitosas: 0");
                System.out.println("Descargas fallidas: 0");
                System.out.println("Directorio: " + directorioDestino.toAbsolutePath());
                return;
            }

            // Crear directorio local si no existe
            Files.createDirectories(directorioDestino);

            // Fase 5, 6 y Complementaria: Resolución, Descarga y Reporte CSV
            List<RegistroDescarga> registros = new ArrayList<>();
            int exitosas = 0;
            int fallidas = 0;
            int contador = 1;

            System.out.println();
            for (String ref : referencias) {
                try {
                    // Fase 5: Resolución de URL relativa a absoluta
                    URL urlImagen = resolverImagen(urlBase, ref);

                    // Fase 6: Descarga binaria y almacenamiento seguro
                    boolean ok = descargarImagen(urlImagen, directorioDestino, contador, registros);
                    if (ok) {
                        exitosas++;
                    } else {
                        fallidas++;
                    }
                } catch (IllegalArgumentException | MalformedURLException e) {
                    System.out.printf("[%d] %-28s ... ERROR (URL inválida/esquema no admitido: %s)%n", contador, ref, e.getMessage());
                    registros.add(new RegistroDescarga(ref, "—", "Error URL: " + e.getMessage(), 0));
                    fallidas++;
                } catch (Exception e) {
                    System.out.printf("[%d] %-28s ... ERROR (%s)%n", contador, ref, e.getMessage());
                    registros.add(new RegistroDescarga(ref, "—", "Error: " + e.getMessage(), 0));
                    fallidas++;
                }
                contador++;
            }

            // Resumen final
            System.out.println("\n---------------------------------------");
            System.out.println("Descargas exitosas: " + exitosas);
            System.out.println("Descargas fallidas: " + fallidas);
            System.out.println("Directorio: " + directorioDestino.toAbsolutePath());

            // Actividad complementaria: Generar descargas.csv
            Path rutaCsv = directorioDestino.resolve(CSV_FILENAME);
            generarReporteCSV(rutaCsv, registros);

        } catch (IllegalArgumentException e) {
            System.err.println("Error de validación de argumentos: " + e.getMessage());
        } catch (MalformedURLException e) {
            System.err.println("URL no admitida: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error de conexión con la página web: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error general inesperado: " + e.getMessage());
        }
    }
}
