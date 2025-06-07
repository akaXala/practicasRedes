// Clase para las respuestas
import Response.Response;

// Librerias de Java
import java.io.*;
import java.net.*;
import java.nio.file.*;

public class Servidor extends Thread {
    // Socket y variables
    protected Socket socket;
    protected PrintWriter pw;
    protected BufferedOutputStream bos;
    protected BufferedReader br;

    public Servidor(Socket socket) throws Exception {
        this.socket = socket;
    }

    public void run(){
        try {
            if (Main.DEBUG_SLEEP) {
                System.out.println("Procesando petición en el hilo: " + Thread.currentThread().getName());
                Thread.sleep(2000); // Simula una tarea de 2 segundos.
            }
            // Abrimos los flujos de entrada y salida
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bos = new BufferedOutputStream(socket.getOutputStream());
            pw = new PrintWriter(new OutputStreamWriter(bos));

            // Leemos la petición HTTP
            String line = br.readLine();
            System.out.println("DATOS: " + line);

            // Si no hat petición (Solo postman)
            if (line == null) {
                String html = withoutRequest();
                pw.print("HTTP/1.1 200 OK\r\n");
                pw.print("Content-Type: text/html\r\n");
                pw.print("Content-Length: " + html.length() + "\r\n");
                pw.print("\r\n");
                pw.print(html);
                pw.flush();
                socket.close();
                return;
            }

            // Leemos si hay argumento
            if (line.startsWith("GET")) {   // petición GET
                // Extraemos la URL
                String[] parts = line.split(" ");
                String url = parts[1];

                // Respuesta
                String response = "";

                // Verificamos si trae parametros
                if (url.contains("?")) {
                    // Parametros
                    String filePath = "";
                    String fileName = "";

                    // Variables de control
                    boolean receivedFilePath = false;
                    boolean receivedFileName = false;

                    // Extraemos los parametros
                    String query = url.substring(url.indexOf("?") + 1);
                    String[] params = query.split("&");

                    for (String param : params) {
                        String[] pair = param.split("=");

                        if (pair.length == 2) {
                            String key = URLDecoder.decode(pair[0], "UTF-8");
                            String value = URLDecoder.decode(pair[1], "UTF-8");

                            switch (key) {
                                case "file_path":
                                    filePath = value;
                                    receivedFilePath = true;
                                    break;
                                
                                case "file_name":
                                    fileName = value;
                                    receivedFileName = true;
                                    break;

                                default:
                                    break;
                            }
                        }
                    }

                    if (!receivedFilePath || !receivedFileName || filePath.isEmpty() || fileName.isEmpty()) {
                        // Handle the case where any parameter is missing or empty
                        response = "<html><body><div><h2>No se han recibido los parametros correctamente</h2><p>La peticion debe ser enviada como: '?file_path=value&file_name=value'</p></div></body></html>";
                        pw.print("HTTP/1.1 400 Bad Request\r\n");
                        pw.print("Content-Type: text/html\r\n");
                        pw.print("Content-Length: " + response.length() + "\r\n");
                        pw.print("\r\n");
                        pw.print(response);
                        pw.flush();
                        socket.close();
                        return;
                    }

                    // Obtenemos los datos
                    Response res = methodGET(filePath, fileName);

                    // Mandamos la respuesta
                    if (res.getStatusCode() == 200) {   // Se leyo de forma correcta
                        response = "<html><body><div><h2>Archivo: " + fileName + "</h2><p>" + res.getMessage() +"</p></div></body></html>";
                        pw.print("HTTP/1.1 200 OK\r\n");
                    } else if (res.getStatusCode() == 404) {    // El archivo no existe
                        response = "<html><body><h2>" + res.getMessage() + "</h2></body></html>";
                        pw.print("HTTP/1.1 404 Not Found\r\n");
                    } else if (res.getStatusCode() == 500) {    // Error interno del servidor
                        response = "<html><body><h2>" + res.getMessage() + "</h2></body></html>";
                        pw.print("HTTP/1.1 500 Internal Server Error\r\n");
                    }
                    pw.print("Content-Type: text/html\r\n");
                    pw.print("Content-Length: " + response.length() + "\r\n");
                    pw.print("\r\n");
                    pw.print(response);
                    pw.flush();
                    socket.close();
                    return;

                } else {
                    // Página que tenemos que mostrar
                    String html = index();

                    // Respuesta a la petición
                    pw.print("HTTP/1.1 200 OK\r\n");
                    pw.print("Content-Type: text/html\r\n");
                    pw.print("Content-Length: " + html.length() + "\r\n");
                    pw.print("\r\n");
                    pw.print(html);
                    pw.flush();

                    socket.close();
                    return;
                }
            } else if (line.startsWith("POST")) {   // petición POST
                // Extraemos la URL
                String[] parts = line.split(" ");
                String url = parts[1];

                // Respuesta
                String response = "";

                // Veemos si hay una query
                if (url.contains("?")) {
                    // Parametros
                    String filePath = "";
                    String fileName = "";
                    String content = "";

                    // Variables de contro
                    boolean receivedFilePath = false;
                    boolean receivedFileName = false;


                    String query = url.substring(url.indexOf("?") + 1);
                    String[] params = query.split("&");

                    // Extraemos todos los parametros
                    for (String param : params) {
                        String[] pair = param.split("=");

                        if (pair.length == 2) {
                            String key = URLDecoder.decode(pair[0], "UTF-8");
                            String value = URLDecoder.decode(pair[1], "UTF-8");
                            
                            switch (key) {
                                case "file_path":
                                    filePath = value;
                                    receivedFilePath = true;
                                    break;
                                case "file_name":
                                    fileName = value;
                                    receivedFileName = true;
                                    break;

                                case "content":
                                    content = value;
                                break;
                            
                                default:
                                    break;
                            }
                        }
                    }

                    if (!receivedFilePath || !receivedFileName || filePath.isEmpty() || fileName.isEmpty()) {
                        // Mandamos la respuesta de error
                        response = "<html><body><div><h2>No se han recibido los parametros correctamente</h2><p>La peticion debe ser enviada como: '?file_path=value&file_name=value&content=value(opcional)'</p></div></body></html>";
                        pw.print("HTTP/1.1 400 Bad Request\r\n");
                        pw.print("Content-Type: text/html\r\n");
                        pw.print("Content-Length: " + response.length() + "\r\n");
                        pw.print("\r\n");
                        pw.print(response);
                        pw.flush();
                        socket.close();
                        return;
                    }

                    // Insertamos los datos
                    Response res = methodPOST(filePath, fileName, content);

                    // Mandamos la respuesta
                    if (res.getStatusCode() == 201) {   // Se creo
                        response = "<html><body><h2>" + res.getMessage() + "</h2><p>Contenido: " + res.getContent() + "</p></body></html>";
                        pw.print("HTTP/1.1 201 Created\r\n");

                    } else if (res.getStatusCode() == 200) {    // Se actualizo
                        response = "<html><body><h2>" + res.getMessage() + "</h2><p>Contenido: " + res.getContent() + "</p></body></html>";
                        pw.print("HTTP/1.1 200 OK\r\n");

                    } else if (res.getStatusCode() == 500) {    // Error al crear o error interno del servidor
                        response = "<html><body><h2>" + res.getMessage() + "</h2></body></html>";
                        pw.print("HTTP/1.1 500 Internal Server Error\r\n");
                    }
                    pw.print("Content-Type: text/html\r\n");
                    pw.print("Content-Length: " + response.length() + "\r\n");
                    pw.print("\r\n");
                    pw.print(response);
                    pw.flush();
                    socket.close();
                    return;

                } else {
                    // Mandamos la respuesta de error
                    response = "<html><body><h2>No se han recibido los parametros correctamente</h2><p>La peticion debe ser enviada como: '?file_path=value&file_name=value&content=value(opcional)'</p></body></html>";
                    pw.print("HTTP/1.1 400 Bad Request\r\n");
                    pw.print("Content-Type: text/html\r\n");
                    pw.print("Content-Length: " + response.length() + "\r\n");
                    pw.print("\r\n");
                    pw.print(response);
                    pw.flush();
                    System.out.println(response.length());
                    return;
                }
                
            } else if (line.startsWith("PUT")) {    // petición PUT
                // Extraemos la URL
                String[] parts = line.split(" ");
                String url = parts[1];

                // Respuesta
                String response = "";

                // Verificamos si trae parametros
                if (url.contains("?")) {
                    // Parametros
                    String filePath = "";
                    String fileName = "";
                    String content = "";

                    // Variables de control
                    boolean receivedFilePath = false;
                    boolean receivedFileName = false;

                    // Extraemos los parametros
                    String query = url.substring(url.indexOf("?") + 1);
                    String[] params = query.split("&");

                    for (String param : params) {
                        String[] pair = param.split("=");

                        if (pair.length == 2 ) {
                            String key = URLDecoder.decode(pair[0], "UTF-8");
                            String value = URLDecoder.decode(pair[1], "UTF-8");
                            switch (key) {
                                case "file_path":
                                    filePath = value;
                                    receivedFilePath = true;
                                    break;
                                case "file_name":
                                    fileName = value;
                                    receivedFileName = true;
                                    break;

                                case "content":
                                    content = value;
                                break;
                            
                                default:
                                    break;
                            }
                        }
                    }

                    if (!receivedFilePath || !receivedFileName || filePath.isEmpty() || fileName.isEmpty()) {
                        // Mandamos la respuesta de error
                        response = "<html><body><div><h2>No se han recibido los parametros correctamente</h2><p>La peticion debe ser enviada como: '?file_path=value&file_name=value&content=value(opcional)'</p></div></body></html>";
                        pw.print("HTTP/1.1 400 Bad Request\r\n");
                        pw.print("Content-Type: text/html\r\n");
                        pw.print("Content-Length: " + response.length() + "\r\n");
                        pw.print("\r\n");
                        pw.print(response);
                        pw.flush();
                        socket.close();
                        return;
                    }

                    // Invocamos al metodo y esperamos respuesta
                    Response res = methodPUT(filePath, fileName, content);

                    response = "<html><body><h2>" + res.getMessage() + "</h2></body></html>";

                    // Mandamos la respuesta
                    if (res.getStatusCode() == 201) {
                        pw.print("HTTP/1.1 201 Created\r\n");
                    } else if (res.getStatusCode() == 200) {
                        pw.print("HTTP/1.1 200 OK\r\n");
                    } else if (res.getStatusCode() == 500) {
                        pw.print("HTTP/1.1 500 Internal Server Error\r\n");
                    }
                    pw.print("Content-Type: text/html\r\n");
                    pw.print("Content-Length: " + response.length() + "\r\n");
                    pw.print("\r\n");
                    pw.print(response);
                    pw.flush();
                    socket.close();
                    return;

                } else {
                    // Mandamos la respuesta de error
                    response = "<html><body><div><h2>No se han recibido los parametros correctamente</h2><p>La peticion debe ser enviada como: '?file_path=value&file_name=value&content=value(opcional)'</p></div></body></html>";
                    pw.print("HTTP/1.1 400 Bad Request\r\n");
                    pw.print("Content-Type: text/html\r\n");
                    pw.print("Content-Length: " + response.length() + "\r\n");
                    pw.print("\r\n");
                    pw.print(response);
                    pw.flush();
                    socket.close();
                    return;
                }

            } else if (line.startsWith("DELETE")) { // petición DELETE
                // Extraemos la URL
                String[] parts = line.split(" ");
                String url = parts[1];

                // Respuesta
                String response = "";

                // Veemos si hay parametros
                if (url.contains("?")) {
                    // Parametros
                    String filePath = "";
                    String fileName = "";

                    // Variables de control
                    boolean receivedFilePath = false;
                    boolean receivedFileName = false;

                    // Extraemos los parametros
                    String query = url.substring(url.indexOf("?") + 1);
                    String[] params = query.split("&");

                    for (String param : params) {
                        String[] pair = param.split("=");

                        if (pair.length == 2) {
                            String key = URLDecoder.decode(pair[0], "UTF-8");
                            String value = URLDecoder.decode(pair[1], "UTF-8");
                            switch (key) {
                                case "file_path":
                                    filePath = value;
                                    receivedFilePath = true;
                                    break;
                                
                                case "file_name":
                                    fileName = value;
                                    receivedFileName = true;
                                    break;

                                default:
                                    break;
                            }
                        }
                    }

                    if (!receivedFilePath || !receivedFileName || filePath.isEmpty() || fileName.isEmpty()) {
                        // Handle the case where any parameter is missing or empty
                        response = "<html><body><div><h2>No se han recibido los parametros correctamente</h2><p>La peticion debe ser enviada como: '?file_path=value&file_name=value'</p></div></body></html>";
                        pw.print("HTTP/1.1 400 Bad Request\r\n");
                        pw.print("Content-Type: text/html\r\n");
                        pw.print("Content-Length: " + response.length() + "\r\n");
                        pw.print("\r\n");
                        pw.print(response);
                        pw.flush();
                        socket.close();
                        return;
                    }

                    // Insertamos los datos
                    Response res = methodDELETE(filePath, fileName);

                    response = "<html><body><h2>" + res.getMessage() + "</h2></body></html>";


                    // Mandamos la respuesta
                    if (res.getStatusCode() == 204) {   // Se elimino
                        pw.print("HTTP/1.1 204 No Content\r\n");
                    } else if (res.getStatusCode() == 404) {    // No existe
                        pw.print("HTTP/1.1 404 Not Found\r\n");
                    } else if (res.getStatusCode() == 500) {    // Error interno
                        pw.print("HTTP/1.1 500 Internal Server Error\r\n");
                    }
                    pw.print("Content-Type: text/html\r\n");
                    pw.print("Content-Length: " + response.length() + "\r\n");
                    pw.print("\r\n");
                    pw.print(response);
                    pw.flush();
                    socket.close();
                    return;
                } else {
                    // Mandamos la respuesta de error
                    response = "<html><body><h2>No se han recibido los parametros correctamente</h2><p>La peticion debe ser enviada como: '?file_path=value&file_name=value'</p></body></html>";
                    pw.print("HTTP/1.1 400 Bad Request\r\n");
                    pw.print("Content-Type: text/html\r\n");
                    pw.print("Content-Length: " + response.length() + "\r\n");
                    pw.print("\r\n");
                    pw.print(response);
                    pw.flush();
                    socket.close();
                    return;
                }

            } else {    // Petición no implementada
                pw.println("HTTP/1.0 501 Not Implemented");
                pw.print("\r\n");
                pw.flush();
                socket.close();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String withoutRequest(){
        // Página que se envia si no hay petición
        String html = "<html><head><title>Servidor web</title></head><body bgcolor=\"cyan\"><br/></body></html>";
        return html;
    }
    
    public static String index(){
        // Página que se tiene que mostrar
        String html = "<html><head><title>Pagina principal</title></head><body bgcolor=\"cyan\"><p>Peticion GET sin parametros</p></body></html>";
        return html;
    }

    public static Response methodGET(String filePath, String fileName) {
        // Creamos la ruta
        String dir = "./" + filePath + "/" + fileName + ".txt";
        Path path = Paths.get(dir);

        try {
            // Verificamos si el archivo existe
            if (Files.exists(path)) {
                String content = Files.readString(path);    // Leemos el archivo

                // Escribimos la respuesta
                Response res = new Response(200, content, null);
                return res;
            } else {    // Si no existe
                // Escribirmos la respuesta
                Response res = new Response(404, "El archivo " + fileName +" no existe", null);            
                return res; 
            }
        } catch (Exception e) {
            e.printStackTrace();

            // Escribirmos la respuesta
            Response res = new Response(500, "Error interno del servidor al intentar leer el archivo", null);
            return res;
        }

    }
    
    public static Response methodPOST(String filePath, String fileName, String content){
        // Creamos la ruta
        String dir = "./" + filePath + "/" + fileName + ".txt";
        Path path = Paths.get(dir);

        // Salto de linea
        String n = "\n";

        try {
            // Creamos los directorios si no existen
            Path parentDir = path.getParent();
            if (parentDir != null && Files.notExists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // Verificamos si existe el archivo
            if (Files.notExists(path)) {    // Si no existe
                Files.createFile(path);     // Creamos el archivo
                Files.write(path, content.getBytes());  // Escribimos el contenido
                String cont = Files.readString(path);    // Leemos el archivo

                // Retornamos una respuesta
                Response res = new Response(201, "El archivo " + fileName + " fue creado correctamente y escrito correctamente", cont);
                return res;
            } else {    // Si existe
                Files.write(path, n.getBytes(), StandardOpenOption.APPEND);    // Escribimos un salto de linea
                Files.write(path, content.getBytes(), StandardOpenOption.APPEND);  // Escribimos el contenido
                String cont = Files.readString(path);    // Leemos el archivo

                // Retornamos una respuesta
                Response res = new Response(200, "El archivo " + fileName + " fue actualizado correctamente", cont);
                return res;
            }
            

        } catch (Exception e) {
            e.printStackTrace();

            // Escribirmos la respuesta
            Response res = new Response(500, "Error interno del servidor al intentar crear el archivo", null);
            return res;
        }
    }

    public static Response methodPUT(String filePath, String fileName, String content) {
        // Creamos la ruta
        String dir = "./" + filePath + "/" + fileName + ".txt";
        Path path = Paths.get(dir);

        try {
            // Creamos los directorios si no existen
            Path parentDir = path.getParent();
            if (parentDir != null && Files.notExists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // Verificamos si existe el archivo
            if (Files.notExists(path)) {    // Si no existe
                Files.createFile(path);     // Creamos el archivo
                Files.write(path, content.getBytes());  // Escribimos el contenido

                // Retornamos una respuesta
                Response res = new Response(201, "El archivo " + fileName + " fue creado correctamente y escrito correctamente", null);
                return res;
            } else {    // Si existe
                Files.delete(path); // Lo borramos
                Files.createFile(path); // Creamos el archivo (sobreescribir)
                Files.write(path, content.getBytes());  // Escribimos el contenido

                // Retornamos una respuesta
                Response res = new Response(200, "El archivo " + fileName + " fue sobreescrito correctamente", null);
                return res;
            }

        } catch (Exception e) {
            e.printStackTrace();

            // Escribirmos la respuesta
            Response res = new Response(500, "Error interno del servidor al intentar crear el archivo", null);
            return res;
        }
    }

    public static Response methodDELETE(String filePath, String fileName) {
        // Creamos la ruta
        String dir = "./" + filePath + "/" + fileName + ".txt";
        Path path = Paths.get(dir); // Obtenemos la ruta

        try {
            // Verificamos si el archivo existe
            if (Files.exists(path)) {   // Si existe
                Files.delete(path); // Borramos el archivo

                // Escribirmos la respuesta
                Response res = new Response(204, "Archivo " + fileName + " eliminado correctamente", null);
                return res;
            } else {    // Si no existe
                // Escribirmos la respuesta
                Response res = new Response(404, "El archivo " + fileName +" no existe", null);            
                return res;    
            }
        } catch (Exception e) {
            e.printStackTrace();

            // Escribirmos la respuesta
            Response res = new Response(500, "Error interno del servidor al intentar borrar el archivo", null);
            return res;
        }
    }
}
