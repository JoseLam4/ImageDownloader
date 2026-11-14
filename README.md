# ImageDownloader - Práctica: Descarga de imágenes mediante URL y URLConnection en Java

## Información General
- **Asignatura:** Desarrollo de Sistemas III
- **Unidad temática:** Arquitectura cliente-servidor y programación de aplicaciones para Internet
- **Nombre de la práctica:** Descargador de imágenes de páginas web (ImageDownloader)
- **Estudiante:** José Carlos Tirado Lam
- **Matrícula:** 225213509
- **Licenciatura:** Ingeniería en Sistemas de Información
- **División:** Ciencias e Ingeniería
- **Institución:** Universidad de Sonora
- **Entorno:** OpenJDK 25 (Compatible con JDK 17+), Git / GitHub, IntelliJ IDEA
- **Restricción tecnológica:** Exclusivamente clases de la API estándar de Java (`java.net.URL`, `java.net.URLConnection`, `java.net.HttpURLConnection`, `java.io.*`, `java.nio.file.*`, `java.util.regex.*`). Prohibido el uso de bibliotecas externas o `HttpClient`.

---

## Propósito del Proyecto
Desarrollar una aplicación de consola en Java que implemente el comportamiento fundamental del cliente en la arquitectura web cliente-servidor:
1. Conectarse a un servidor web mediante HTTP/HTTPS utilizando `URL` y `HttpURLConnection`.
2. Descargar e interpretar el documento HTML principal.
3. Analizar e identificar referencias a recursos gráficos (`<img>`).
4. Resolver direcciones relativas con base en la URL del documento.
5. Descargar concurrentemente o secuencialmente los flujos binarios de cada imagen aplicando validaciones de tipo MIME, límites de tamaño (10 MB máx.), prevención de sobreescritura y control de errores.
