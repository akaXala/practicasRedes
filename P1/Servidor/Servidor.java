// Carpeta en donde se ubica el servidor
package Servidor;

//Bibliotecas
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Stack;
import java.util.zip.*;

public class Servidor {
    private static String directorioBase = "./Servidor/Archivos";
    private static Stack<String> directorios = new Stack<>();
    private static String dir = "";
    public static void main(String[] args) {
        // Configuraciones de conexion
        int port = 7777;

        try(ServerSocket serverSocket = new ServerSocket(port)) {
            // Conexión del cliente
            System.out.println("Servidor esperando conexiones en el puerto " + port + "...");
            Socket socket = serverSocket.accept();
            System.out.println("Cliente conectado");

            // Flujo de entrada/salida texto
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter pout = new PrintWriter(socket.getOutputStream(), true);            

            // Enviar lista de archivos del servidor
            listarArchivos(pout, directorioBase);
            
            while (true) {
                // Leemos la instrucción dada por el cliente
                String comando = in.readLine();           

                // Opciones
                if(comando.equals("ls")){   // Listar archivos
                    if(directorios.empty()){         // Comprobamos si estamos en la carpeta raíz
                        listarArchivos(pout, directorioBase);   // Listamos archivos/carpetas en la carpeta raíz
                    } else {
                        listarArchivos(pout, dir);              // Listamos archivos/carpetas en la ruta actual
                    }

                } else if(comando.equals("cd")){    // Cambiar diretorio
                    String nombreDirectorio = in.readLine(); // Nombre directorio 
                    moverDirectorio(pout, nombreDirectorio); // Invocamos a la función

                } else if(comando.equals("cp")) {   // Enviar archivo
                    String nombreArchivo = in.readLine();    // Recibimos el archivo
                    enviarArchivo(nombreArchivo);            // Invocamos a la función

                } else if(comando.equals("cpdir")) {   // Enviar archivo
                    String nombreCarpeta = in.readLine();    // Recibimos el archivo
                    enviarCarpeta(nombreCarpeta);            // Invocamos a la función

                } else if(comando.equals("touch")){ // Crear archivo
                    String nombreArchivo = in.readLine();    // Nombre archivo
                    crearArchivo(pout, nombreArchivo);       // Invocamos a la función

                } else if(comando.equals("mkdir")){ // Crear directorio
                    String nombreArchivo = in.readLine();    // Nombre directorio
                    crearCarpeta(pout, nombreArchivo);       // Invocamos a la función

                } else if(comando.equals("mv")){    // Renombrar archivo/carpeta
                    String nombreOriginal = in.readLine();   // Nombre original archivo/carpeta
                    String nombreNuevo = in.readLine();      // Nombre nuevo archivo/carpeta
                    renombrar(pout, nombreOriginal, nombreNuevo);   // Invocamos a la función

                } else if(comando.equals("rm")){    // Eliminar archivo
                    String nombreArchivo = in.readLine();    // Nombre archivo
                    eliminarArchivo(pout, nombreArchivo);    // Invocamos a la función

                } else if(comando.equals("rmdir")){ // Eliminar carpeta
                    String nombreCarpeta = in.readLine();    // Nombre carpeta
                    eliminarCarpeta(pout, nombreCarpeta);    // Invocamos a la función

                } else if(comando.equals("send")) { // Terminar conexion
                    String opcion = in.readLine();
                    String nombre = in.readLine();

                    if(opcion.equals("cp")){
                        descargarArchivo(nombre);
                    } else if(opcion.equals("cpdir")){
                        descargarCarpeta(nombre);
                    }

                } else if(comando.equals("exit")) { // Terminar conexion
                    break;

                } else if(comando.equals("exit")) { // Terminar conexion
                    break;

                } else {
                    System.out.println("error");
                }
            }
            // Cerramos el servidor y el socket
            socket.close();
            serverSocket.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private static void listarArchivos(PrintWriter out, String dir){
        File directorio = new File(dir);
        File[] archivos = directorio.listFiles();

        // Enviamos la lista
        if(archivos != null){
            for(File archivo : archivos) {
                out.println(archivo.getName());
            }
        } else {
            out.println("No se pudo acceder al directorio");
        }
        out.println("END");
    }

    private static void moverDirectorio(PrintWriter out, String argumento){
        if(argumento.equals("..")){
            if(directorios.empty()){
                out.println("empty");
            } else {
                directorios.pop();
                out.println("back");
            }
            dir = setDirectorio();
            System.out.println("Retrocediento. " + dir);
        } else {    // Cambio de directorio
            // Verificar si el directorio existe antes de hacer push
                String nuevoDirectorio = setDirectorio() + "/" + argumento;
                File file = new File(nuevoDirectorio);

                if(file.exists() && file.isDirectory()){
                    directorios.push(argumento);
                    dir = setDirectorio();
                    out.println("ready");
                    System.out.println("Directorio recibido del cliente: " + dir);
                } else {
                    out.println("error");
                    System.out.println("El directorio especificado no existe");
                }
        }
    }

    private static String setDirectorio(){
        StringBuilder directorioActual = new StringBuilder();
        
        directorioActual.append(directorioBase);
        for(int i = 0; i < directorios.size(); i++){
            directorioActual.append("/");
            directorioActual.append(directorios.get(i));
        }

        return directorioActual.toString();
    }

    private static void enviarArchivo(String archivo){
        String nombreArchivo = setDirectorio() + "/" + archivo;
        File file = new File(nombreArchivo);

        // Configuraciones de conexión
        int port = 7778;

        try(ServerSocket serverSocketFile = new ServerSocket(port)){
            Socket socketFile = serverSocketFile.accept();
            PrintWriter pout = new PrintWriter(socketFile.getOutputStream(), true);

            if(!file.exists()){
                pout.println("error");
                return;
            } else {
                pout.println("ready");
            }

            OutputStream os = socketFile.getOutputStream();
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);

            byte[] buffer = new byte[1024];
            int count;

            while ((count = bis.read(buffer)) > 0) {
                os.write(buffer, 0, count);
            }
            os.flush();
            System.out.println("Archivo enviado");
            bis.close();
            os.close();

        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private static void descargarArchivo(String archivo){
        String nombreArchivo = setDirectorio() + "/" + archivo;
        File file = new File(nombreArchivo);

        // Declaramos un nuevo socket para enviar archivos
        String serverAddress = "localhost";
        int port = 7778;

        try (Socket socketFiles = new Socket(serverAddress, port)){
            BufferedReader in = new BufferedReader(new InputStreamReader(socketFiles.getInputStream()));

            String respuesta = in.readLine();

            if(respuesta.equals("error")){
                System.out.println("El archivo no existe");
                return;
            }

            InputStream is = socketFiles.getInputStream();
            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);

            byte[] buffer = new byte[1024];
            int count;

            while ((count = is.read(buffer)) > 0){
                bos.write(buffer, 0, count);
            }

            bos.flush();
            System.out.println("Archivo descargado correctamente");
            bos.close();
            is.close();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void enviarCarpeta(String carpeta){
        String nombreCarpeta = setDirectorio() + "/" + carpeta;
        File dir = new File(nombreCarpeta);

        // Configuraciones de conexión
        int port = 7778;

        try(ServerSocket serverSocketFile = new ServerSocket(port)){
            Socket socketFile = serverSocketFile.accept();
            PrintWriter pout = new PrintWriter(socketFile.getOutputStream(), true);

            if (!dir.exists() || !dir.isDirectory()) {
                pout.println("error");
                System.out.println("La carpeta no existe o no es un directorio.");
                return;
            } else {
                pout.println("ready");
            }

            String archivoZip = nombreCarpeta + ".zip";
            try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(archivoZip))) {
                File fileToZip = new File(nombreCarpeta);
                comprimirArchivo(fileToZip, fileToZip.getName(), zipOut);
            } catch (IOException e) {
                e.printStackTrace();
            }

            File file = new File(archivoZip);

            OutputStream os = socketFile.getOutputStream();
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);

            byte[] buffer = new byte[1024];
            int count;

            while ((count = bis.read(buffer)) > 0) {
                os.write(buffer, 0, count);
            }
            os.flush();
            bis.close();
            os.close();
            file.delete();
            System.out.println("Directorio enviado");

        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private static void descargarCarpeta(String archivo){
        String nombreDirectorio = setDirectorio();
        String nombreArchivo = setDirectorio() + "/" + archivo + ".zip";
        File file = new File(nombreArchivo);

        // Declaramos un nuevo socket para enviar archivos
        String serverAddress = "localhost";
        int port = 7778;

        try (Socket socketFiles = new Socket(serverAddress, port)){
            BufferedReader in = new BufferedReader(new InputStreamReader(socketFiles.getInputStream()));

            String respuesta = in.readLine();

            if(respuesta.equals("error")){
                System.out.println("El directorio no existe");
                return;
            }

            InputStream is = socketFiles.getInputStream();
            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);

            byte[] buffer = new byte[1024];
            int count;

            while ((count = is.read(buffer)) > 0){
                bos.write(buffer, 0, count);
            }

            bos.flush();
            bos.close();
            is.close();
            descomprimirArchivo(nombreArchivo, nombreDirectorio);
            file.delete();
            System.out.println("Directorio descargado correctamente");
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void descomprimirArchivo(String archivoZip, String destino) throws IOException {
        File destDir = new File(destino);
        if (!destDir.exists()) {
            if (!destDir.mkdirs()) {
                System.out.println("No se pudo crear el directorio destino.");
                return;
            }
        }
    
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(archivoZip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // Crear una ruta sin duplicar directorios
                File file = new File(destDir, entry.getName());
    
                // Si la entrada es un directorio, lo creamos
                if (entry.isDirectory()) {
                    if (!file.exists() && !file.mkdirs()) {
                        System.out.println("No se pudo crear el directorio: " + file);
                    }
                } else {
                    // Si la entrada es un archivo, lo escribimos
                    try {
                        // Asegurarse de que el directorio del archivo exista antes de escribirlo
                        File parentDir = file.getParentFile();
                        if (!parentDir.exists() && !parentDir.mkdirs()) {
                            System.out.println("No se pudo crear el directorio: " + parentDir);
                        }
    
                        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
                            byte[] buffer = new byte[1024];
                            int count;
                            while ((count = zis.read(buffer)) > 0) {
                                bos.write(buffer, 0, count);
                            }
                        }
                    } catch (IOException e) {
                        System.out.println("Error al escribir el archivo: " + file);
                        e.printStackTrace();
                    }
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            System.out.println("Error al descomprimir el archivo.");
            e.printStackTrace();
        }
    }
    
    private static void comprimirArchivo(File file, String nombreRelativo, ZipOutputStream zipOut) throws IOException {
        if (file.isDirectory()) {
            for (File subFile : file.listFiles()) {
                comprimirArchivo(subFile, nombreRelativo + "/" + subFile.getName(), zipOut);
            }
        } else {
            try (FileInputStream fis = new FileInputStream(file)) {
                ZipEntry zipEntry = new ZipEntry(nombreRelativo);
                zipOut.putNextEntry(zipEntry);
    
                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) >= 0) {
                    zipOut.write(buffer, 0, length);
                }
    
                zipOut.closeEntry();
            }
        }
    }

    private static void crearArchivo(PrintWriter out, String nombreArchivo){
        String directorio = setDirectorio() + "/" + nombreArchivo;
        File archivo = new File(directorio);

        try {
            if(archivo.createNewFile()){
                out.println("ready");
                System.out.println("Archivo creado exitosamente");
            } else {
                out.println("notAllowed");
                System.out.println("El archivo ya existe");
            }
        } catch (IOException e) {
            out.println("error");
            System.out.println("Error al crear el archivo");
            e.printStackTrace();
        }
    }

    private static void crearCarpeta(PrintWriter out, String nombreCarpeta){
        String directorio = setDirectorio() + "/" + nombreCarpeta;
        File carpeta = new File(directorio);

        if(carpeta.mkdir()){
            out.println("ready");
            System.out.println("Carpeta creada exitosamente");
        } else {
            out.println("notAllowed");
            System.out.println("La carpeta ya existe");
        }
    }

    private static void renombrar(PrintWriter out, String nombre, String nombreNuevo){
        String directorio = setDirectorio() + "/" + nombre;
        String directorio2 = setDirectorio() + "/" + nombreNuevo;

        File original = new File(directorio);
        File nuevo = new File(directorio2);

        if(original.renameTo(nuevo)){
            out.println("ready");
            System.out.println("Archivo o directorio renombrado correctamente");
        } else {
            out.println("error");
            System.out.println("Error al renombrar el archivo o directorio");
        }
    }

    private static void eliminarArchivo(PrintWriter out, String nombreArchivo){
        String directorio = setDirectorio() + "/" + nombreArchivo;

        File archivo = new File(directorio);

        if(archivo.delete()){
            out.println("ready");
            System.out.println("Archivo eliminado correctamente");
        } else {
            out.println("error");
            System.out.println("No se pudo eliminar el archivo");
        }
    }

    private static void eliminarCarpeta(PrintWriter out, String nombreCarpeta){
        String directorio = setDirectorio() + "/" + nombreCarpeta;

        File carpeta = new File(directorio);

        if(eliminarRecursivo(carpeta)){
            out.println("ready");
            System.out.println("Carpeta eliminada correctamente");
        } else {
            out.println("error");
            System.out.println("No se puso eliminar la carpeta");
        }
    }

    private static boolean eliminarRecursivo(File carpeta){
        if(!carpeta.exists()) return false;

        File[] archivos = carpeta.listFiles();

        if(archivos != null){
            for(File archivo : archivos){
                if(archivo.isDirectory()){
                    eliminarRecursivo(archivo);
                } else {
                    archivo.delete();
                }
            }
        }
        return carpeta.delete();
    }
}
