import java.util.Scanner;
import java.util.HashSet;
import java.util.Set;

// Libreria Jsoup para extraer links
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

// Libreria java.nio para crear archivos y directorios
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// Libreria java.io para operaciones de lectura y escritura
import java.io.*;

// Libreria java.net para las peticiones
import java.net.*;
import javax.net.ssl.HttpsURLConnection;

public class Prueba {
    private static int depthLevel = 2; // Cambia el nivel de profundidad aquí (0 o 1 para solo el index.html)
    private static Set<String> visited = new HashSet<>();

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        // Pedimos la url:
        System.out.print("Ingresa una URL: ");
        String dir = sc.nextLine();

        URI uri = new URI(dir);
        String host = uri.getHost();

        createDirectory(host);

        downloadFile(uri.toString(), host, 0);

        sc.close();
    }

    private static void downloadFile(String urlStr, String baseDir, int currentDepth) {
        // Comprueba si ya se visito el sitio y si se tiene que seguir descargando
        if (visited.contains(urlStr) || currentDepth > depthLevel) return;
        visited.add(urlStr);

        try {
            URI uri = new URI(urlStr);
            URL url = uri.toURL();
            String scheme = uri.getScheme();
            String slashPath = uri.getPath();

            // Creamos el objeto de la petición
            HttpURLConnection connection;
            if (scheme.equalsIgnoreCase("https")) {
                connection = (HttpsURLConnection) url.openConnection();
            } else if (scheme.equalsIgnoreCase("http")) {
                connection = (HttpURLConnection) url.openConnection();
            } else {
                System.err.println("Unsupported protocol: " + scheme);
                return;
            }

            // Hacemos la petición como si fuera desde Mozilla
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            int responseCode = connection.getResponseCode();

            // Si el GET es OK 200 guardamos
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String filePath;
                if (slashPath == null || slashPath.equals("/") || slashPath.isEmpty()) {
                    filePath = "./" + baseDir + "/index.html";
                } else {
                    filePath = "./" + baseDir + slashPath;
                    if (filePath.endsWith("/")) filePath += "index.html";
                }

                Path path = Paths.get(filePath);
                Files.createDirectories(path.getParent());

                // Descarga el archivo y guarda el contenido en memoria si es CSS o JS
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (InputStream in = connection.getInputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        baos.write(buffer, 0, bytesRead);
                    }
                    Files.write(path, baos.toByteArray());
                }

                // Avisamos que guardamos
                System.out.println("Archivo: " + filePath + " guardado correctamente.");

                String contentType = connection.getContentType();
                String lowerPath = filePath.toLowerCase();

                // Si el depthLevel es mayor a 1 busca más dependencias que descargar
                if (contentType != null && contentType.contains("text/html") && currentDepth < depthLevel) {
                    getDependences(filePath, urlStr, baseDir, currentDepth + 1);
                } else if ((contentType != null && contentType.contains("text/css")) || lowerPath.endsWith(".css")) {
                    getCssDependences(baos.toString("UTF-8"), urlStr, baseDir, currentDepth + 1);
                } else if ((contentType != null && contentType.contains("javascript")) || lowerPath.endsWith(".js")) {
                    getJsDependences(baos.toString("UTF-8"), urlStr, baseDir, currentDepth + 1);
                }
            } else {
                // En caso de que la petición no sea un OK 200
                System.out.println("Error en la peticion GET: " + urlStr);
                System.out.println("Response Code: " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("Error descargando " + urlStr + ": " + e.getMessage());
        }
    }

    // Crea el directorio con el nombre de la página
    private static void createDirectory(String directoryName) {
        String dir = "./" + directoryName;
        Path path = Paths.get(dir);

        try {
            if (!Files.exists(path)) {
                    Files.createDirectories(path);
            }          
        } catch (Exception e) {
            System.out.println("No se pudo crear el directorio");
            e.printStackTrace();
        }
    }

    // Buscar y descargar referencias en el HTML
    private static void getDependences(String filePath, String baseUri, String baseDir, int currentDepth) throws IOException {
        File file = new File(filePath);
        Document doc = Jsoup.parse(file, "UTF-8", baseUri);

        // Selecciona todos los elementos con atributos que pueden contener recursos
        Elements resources = doc.select("[src], [href], [data-src], [poster]");

        for (Element elem : resources) {
            String absUrl = "";
            if (elem.hasAttr("src")) absUrl = elem.absUrl("src");
            else if (elem.hasAttr("href")) absUrl = elem.absUrl("href");
            else if (elem.hasAttr("data-src")) absUrl = elem.absUrl("data-src");
            else if (elem.hasAttr("poster")) absUrl = elem.absUrl("poster");

            // Filtra enlaces vacíos, mailto, javascript, anchors, etc.
            if (!absUrl.isEmpty() && !absUrl.startsWith("mailto:") && !absUrl.startsWith("javascript:") && !absUrl.startsWith("#")) {
                downloadFile(absUrl, baseDir, currentDepth);
            }
        }
    }   

    // Busca url(...) en CSS y descarga los recursos
    private static void getCssDependences(String cssContent, String baseUri, String baseDir, int currentDepth) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("url\\(['\"]?([^'\"\\)]+)['\"]?\\)");
        java.util.regex.Matcher matcher = pattern.matcher(cssContent);
        while (matcher.find()) {
            String resourceUrl = matcher.group(1);
            try {
                String absUrl = new URI(baseUri).resolve(resourceUrl).toURL().toString();
                if (!visited.contains(absUrl)) {
                    downloadFile(absUrl, baseDir, currentDepth);
                }
            } catch (Exception e) {
                // Ignora errores de URL mal formadas
            }
        }
    }

    // Busca rutas comunes en JS (muy básico, solo para casos sencillos)
    private static void getJsDependences(String jsContent, String baseUri, String baseDir, int currentDepth) {
        // Busca patrones como src="...", href="...", url("..."), import("..."), fetch("..."), etc.
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "(?:src|href|url|import|fetch)\\s*\\(?'?\"?([^'\"\\)]+)['\"\\)]?"
        );
        java.util.regex.Matcher matcher = pattern.matcher(jsContent);
        while (matcher.find()) {
            String resourceUrl = matcher.group(1);
            // Ignora rutas absolutas externas que no sean http/https
            if (!resourceUrl.startsWith("data:") && !resourceUrl.startsWith("mailto:") && !resourceUrl.startsWith("javascript:")) {
                try {
                    String absUrl = new URI(baseUri).resolve(resourceUrl).toURL().toString();
                    if (!visited.contains(absUrl)) {
                        downloadFile(absUrl, baseDir, currentDepth);
                    }
                } catch (Exception e) {
                    // Ignora errores de URL mal formadas
                }
            }
        }
    }
}
