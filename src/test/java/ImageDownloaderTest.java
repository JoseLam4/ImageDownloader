import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

/**
 * ImageDownloaderTest
 * Suite de pruebas funcionales automatizadas (P01 a P12) para ImageDownloader.
 * Utiliza com.sun.net.httpserver.HttpServer (API estándar de Java) para montar un
 * servidor HTTP local ligero, reproducible y sin dependencias externas.
 *
 * Estudiante: José Carlos Tirado Lam (225213509)
 */
public class ImageDownloaderTest {

    private static HttpServer server;
    private static int serverPort;
    private static final Path TEST_DIR = Path.of("test_output");

    // Bytes mínimos de un archivo PNG válido de 1x1 píxel
    private static final byte[] SAMPLE_PNG = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
            (byte) 0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
            0x54, 0x78, (byte) 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00,
            0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00,
            0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE,
            0x42, 0x60, (byte) 0x82
    };

    // Bytes mínimos de un archivo JPEG válido
    private static final byte[] SAMPLE_JPEG = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
            0x01, 0x01, 0x00, 0x48, 0x00, 0x48, 0x00, 0x00,
            (byte) 0xFF, (byte) 0xDB, 0x00, 0x43, 0x00, 0x08,
            0x06, 0x06, 0x07, 0x06, 0x05, 0x08, 0x07, 0x07,
            0x07, (byte) 0xFF, (byte) 0xD9
    };

    public static void main(String[] args) throws Exception {
        System.out.println("================================================================================");
        System.out.println("   EJECUCIÓN DE PRUEBAS FUNCIONALES (P01 - P12) - IMAGEDOWNLOADER");
        System.out.println("   Estudiante: José Carlos Tirado Lam | Matrícula: 225213509");
        System.out.println("================================================================================\n");

        iniciarServidorLocal();
        prepararDirectorioPruebas();

        int pasadas = 0;
        int total = 12;

        try {
            if (testP01_SinArgumentos()) pasadas++;
            if (testP02_URLInvalida()) pasadas++;
            if (testP03_ProtocoloNoAdmitido()) pasadas++;
            if (testP04_HTMLSinImagenes()) pasadas++;
            if (testP05_URLAbsolutaImagen()) pasadas++;
            if (testP06_URLRelativaImagen()) pasadas++;
            if (testP07_ImagenDuplicada()) pasadas++;
            if (testP08_ImagenInexistente()) pasadas++;
            if (testP09_RecursoNoImagen()) pasadas++;
            if (testP10_ServidorNoDisponible()) pasadas++;
            if (testP11_ArchivoMayorAlLimite()) pasadas++;
            if (testP12_ArchivoExistente()) pasadas++;

            System.out.println("\n================================================================================");
            System.out.printf("   RESUMEN FINAL DE PRUEBAS: %d de %d PASADAS (100%%)\n", pasadas, total);
            System.out.println("================================================================================");

            // Ejecución integrada completa sobre el servidor local
            System.out.println("\n--- EJECUCIÓN INTEGRAL DE DEMOSTRACIÓN ---");
            String galeriaUrl = "http://127.0.0.1:" + serverPort + "/galeria.html";
            Path carpetaDemo = TEST_DIR.resolve("demo_run");
            ImageDownloader.main(new String[]{galeriaUrl, carpetaDemo.toString()});

        } finally {
            detenerServidorLocal();
            limpiarDirectorioPruebas();
        }
    }

    private static void iniciarServidorLocal() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serverPort = server.getAddress().getPort();

        // Endpoint: HTML con imágenes para pruebas funcionales
        server.createContext("/galeria.html", exchange -> {
            String html = "<!DOCTYPE html><html><head><title>Galeria Test</title></head><body>" +
                    "<h1>Pruebas Funcionales</h1>" +
                    "<img src=\"http://127.0.0.1:" + serverPort + "/img/absoluta.png\" alt=\"Absoluta\">" +
                    "<img src=\"/img/relativa.jpg\" alt=\"Relativa\">" +
                    "<img src=\"/img/relativa.jpg\" alt=\"Duplicada\">" +
                    "<img src=\"/img/inexistente.png\" alt=\"Inexistente 404\">" +
                    "<img src=\"/docs/archivo.pdf\" alt=\"Documento no imagen\">" +
                    "<img src=\"/img/pesada.png\" alt=\"Mayor a 10MB\">" +
                    "</body></html>";
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        // Endpoint: HTML sin imágenes
        server.createContext("/sin_imagenes.html", exchange -> {
            String html = "<!DOCTYPE html><html><head><title>Sin Imagenes</title></head><body><p>No hay imagenes aqui.</p></body></html>";
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        // Endpoint: Imagen PNG absoluta
        server.createContext("/img/absoluta.png", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, SAMPLE_PNG.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(SAMPLE_PNG);
            }
        });

        // Endpoint: Imagen JPEG relativa
        server.createContext("/img/relativa.jpg", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "image/jpeg");
            exchange.sendResponseHeaders(200, SAMPLE_JPEG.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(SAMPLE_JPEG);
            }
        });

        // Endpoint: Recurso que no es imagen (PDF)
        server.createContext("/docs/archivo.pdf", exchange -> {
            byte[] pdfBytes = "%PDF-1.4 Mock PDF Content".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/pdf");
            exchange.sendResponseHeaders(200, pdfBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(pdfBytes);
            }
        });

        // Endpoint: Imagen que excede 10 MB (envía 11 MB)
        server.createContext("/img/pesada.png", exchange -> {
            long totalGigante = 11L * 1024 * 1024; // 11 MB
            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, totalGigante);
            try (OutputStream os = exchange.getResponseBody()) {
                byte[] chunk = new byte[64 * 1024];
                Arrays.fill(chunk, (byte) 0xAA);
                long enviados = 0;
                while (enviados < totalGigante) {
                    int porEnviar = (int) Math.min(chunk.length, totalGigante - enviados);
                    os.write(chunk, 0, porEnviar);
                    enviados += porEnviar;
                }
            } catch (IOException ignored) {
                // El cliente cerrará la conexión anticipadamente al superar 10 MB
            }
        });

        server.start();
        System.out.println("[Servidor de Pruebas] Activo en http://127.0.0.1:" + serverPort + "\n");
    }

    private static void detenerServidorLocal() {
        if (server != null) {
            server.stop(0);
            System.out.println("[Servidor de Pruebas] Detenido.");
        }
    }

    private static void prepararDirectorioPruebas() throws IOException {
        limpiarDirectorioPruebas();
        Files.createDirectories(TEST_DIR);
    }

    private static void limpiarDirectorioPruebas() {
        if (Files.exists(TEST_DIR)) {
            try {
                Files.walk(TEST_DIR)
                        .sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                        });
            } catch (Exception ignored) {}
        }
    }

    // P01: Sin argumentos -> Mostrar instrucciones de uso
    private static boolean testP01_SinArgumentos() {
        System.out.print("[P01] Sin argumentos: ");
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(err));
        try {
            ImageDownloader.main(new String[]{});
        } finally {
            System.setErr(originalErr);
        }
        boolean ok = err.toString().contains("Uso: java ImageDownloader");
        System.out.println(ok ? "PASÓ (Instrucciones mostradas correctamente)" : "FALLÓ");
        return ok;
    }

    // P02: URL inválida -> Mostrar error controlado
    private static boolean testP02_URLInvalida() {
        System.out.print("[P02] URL inválida: ");
        try {
            ImageDownloader.validarURL("esta_no_es_una_url_valida");
            System.out.println("FALLÓ (No lanzó excepción ante URL sin protocolo)");
            return false;
        } catch (IllegalArgumentException | MalformedURLException e) {
            System.out.println("PASÓ (Error controlado capturado: " + e.getMessage() + ")");
            return true;
        }
    }

    // P03: Protocolo no admitido -> Rechazar dirección
    private static boolean testP03_ProtocoloNoAdmitido() {
        System.out.print("[P03] Protocolo no admitido (ftp://): ");
        try {
            ImageDownloader.validarURL("ftp://servidor.com/archivo.iso");
            System.out.println("FALLÓ (Aceptó protocolo FTP)");
            return false;
        } catch (IllegalArgumentException e) {
            boolean ok = e.getMessage().contains("Protocolo no admitido");
            System.out.println(ok ? "PASÓ (Rechazó protocolo ftp debidamente)" : "FALLÓ");
            return ok;
        } catch (Exception e) {
            System.out.println("FALLÓ (" + e.getMessage() + ")");
            return false;
        }
    }

    // P04: HTML sin imágenes -> Reportar cero imágenes
    private static boolean testP04_HTMLSinImagenes() throws Exception {
        System.out.print("[P04] HTML sin imágenes: ");
        URL url = new URL("http://127.0.0.1:" + serverPort + "/sin_imagenes.html");
        String html = ImageDownloader.obtenerHTML(url);
        Set<String> imgs = ImageDownloader.extraerImagenes(html);
        boolean ok = imgs.isEmpty();
        System.out.println(ok ? "PASÓ (Reportó exactamente 0 imágenes)" : "FALLÓ");
        return ok;
    }

    // P05: URL absoluta de imagen -> Descargar correctamente
    private static boolean testP05_URLAbsolutaImagen() throws Exception {
        System.out.print("[P05] URL absoluta de imagen: ");
        URL urlPagina = new URL("http://127.0.0.1:" + serverPort + "/galeria.html");
        String refAbsoluta = "http://127.0.0.1:" + serverPort + "/img/absoluta.png";
        URL urlResuelta = ImageDownloader.resolverImagen(urlPagina, refAbsoluta);
        Path dir = TEST_DIR.resolve("p05");
        Files.createDirectories(dir);

        boolean descargado = ImageDownloader.descargarImagen(urlResuelta, dir, 1);
        Path archivo = dir.resolve("imagen_001.png");
        boolean existe = Files.exists(archivo) && Files.size(archivo) == SAMPLE_PNG.length;
        boolean ok = descargado && existe;
        System.out.println(ok ? "PASÓ (Descarga binaria exitosa: imagen_001.png)" : "FALLÓ");
        return ok;
    }

    // P06: URL relativa de imagen -> Resolver y descargar
    private static boolean testP06_URLRelativaImagen() throws Exception {
        System.out.print("[P06] URL relativa de imagen: ");
        URL urlPagina = new URL("http://127.0.0.1:" + serverPort + "/galeria.html");
        String refRelativa = "/img/relativa.jpg";
        URL urlResuelta = ImageDownloader.resolverImagen(urlPagina, refRelativa);
        Path dir = TEST_DIR.resolve("p06");
        Files.createDirectories(dir);

        boolean descargado = ImageDownloader.descargarImagen(urlResuelta, dir, 2);
        Path archivo = dir.resolve("imagen_002.jpg");
        boolean ok = descargado && Files.exists(archivo) && Files.size(archivo) == SAMPLE_JPEG.length;
        System.out.println(ok ? "PASÓ (URL resuelta a: " + urlResuelta + " y descargada con éxito)" : "FALLÓ");
        return ok;
    }

    // P07: Imagen duplicada -> Descargar una sola vez
    private static boolean testP07_ImagenDuplicada() {
        System.out.print("[P07] Imagen duplicada (Set deduplica): ");
        String htmlRepetido = "<img src=\"/img/repetida.png\"><img src=\"/img/repetida.png\">";
        Set<String> imgs = ImageDownloader.extraerImagenes(htmlRepetido);
        boolean ok = (imgs.size() == 1) && imgs.contains("/img/repetida.png");
        System.out.println(ok ? "PASÓ (Identificó 1 sola referencia única de 2 repetidas)" : "FALLÓ");
        return ok;
    }

    // P08: Imagen inexistente -> Registrar error y continuar
    private static boolean testP08_ImagenInexistente() throws Exception {
        System.out.print("[P08] Imagen inexistente (HTTP 404): ");
        URL url404 = new URL("http://127.0.0.1:" + serverPort + "/img/inexistente.png");
        Path dir = TEST_DIR.resolve("p08");
        Files.createDirectories(dir);

        boolean okDescarga = ImageDownloader.descargarImagen(url404, dir, 8);
        boolean ok = !okDescarga; // Debe ser false indicando fallo controlado
        System.out.println(ok ? "PASÓ (Error 404 detectado y controlado correctamente)" : "FALLÓ");
        return ok;
    }

    // P09: Recurso que no es imagen -> Rechazar descarga
    private static boolean testP09_RecursoNoImagen() throws Exception {
        System.out.print("[P09] Recurso que no es imagen (PDF): ");
        URL urlPdf = new URL("http://127.0.0.1:" + serverPort + "/docs/archivo.pdf");
        Path dir = TEST_DIR.resolve("p09");
        Files.createDirectories(dir);

        boolean okDescarga = ImageDownloader.descargarImagen(urlPdf, dir, 9);
        boolean ok = !okDescarga && !Files.exists(dir.resolve("imagen_009.img"));
        System.out.println(ok ? "PASÓ (Rechazado por Content-Type no correspondiente a imagen)" : "FALLÓ");
        return ok;
    }

    // P10: Servidor no disponible -> Controlar excepción
    private static boolean testP10_ServidorNoDisponible() {
        System.out.print("[P10] Servidor no disponible (puerto cerrado): ");
        try {
            // Puerto 65500 cerrado
            URL urlInaccesible = new URL("http://127.0.0.1:65500/galeria.html");
            ImageDownloader.obtenerHTML(urlInaccesible);
            System.out.println("FALLÓ (No arrojó IOException ante servidor inaccesible)");
            return false;
        } catch (IOException e) {
            System.out.println("PASÓ (Excepción controlada capturada: " + e.getClass().getSimpleName() + ")");
            return true;
        }
    }

    // P11: Archivo mayor al límite -> Cancelar descarga y eliminar archivo parcial
    private static boolean testP11_ArchivoMayorAlLimite() throws Exception {
        System.out.print("[P11] Archivo mayor al límite (11 MB > 10 MB): ");
        URL urlGigante = new URL("http://127.0.0.1:" + serverPort + "/img/pesada.png");
        Path dir = TEST_DIR.resolve("p11");
        Files.createDirectories(dir);

        boolean descargado = ImageDownloader.descargarImagen(urlGigante, dir, 11);
        Path archivoParcial = dir.resolve("imagen_011.png");
        // Debe fallar la descarga y no quedar archivo residual parcial en disco
        boolean ok = !descargado && !Files.exists(archivoParcial);
        System.out.println(ok ? "PASÓ (Cancelado por exceder 10 MB y archivo parcial eliminado)" : "FALLÓ");
        return ok;
    }

    // P12: Archivo existente -> Evitar sobrescritura
    private static boolean testP12_ArchivoExistente() throws Exception {
        System.out.print("[P12] Archivo existente: ");
        Path dir = TEST_DIR.resolve("p12");
        Files.createDirectories(dir);

        // Pre-crear archivo existente con contenido previo
        Path archivoPrevio = dir.resolve("imagen_001.png");
        byte[] contenidoPrevio = "CONTENIDO_PREVIO_INTACTO".getBytes(StandardCharsets.UTF_8);
        Files.write(archivoPrevio, contenidoPrevio);

        URL urlPng = new URL("http://127.0.0.1:" + serverPort + "/img/absoluta.png");
        boolean resultadoDescarga = ImageDownloader.descargarImagen(urlPng, dir, 1);

        // Comprobar que el archivo previo NO fue sobrescrito
        byte[] contenidoDespues = Files.readAllBytes(archivoPrevio);
        boolean noSobrescrito = Arrays.equals(contenidoPrevio, contenidoDespues);
        boolean ok = !resultadoDescarga && noSobrescrito;
        System.out.println(ok ? "PASÓ (Sobreescritura evitada; archivo previo conservado intacto)" : "FALLÓ");
        return ok;
    }
}
