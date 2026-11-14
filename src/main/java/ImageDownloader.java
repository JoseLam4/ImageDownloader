import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * ImageDownloader
 * Aplicación de consola en Java para la descarga de imágenes de páginas web
 * utilizando la API estándar java.net (URL, URLConnection, HttpURLConnection).
 *
 * @author José Carlos Tirado Lam (Matrícula: 225213509)
 * @version 1.0
 */
public class ImageDownloader {

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

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Uso: java ImageDownloader <URL> [directorio]");
            System.err.println("Ejemplo: java ImageDownloader https://ejemplo.org/galeria.html ./imagenes");
            return;
        }

        try {
            URL url = validarURL(args[0]);
            System.out.println("URL validada con éxito:");
            System.out.println("  Protocolo: " + url.getProtocol());
            System.out.println("  Servidor:  " + url.getHost());
            System.out.println("  Puerto:    " + (url.getPort() == -1 ? url.getDefaultPort() : url.getPort()));
            System.out.println("  Ruta:      " + (url.getPath().isEmpty() ? "/" : url.getPath()));
        } catch (IllegalArgumentException e) {
            System.err.println("Error de validación de URL: " + e.getMessage());
        } catch (MalformedURLException e) {
            System.err.println("URL no admitida: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error inesperado: " + e.getMessage());
        }
    }
}
