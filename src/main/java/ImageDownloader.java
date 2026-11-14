import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * ImageDownloader
 * Aplicación de consola en Java para la descarga de imágenes de páginas web
 * utilizando la API estándar java.net (URL, URLConnection, HttpURLConnection).
 *
 * @author José Carlos Tirado Lam (Matrícula: 225213509)
 * @version 1.0
 */
public class ImageDownloader {

    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 15000;
    private static final String USER_AGENT = "ImageDownloader/1.0 (Java Standard Library; UNISON)";

    /**
     * Valida el argumento recibido y construye un objeto URL verificando
     * protocolo permitido (HTTP/HTTPS) y existencia de un host válido.
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
            connection.setRequestProperty("User-Agent", USER_AGENT);

            System.out.println("Conectando con: " + url);
            int codigo = connection.getResponseCode();
            System.out.println("\nCódigo HTTP: " + codigo);

            String contentType = connection.getContentType();
            System.out.println("Tipo de contenido: " + (contentType != null ? contentType : "desconocido"));

            if (codigo != HttpURLConnection.HTTP_OK) {
                throw new IOException("El servidor respondió con código HTTP de error: " + codigo + " " + connection.getResponseMessage());
            }

            if (contentType != null && !contentType.toLowerCase().contains("text/html") && !contentType.toLowerCase().contains("application/xhtml")) {
                System.err.println("Advertencia: El tipo de contenido (" + contentType + ") no parece ser un documento HTML estándar.");
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
                        // Si el charset no es reconocido, se recurre al valor por defecto
                    }
                }
            }
        }
        return StandardCharsets.UTF_8;
    }

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Uso: java ImageDownloader <URL> [directorio]");
            System.err.println("Ejemplo: java ImageDownloader https://ejemplo.org/galeria.html ./imagenes");
            return;
        }

        try {
            URL url = validarURL(args[0]);
            String html = obtenerHTML(url);

            System.out.println("\n--- Muestra del HTML obtenido (primeros 500 caracteres) ---");
            int finMuestra = Math.min(500, html.length());
            System.out.println(html.substring(0, finMuestra));
            System.out.println("\n[Longitud total del documento: " + html.length() + " caracteres]");

        } catch (IllegalArgumentException e) {
            System.err.println("Error de validación de URL: " + e.getMessage());
        } catch (MalformedURLException e) {
            System.err.println("URL no admitida: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error de conexión o E/S: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error inesperado: " + e.getMessage());
        }
    }
}
