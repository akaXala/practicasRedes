// Carpeta donde se ubica el cliente
package Cliente;

// Bibliotecas
import java.nio.channels.*;
import java.nio.*;
import java.net.*;
import java.util.Iterator;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.io.*;

public class Cliente {
    // Configuraciones servidor
    private static final String serverHost = "127.0.0.1";   // localhost
    private static final int serverPort = 7777;

    private static final String baseDirectory = "./Cliente/Archivos";   // Carpeta raíz
    private static Stack<String> directories = new Stack<>();           // Pila para las direciones
    private static String dir = "";                                     // Directorio actual

    public static void main(String[] args) {
        try {            
            // Creamos el socket no bloqueante
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);

            // Creamos un selector para configurar las conexiones
            Selector selector = Selector.open();

            // Nos conectamos al socket de servidor y registramos la conexion
            socketChannel.connect(new InetSocketAddress(serverHost, serverPort));
            socketChannel.register(selector, SelectionKey.OP_CONNECT);

            // Bucle While para ejecutar los comandos
            while (true) {
                selector.select();  // Selecionamos una key de conexion de entrada o salida
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();   // Agregamos la key al iterador
            
                // Si hay keys en el iterator
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = (SelectionKey) iterator.next(); // Damos una key del iterator
                    iterator.remove();  // Y la removemos del iterator

                    // Si la key se puede conectar al servidor
                    if (selectionKey.isConnectable()) {
                        SocketChannel socketChannel2 = (SocketChannel) selectionKey.channel();  // Retorna el canal de la conexion
                    
                        // Si la conexión es pendiente
                        if (socketChannel2.isConnectionPending()) {
                            try {
                                socketChannel2.finishConnect(); // Terminamos de conectar
                                System.out.println("Cliente conectado al servidor");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        // Registramos la conexion
                        socketChannel2.register(selector, SelectionKey.OP_READ|SelectionKey.OP_WRITE);
                        continue;
                    }

                    // Leemos la linea de comando
                    System.out.println("\nEscriba un comando...");
                    System.out.print("> ");

                    // Leemos la entrada de texto en un buffer
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                    // Obtenemos lo escrito en la linea de comandos
                    String line = br.readLine();

                    // Separamos en comando y argumento
                    String[] partes = line.split(" ", 4);
                    String argumento = partes[0];
                    String argumento1 = (partes.length > 1) ? partes[1] : "";
                    String argumento2 = (partes.length > 2) ? partes[2] : "";

                    // Comandos
                    if (argumento.equals("ls")) {   // Listar archivos
                        if (directories.empty()) {
                            listLocalFiles(baseDirectory);  // Listamos archivos/carpetas en la carpeta raíz
                            continue;
                        } else {
                            listLocalFiles(dir);             // Listamos archivos/carpetas en la ruta actual
                            continue;
                        }

                    } else if (argumento.equals("cd")) {     // Nos movemos por los directorios
                        moveDirectory(argumento1);
                        continue;

                    } else if (argumento.equals("cp")) {      // Copiar archivos C -> S
                        sendFile(argumento1, socketChannel);
                        continue;
                    } else if (argumento.equals("cpdir")) {   // Copiar directorios C -> S
                        sendFolder(argumento1, socketChannel);
                        continue;

                    } else if (argumento.equals("touch")) {   // Crear archivo vacio
                        createFile(argumento1);
                        continue;

                    } else if (argumento.equals("mkdir")) {   // Crear carpeta
                        createFolder(argumento1);
                        continue;

                    } else if (argumento.equals("mv")) {      // Renombrar archivo/carpeta
                        rename(argumento1, argumento2);
                        continue;
                        
                    } else if (argumento.equals("rm")) {      // Eliminar archivo
                        deleteFile(argumento1);
                        continue;

                    } else if (argumento.equals("rmdir")) {   // Eliminar carpeta
                        deleteFolder(argumento1);
                        continue;

                    } else if (argumento.equals("cls")) {   // Borrar pantalla
                        clearTerminal();
                        continue;
                    }

                    // Si podemos escribir en la conexión
                    if (selectionKey.isWritable()) {
                        SocketChannel socketChannel2 = (SocketChannel) selectionKey.channel(); // Retorna el canal de la conexion
                        
                        // Escribimos el comando en el buffer
                        ByteBuffer bb = ByteBuffer.wrap(line.getBytes());
                        socketChannel2.write(bb);

                        if (argumento.equalsIgnoreCase("exit")) {    // Terminar conexion
                            System.out.println("Cliente cerrado");
                            socketChannel2.close(); // Cerramos el canal de conexión
                            System.exit(0);

                        } else if (argumento.equals("SRV")) {   // Comandos del servidor
                            selectionKey.interestOps(SelectionKey.OP_READ); // Cambiamos el socket a lectura
                            selector.select();  // Esperamos a que haya datos

                            if (selectionKey.isReadable()) {
                                // En caso de recibir un archivo o carpeta
                                if (argumento1.equals("cp")) {
                                    receiveFile(argumento2, socketChannel2);
                                } else if (argumento1.equals("cpdir")){
                                    receiveFolder(argumento2, socketChannel2);
                                } else {
                                    // Preparamos un buffer de lectura
                                    ByteBuffer readBuffer = ByteBuffer.allocate(2000);

                                    // Stringbuilder para las respuestas
                                    StringBuilder responseString = new StringBuilder();

                                    // Limpiamos y guardamos la respuesta en el buffer
                                    readBuffer.clear();
                                    int bytesRead = socketChannel2.read(readBuffer);

                                    // Escribimos la respuesta en una String
                                    if (bytesRead > 0) {
                                        readBuffer.flip();
                                        String response = new String(readBuffer.array(), 0, bytesRead);
                                        responseString.append(response);
                                    }

                                    // Imprimimos la respuesta
                                    System.out.println(responseString.toString());
                                }
                                
                                // Permitimos escritura nuevamente
                                selectionKey.interestOps(SelectionKey.OP_WRITE);
                            }
                            continue;
                        }
                    } 
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------------------- PARTE DE FUNCIONES DEL CLIENTE ---------------------------- //
    private static void listLocalFiles(String directory) {
        // Mostramos los archivos del cliente
        System.out.println("\n--- Mostrando archivos locales ---");
        System.out.println(directory);
        File path = new File(directory);
        File[] archivos = path.listFiles();

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

    private static String setDirectory(){
        StringBuilder actualDirectory = new StringBuilder();

        actualDirectory.append(baseDirectory);
        for(int i = 0; i < directories.size(); i++){
            actualDirectory.append("/");
            actualDirectory.append(directories.get(i));
        }

        return actualDirectory.toString();
    }

    private static void moveDirectory(String argument){
        if(argument.equals("..")){
            if(directories.empty()){
                System.out.println("Ya estas en la carpeta raíz");
            } else {
                directories.pop();
            }
            dir = setDirectory();
        } else {
            // Verificar si el directorio existe
            String newDirectory = setDirectory() + "/" + argument;
            File file = new File(newDirectory);

            if(file.exists() && file.isDirectory()){
                directories.push(argument);
                dir = setDirectory();
            } else {
                System.out.println("El directorio especificado no existe");
            }
        }
    }

    private static void sendFile(String fileName, SocketChannel socketChannel) throws IOException {
        String filePath = setDirectory() + "/" + fileName;
        File file = new File(filePath);

        if (!file.exists() || !file.isFile()) {
            System.out.println("El archivo no existe\n");
            return;
        }

        // Enviar comando al servidor
        String command = "cp " + fileName;
        ByteBuffer cmdBuffer = ByteBuffer.wrap(command.getBytes());
        socketChannel.write(cmdBuffer);

        // Enviar tamaño del archivo
        long fileSize = file.length();
        ByteBuffer sizeBuffer = ByteBuffer.allocate(Long.BYTES);
        sizeBuffer.putLong(fileSize);
        sizeBuffer.flip();
        socketChannel.write(sizeBuffer);

        // Enviar el archivo en bloques
        try (FileInputStream fis = new FileInputStream(file);
             FileChannel fileChannel = fis.getChannel()) {
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            while (fileChannel.read(buffer) > 0) {
                buffer.flip();
                socketChannel.write(buffer);
                buffer.clear();
            }
        }

        System.out.println("Archivo enviado al servidor.");
    }

    private static void sendFolder(String folderName, SocketChannel socketChannel) throws IOException {
        String folderPath = setDirectory() + "/" + folderName;
        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("La carpeta no existe");
            return;
        }

        // Comprimir la carpeta en zip
        String zipFolder = folderPath + ".zip";
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFolder))) {
            File folderToZip = new File(folderPath);
            compressFolder(folderToZip, folderToZip.getName(), zipOut);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        File zip = new File(zipFolder);

        // Enviar comando al servidor
        String command = "cpdir " + folderName;
        ByteBuffer cmdBuffer = ByteBuffer.wrap(command.getBytes());
        socketChannel.write(cmdBuffer);

        // Enviar tamaño del zip
        long zipSize = zip.length();
        ByteBuffer sizeBuffer = ByteBuffer.allocate(Long.BYTES);
        sizeBuffer.putLong(zipSize);
        sizeBuffer.flip();
        socketChannel.write(sizeBuffer);

        // Enviar el zip en bloques
        try (FileInputStream fis = new FileInputStream(zip);
             FileChannel fileChannel = fis.getChannel()) {
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            while (fileChannel.read(buffer) > 0) {
                buffer.flip();
                socketChannel.write(buffer);
                buffer.clear();
            }
        }
        zip.delete();
        System.out.println("Carpeta enviada al servidor.");
    }

    private static void compressFolder(File file, String relativeName, ZipOutputStream zipOut) throws IOException {
        if (file.isDirectory()) {
            for (File subFile : file.listFiles()) {
                compressFolder(subFile, relativeName + "/" + subFile.getName(), zipOut);
            }
        } else {
            try (FileInputStream fis = new FileInputStream(file)) {
                ZipEntry zipEntry = new ZipEntry(relativeName);
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

    private static void createFile(String fileName) {
        String path = setDirectory() + "/" + fileName;
        File file = new File(path);

        try {
            if (file.createNewFile()) {
                System.out.println("El archivo fue creado exitosamente");
            } else {
                System.out.println("El archivo ya existe");
            }
        } catch (IOException e) {
            System.out.println("Error al crear el archivo");
            e.printStackTrace();
        }
    }

    private static void createFolder(String folderName){
        String path = setDirectory() + "/" + folderName;
        File folder = new File(path);

        if(folder.mkdir()){
            System.out.println("Carpeta creada exitosamente");
        } else {
            System.out.println("La carpeta ya existe");
        }
    }

    private static void rename(String name, String newName){
        String actualDirectory = setDirectory();
        String directory = actualDirectory + "/" + name;
        String directory2 = actualDirectory + "/" + newName;

        File originalPath = new File(directory);
        File newPath = new File(directory2);

        if(originalPath.renameTo(newPath)){
            System.out.println("Archivo o directorio renombrado correctamente");
        } else {
            System.out.println("Error al renombrar el archivo o directorio");
        }
    }

    private static void deleteFile(String fileName){
        String path = setDirectory() + "/" + fileName;

        File file = new File(path);

        if(file.delete()){
            System.out.println("Archivo eliminado correctamente");
        } else {
            System.out.println("No se pudo eliminar el archivo");
        }
    }

    private static void deleteFolder(String folderName){
        String path = setDirectory() + "/" + folderName;

        File folder = new File(path);

        if(deleteRecursive(folder)){
            System.out.println("Carpeta eliminada correctamente");
        } else {
            System.out.println("No se puso eliminar la carpeta");
        }
    }

    private static boolean deleteRecursive(File folder){
        if (!folder.exists()) return false;

        File[] files = folder.listFiles();

        if (files != null){
            for(File file : files){
                if(file.isDirectory()){
                    deleteRecursive(file);
                } else {
                    file.delete();
                }
            }
        }

        return folder.delete();
    }

    private static void clearTerminal() {
        try {
            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // ---------------------------- PARTE PARA RECIBIR ARCHIVOS DEL SERVIDOR ---------------------------- //
    private static void receiveFile(String fileName, SocketChannel socketChannel) throws IOException {
        // Leer tamaño del archivo primero
        ByteBuffer sizeBuffer = ByteBuffer.allocate(Long.BYTES);
        socketChannel.read(sizeBuffer);
        sizeBuffer.flip();
        long fileSize = sizeBuffer.getLong();

        // Recibir el archivo
        String savePath = setDirectory() + "/" + fileName;
        try (FileOutputStream fos = new FileOutputStream(savePath);
            FileChannel fileChannel = fos.getChannel()) {
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            long bytesReceived = 0;
            while (bytesReceived < fileSize) {
                int read = socketChannel.read(buffer);
                if (read == -1) break;
                buffer.flip();
                fileChannel.write(buffer);
                bytesReceived += read;
                buffer.clear();
            }
        }

        // Notificamos de recibido
        System.out.println("Archivo recibido y guardado en: " + savePath);
    }

    private static void receiveFolder(String fileName, SocketChannel socketChannel) throws IOException {
        // Leer tamaño del archivo primero
        ByteBuffer sizeBuffer = ByteBuffer.allocate(Long.BYTES);
        socketChannel.read(sizeBuffer);
        sizeBuffer.flip();
        long fileSize = sizeBuffer.getLong();

        // Recibir el archivo zip
        String savePath = setDirectory() + "/" + fileName + ".zip";
        String folderPath = setDirectory() + "/" + fileName;
        try (FileOutputStream fos = new FileOutputStream(savePath);
            FileChannel fileChannel = fos.getChannel()) {
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            long bytesReceived = 0;
            while (bytesReceived < fileSize) {
                int read = socketChannel.read(buffer);
                if (read == -1) break;
                buffer.flip();
                fileChannel.write(buffer);
                bytesReceived += read;
                buffer.clear();
            }
        }

        // Descomprimimos el zip y lo borramos
        unzipFolder(savePath, setDirectory());
        File zip = new File(savePath);
        zip.delete();

        // Notificamos de recibido
        System.out.println("Carpeta recibido y guardado en: " + folderPath);
    }

    private static void unzipFolder(String zipFile, String path) {
        File pathZip = new File(path);

        if (!pathZip.exists()) {
            if (!pathZip.mkdirs()) {
                System.out.println("No se pudo crear el directorio destino.");
                return;
            }
        }

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                // Crear una ruta sin duplicar directorios
                File file = new File(pathZip, entry.getName());
    
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
        } catch (Exception e) {
            System.out.println("Error al descomprimir el archivo.");
            e.printStackTrace();
        }
    }
}
