// Carpeta donde se ubica el cliente
package Cliente;

//Bibliotecas
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.Stack;
import java.util.zip.*;

public class Cliente {
    private static String directorioBase = "./Cliente/Archivos";    // Definimos carpeta raíz
    private static Stack<String> directorios = new Stack<>();       // Pila para las direcciones
    private static String dir = "";                                 // Dirección actual
    //private static String outputFilePath = "./Cliente/Archivos/Descargas/descargado.jpg";  // Dirección por defecto para las descargas
    public static void main(String[] args) {
        // Scanner (leer texto en terminal)
        Scanner sc = new Scanner(System.in);

        // Configuraciones de conexion
        String serverAddress = "localhost";
        int port = 7777;

        try (Socket socket = new Socket(serverAddress, port)) {
            // Variables para enviar/recibir texto
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("Cliente conectado al servidor");
            listarArchivosLocales(directorioBase);
            // Leer y mostrar la lista de archivos recibida del servidor
            listarArchivosRemotos(in);

            while (true) {
                // Nos movemos de directorio
                System.out.println("\nEscriba HELP para obtener más información ");
                System.out.print("> ");
                String line = sc.nextLine();

                // Separamos en comando y argumento
                String[] partes = line.split(" ", 4);
                String argumento = partes[0];
                String argumento1 = (partes.length > 1) ? partes[1] : "";
                String argumento2 = (partes.length > 2) ? partes[2] : "";
                String argumento3 = (partes.length > 3) ? partes [3] : "";

                // Opciones
                if(argumento.equals("SRV")){
                    if(argumento1.equals("ls")){    // Listar archivos
                        out.println(argumento1);    // Enviamos el comando al servidor
                        listarArchivosRemotos(in);  // Invocamos a la función

                    }else if (argumento1.equals("cd")){ // Cambiar directorio
                        out.println(argumento1);    // Enviamos el comando al servidor
                        out.println(argumento2);    // Enviamos el nombre del directorio o ".."
                        moverDirectorioRemoto(argumento2, in);  // Invocamos a la función

                    } else if (argumento1.equals("cp")){    // Copiar archivos S -> C
                        out.println(argumento1);    // Enviamos el comando al servidor
                        out.println(argumento2);    // Seleccionamos el archivo o la ruta del archivo del Servidor
                        descargarArchivo(argumento2); // Invocamos a la funcion

                    } else if (argumento1.equals("cpdir")){    // Copiar directorios S -> C
                        out.println(argumento1);    // Enviamos el comando al servidor
                        out.println(argumento2);    // Seleccionamos la ruta del directorio del Servidor
                        descargarCarpeta(argumento2); // Invocamos a la funcion

                    } else if (argumento1.equals("touch")){ // Crear archivo vacio
                        out.println(argumento1);    // Enviamos el comando
                        out.println(argumento2);    // Enviamos el nombre del archivo
                        crearArchivoRemoto(in);     // Invocamos a la funcion

                    } else if (argumento1.equals("mkdir")){    // Crear carpeta
                        out.println(argumento1);    // Enviamos el comando
                        out.println(argumento2);    // Enviamos el nombre del archivo
                        crearCarpetaRemoto(in);     // Invocamos a la funcion

                    } else if (argumento1.equals("mv")){    // Renombrar archivo/carpeta
                        out.println(argumento1);    // Enviamos el comando
                        out.println(argumento2);    // Enviamos el nombre actual
                        out.println(argumento3);    // Enviamos el nombre nuevo
                        renombrarRemoto(in);        // Invocamos a la función

                    } else if (argumento1.equals("rm")) {   // Eliminar archivo
                        out.println(argumento1);    // Enviamos el comando
                        out.println(argumento2);    // Enviamos el nombre del archivo
                        eliminarArchivoRemoto(in);  // Invocamos a la función

                    } else if (argumento1.equals("rmdir")){ // Eliminar directorio
                        out.println(argumento1);    // Enviamos el comando
                        out.println(argumento2);    // Enviamos el nombre del directorio
                        eliminarCarpetaRemoto(in);  // Invocamos a la función

                    } else {    // En caso de no ingresar algo valido despues de SRV
                        System.out.println("");
                    }
                } else if(argumento.equals("ls")) {    // Comandos para el cliente
                    if(directorios.empty()){
                        listarArchivosLocales(directorioBase);  // Listamos archivos/carpetas en la carpeta raíz
                    } else {
                        listarArchivosLocales(dir);             // Listamos archivos/carpetas en la ruta actual
                    }

                } else if(argumento.equals("cd")){     // Nos movemos por los directorios
                    moverDirectorio(argumento1);

                } else if(argumento.equals("cp")){      // Copiar archivos C -> S
                    out.println("send");                       // Avisamos al servidor que vamos a subir un archivo
                    out.println(argumento);                      // Mandamos el comando
                    out.println(argumento1);                     // Mandamos el nombre del archivo
                    enviarArchivo(argumento1);                   // Invocamos a la función

                } else if(argumento.equals("cpdir")){   // Copiar directorios C -> S
                    out.println("send");                       // Avisamos al servidor que vamos a subir un directorio
                    out.println(argumento);                      // Mandamos el comando
                    out.println(argumento1);                     // Mandamos el nombre del directorio
                    enviarCarpeta(argumento1);                   // Invocamos a la función

                } else if(argumento.equals("touch")){   // Crear archivo vacio
                    crearArchivo(argumento1);

                } else if(argumento.equals("mkdir")){   // Crear carpeta
                    crearCarpeta(argumento1);

                } else if(argumento.equals("mv")){      // Renombrar archivo/carpeta
                    renombrar(argumento1, argumento2);
                    
                } else if(argumento.equals("rm")){      // Eliminar archivo
                    eliminarArchivo(argumento1);

                } else if(argumento.equals("rmdir")){   // Eliminar carpeta
                    eliminarCarpeta(argumento1);

                } else if (argumento.equals("exit")) {    // Terminar ejecución
                    out.println(argumento);
                    break;
                } else {    // No se ingreso algo valido
                    System.out.println("");
                }
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        sc.close();
    }

    private static void listarArchivosLocales(String dir){
        // Mostramos los archivos del cliente
        System.out.println("--- Mostrando archivos locales ---");
        File directorio = new File(dir);
        File[] archivos = directorio.listFiles();

        // Listamos los archivos
        if(archivos != null){
            for(File archivo : archivos){
                System.out.println(archivo.getName());
            }
        } else {
            System.out.println("No se pudo acceder al directorio");
        }
        System.out.println();
    }

    private static void listarArchivosRemotos(BufferedReader in) throws IOException {
        // Leer y mostrar la lista de archivos recibida del servidor
        String linea;
        System.out.println("--- Mostrando archivos en el servidor ---");
        while((linea = in.readLine()) != null){
            if(linea.equals("END")) break;
            System.out.println(linea);
        }
    }

    private static void moverDirectorio(String argumento){
        if(argumento.equals("..")){
            if(directorios.empty()){
                System.out.println("Ya estas en la carpeta raíz");
            } else {
                directorios.pop();
            }
            dir = setDirectorio();
        } else {
            // Verificar si el directorio existe
            String nuevoDirectorio = setDirectorio() + "/" + argumento;
            File file = new File(nuevoDirectorio);

            if(file.exists() && file.isDirectory()){
                directorios.push(argumento);
                dir = setDirectorio();
            } else {
                System.out.println("El directorio especificado no existe");
            }
        }
    }
    
    private static void moverDirectorioRemoto(String argumento, BufferedReader in) throws IOException{
        String respuesta = in.readLine();
        
        if(argumento.equals("..")){
            if(respuesta.equals("empty")){
                System.out.println("Ya estas en la carpeta raíz");
            } else if(respuesta.equals("back")){
                System.out.println();
            }
        } else {
            if(respuesta.equals("error")){
                System.out.println("El directorio no existe");
            } else if(respuesta.equals("ready")) {
                System.out.println();
            }
        }
    }

    private static String setDirectorio(){
        StringBuilder directorioActual = new StringBuilder();

        directorioActual.append(directorioBase);
        for(int i = 0; i<directorios.size(); i++){
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
    
    private static void crearArchivo(String nombreArchivo){
        String directorio = setDirectorio() + "/" + nombreArchivo;
        File archivo = new File(directorio);

        try {
            if(archivo.createNewFile()){
                System.out.println("Archivo creado exitosamente");
            } else {
                System.out.println("El archivo ya existe");
            }
        } catch (IOException e) {
            System.out.println("Error al crear el archivo");
            e.printStackTrace();
        }

    }

    private static void crearArchivoRemoto(BufferedReader in) throws IOException {
        String respuesta = in.readLine();

        if(respuesta.equals("ready")){
            System.out.println("Archivo creado exitosamente");
        } else if(respuesta.equals("notAllowed")){
            System.out.println("El archivo ya existe");
        } else if(respuesta.equals("error")){
            System.out.println("Error al crear el archivo");
        }
    }

    private static void crearCarpeta(String nombreCarpeta){
        String directorio = setDirectorio() + "/" + nombreCarpeta;
        File carpeta = new File(directorio);

        if(carpeta.mkdir()){
            System.out.println("Carpeta creada exitosamente");
        } else {
            System.out.println("La carpeta ya existe");
        }

    }

    private static void crearCarpetaRemoto(BufferedReader in) throws IOException {
        String respuesta = in.readLine();

        if(respuesta.equals("ready")){
            System.out.println("Carpeta creada exitosamente");
        } else if(respuesta.equals("notAllowed")){
            System.out.println("La carpeta ya existe");
        }
    }

    private static void renombrar(String nombre, String nombreNuevo){
        String directorioActual = setDirectorio();
        String directorio = directorioActual + "/" + nombre;
        String directorio2 = directorioActual + "/" + nombreNuevo;

        File original = new File(directorio);
        File nuevo = new File(directorio2);

        if(original.renameTo(nuevo)){
            System.out.println("Archivo o directorio renombrado correctamente");
        } else {
            System.out.println("Error al renombrar el archivo o directorio");
        }

    }

    private static void renombrarRemoto(BufferedReader in) throws IOException {
        String respuesta = in.readLine();

        if(respuesta.equals("ready")){
            System.out.println("Archivo o directorio renombrado correctamente");
        } else if (respuesta.equals("error")) {
            System.out.println("Error al renombrar el archivo o directorio");
        }
    }

    private static void eliminarArchivo(String nombreArchivo){
        String directorio = setDirectorio() + "/" + nombreArchivo;

        File archivo = new File(directorio);

        if(archivo.delete()){
            System.out.println("Archivo eliminado correctamente");
        } else {
            System.out.println("No se pudo eliminar el archivo");
        }
    }

    private static void eliminarArchivoRemoto(BufferedReader in) throws IOException {
        String respuesta = in.readLine();

        if(respuesta.equals("ready")){
            System.out.println("Archivo eliminado correctamente");
        } else if (respuesta.equals("error")) {
            System.out.println("No se pudo eliminar el archivo");
        }
    }

    private static void eliminarCarpeta(String nombreCarpeta){
        String directorio = setDirectorio() + "/" + nombreCarpeta;

        File carpeta = new File(directorio);

        if(eliminarRecursivo(carpeta)){
            System.out.println("Carpeta eliminada correctamente");
        } else {
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

    private static void eliminarCarpetaRemoto(BufferedReader in) throws IOException {
        String respuesta = in.readLine();

        if(respuesta.equals("ready")){
            System.out.println("Carpeta eliminada correctamente");
        } else if (respuesta.equals("error")) {
            System.out.println("No se puso eliminar la carpeta");
        }

    }
}
