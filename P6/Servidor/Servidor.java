// Carpeta donde se ubica el servidor
package Servidor;

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

public class Servidor {
    // Configuraciones servidor
    private static final int serverPort = 7777;
    private static final String baseDirectory = "./Servidor/Archivos";
    private static Stack<String> directories = new Stack<>();
    private static String dir = "";

    public static void main(String[] args) {
        try {
            // Creamos y configuramos el socket de servidor no bloqueante
            ServerSocketChannel serverSocket = ServerSocketChannel.open();
            serverSocket.configureBlocking(false);  // Para que sea no bloqueante
            serverSocket.socket().bind(new InetSocketAddress(serverPort));   // Asignamos el puerto

            // Creamos un selector para aceptar las conexiones
            Selector selector = Selector.open();

            // Asignamos el selector a nuestro socket
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Servidor esperando conexiones en el puerto " + serverPort + "...");

            // Bucle While para siempre aceptar conexiones
            while (true) {
                selector.select();  // Selecionamos una key de conexion de entrada o salida
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();   // Agregamos las keys al iterador

                // Si hay keys en el iterator
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = (SelectionKey) iterator.next();   // Damos una key del iterator
                    iterator.remove();  // Y la removemos del iterator

                    // Si la key esta lista para conexion
                    if (selectionKey.isAcceptable()) {
                        SocketChannel socketChannel = serverSocket.accept(); // Aceptamos la conexion al servidor
                        System.out.println("El cliente " + socketChannel.socket().getInetAddress() + ":" + socketChannel.socket().getPort() + " se ha conectado");

                        // Configuramos la conexion a no bloqueante y registramos
                        socketChannel.configureBlocking(false);
                        socketChannel.register(selector, SelectionKey.OP_READ); // Socket de lectura
                        continue;
                    }

                    // Si la key esta lista para leer
                    if (selectionKey.isReadable()) {
                        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();   // Retorna el canal de la conexion

                        // Creamos un buffer de bytes y lo limpiamos
                        ByteBuffer bb = ByteBuffer.allocate(2000);
                        bb.clear();

                        // Leemos el mensaje
                        int n = socketChannel.read(bb);
                        bb.flip();

                        // Leemos el comando enviado por el socket y lo dividimos en sus argumentos
                        String line = new String(bb.array(), 0, n);
                        String[] parts = line.split(" ", 4);
                        String command = (parts.length > 1) ? parts[1] : "";
                        String argument = (parts.length > 2) ? parts[2] : "";
                        String argument1 = (parts.length > 3) ? parts[3] : "";

                        System.out.println(socketChannel.socket().getInetAddress() + ":" + socketChannel.socket().getPort() + " peticion: " + line);
                        
                        // Comandos S->C
                        if (command.equals("ls")) { // Listar archivos
                            if (directories.empty()) {      // Comprobamos si estamos en la carpeta raíz
                                listFiles(baseDirectory, socketChannel);    // Listamos archivos/carpetas en la carpeta raíz
                            } else {
                                listFiles(dir, socketChannel);              // Listamos archivos/carpetas en la ruta actual
                            }
                            continue;

                        } else if (command.equals("cd")) {      // Nos movemos por los directorios
                            moveDirectory(argument, socketChannel);
                            continue;

                        } else if (command.equals("cp")) {      // Copiar archivos S -> C
                            sendFile(argument, socketChannel);
                            continue;

                        } else if (command.equals("cpdir")) {   // Copiar carpetas S -> C
                            sendFolder(argument, socketChannel);
                            continue;

                        } else if (command.equals("touch")) {   // Crear archivo vacio
                            createFile(argument, socketChannel);
                            continue;

                        } else if (command.equals("mkdir")) {   // Crear carpeta
                            createFolder(argument, socketChannel);
                            continue;

                        } else if (command.equals("mv")) {      // Renombrar archivo/carpeta
                            rename(argument, argument1, socketChannel);
                            continue;

                        } else if (command.equals("rm")) {      // Eliminar archivo
                            deleteFile(argument, socketChannel);
                            continue;
                        
                        } else if (command.equals("rmdir")) {   // Eliminar carpeta
                            deleteFolder(argument, socketChannel);
                            continue;
                        }

                        // Comandos C -> S
                        if (line.startsWith("cp ")) {
                            String fileName = (parts.length > 1) ? parts[1] : "";
                            receiveFile(fileName, socketChannel);
                            continue;
                        } else if (line.startsWith("cpdir ")) {
                            String folderName = (parts.length > 1) ? parts[1] : "";
                            receiveFolder(folderName, socketChannel);
                            continue;
                        } else if (line.equals("exit")) { // Terminar conexion
                            System.out.println("El cliente " + socketChannel.socket().getInetAddress() + ":" + socketChannel.socket().getPort() + " se ha desconectado");
                            socketChannel.close();
                            continue;
                        } else {
                            System.out.println("Error de peticion.");
                            continue;
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------------------- PARTE DE FUNCIONES DEL SERVIDOR ---------------------------- //
    private static void listFiles(String directory, SocketChannel socketChannel) throws IOException {
        // Paths para lecturas
        File path = new File(directory);
        File[] files = path.listFiles();
        
        // Stringbuilder para respuesta
        StringBuilder sb = new StringBuilder();
        sb.setLength(0);

        // Agregamos un encabezado
        sb.append("\n");
        sb.append("SERVIDOR: --- Mostrando archivos en el servidor ---\n");
        sb.append(directory);
        sb.append("\n");

        // Creamos la lista
        if (files != null) {
            for (File file : files) {
                sb.append(file.getName()).append("\n");
            }
        } else {
            System.out.println("No se pudo acceder al directorio");
            sb.append("No se pudo acceder al directorio");
        }

        // Enviamos la lista
        ByteBuffer bb = ByteBuffer.wrap(sb.toString().getBytes());
        socketChannel.write(bb);
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

    private static void moveDirectory(String argument, SocketChannel socketChannel)  throws IOException {
        // Stringbuilder para respuesta
        StringBuilder sb = new StringBuilder();
        sb.setLength(0);
        sb.append("\n");
        
        if(argument.equals("..")){
            if(directories.empty()){
                System.out.println("Ya estas en la carpeta raíz");
                sb.append("SERVIDOR: Ya estas en la carpeta raíz");
            } else {
                directories.pop();
                sb.append("SERVIDOR: Ruta actual\n");
            }
            dir = setDirectory();
            sb.append(dir);
        } else {
            // Verificar si el directorio existe
            String newDirectory = setDirectory() + "/" + argument;
            File file = new File(newDirectory);

            if(file.exists() && file.isDirectory()){
                directories.push(argument);
                dir = setDirectory();
                sb.append("SERVIDOR: Ruta actual\n");
                sb.append(dir);
            } else {
                System.out.println("El directorio especificado no existe");
                sb.append("SERVIDOR: El directorio especificado no existe");
            }
        }

        // Enviamos la respuesta
        ByteBuffer bb = ByteBuffer.wrap(sb.toString().getBytes());
        socketChannel.write(bb);
    }

    private static void sendFile(String fileName, SocketChannel socketChannel) throws IOException {
        // Creamos la ruta
        String filePath = setDirectory() + "/" + fileName;
        File file = new File(filePath);

        // Checamos si existe
        if (!file.exists() || !file.isFile()) {
            String msg = "SERVIDOR: El archivo no existe\n";
            socketChannel.write(ByteBuffer.wrap(msg.getBytes()));
            return;
        }

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

        System.out.println("Archivo enviado al cliente");
    }

    private static void sendFolder(String folderName, SocketChannel socketChannel) throws IOException {
        // Creamos la ruta
        String folderPath = setDirectory() + "/" + folderName;
        File folder = new File(folderPath);

        // Checamos si existe
        if (!folder.exists() || !folder.isDirectory()) {
            String msg = "SERVIDOR: La carpeta no existe\n";
            socketChannel.write(ByteBuffer.wrap(msg.getBytes()));
            return;
        }

        // Creamos un zip para enviarla
        String zipFolder = folderPath + ".zip";
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFolder))) {
            File folderToZip = new File(folderPath);
            compressFolder(folderToZip, folderToZip.getName(), zipOut);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Obtenemos el zip
        File zip = new File(zipFolder);

        // Enviar tamaño del archivo
        long zipSize = zip.length();
        ByteBuffer sizeBuffer = ByteBuffer.allocate(Long.BYTES);
        sizeBuffer.putLong(zipSize);
        sizeBuffer.flip();
        socketChannel.write(sizeBuffer);

        // Tratar de enviar el archivo en bloques
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
        System.out.println("Carpeta enviado al cliente");
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

    private static void createFile(String fileName, SocketChannel socketChannel) throws IOException {
        // Stringbuilder para respuesta
        StringBuilder sb = new StringBuilder();
        sb.setLength(0);

        // Obtenemos la ruta actual
        String path = setDirectory() + "/" + fileName;
        File file = new File(path);

        sb.append("\nSERVIDOR: ");

        try {
            if (file.createNewFile()) {
                System.out.println("El archivo fue creado exitosamente");
                sb.append("El archivo fue creado exitosamente");
            } else {
                System.out.println("El archivo ya existe");
                sb.append("El archivo ya existe");
            }
        } catch (IOException e) {
            System.out.println("Error al crear el archivo");
            sb.append("Error al crear el archivo");
            e.printStackTrace();
        }

        // Enviamos la respuesta
        ByteBuffer bb = ByteBuffer.wrap(sb.toString().getBytes());
        socketChannel.write(bb);
    }

    private static void createFolder(String folderName, SocketChannel socketChannel) throws IOException {
        // Stringbuilder para respuesta
        StringBuilder sb = new StringBuilder();
        sb.setLength(0);

        // Obtenemos la ruta actual
        String path = setDirectory() + "/" + folderName;
        File folder = new File(path);

        sb.append("\nSERVIDOR: ");

        if(folder.mkdir()){
            System.out.println("Carpeta creada exitosamente");
            sb.append("Carpeta creada exitosamente");
        } else {
            System.out.println("La carpeta ya existe");
            sb.append("La carpeta ya existe");
        }

        // Enviamos la respuesta
        ByteBuffer bb = ByteBuffer.wrap(sb.toString().getBytes());
        socketChannel.write(bb);
    }

    private static void rename(String name, String newName, SocketChannel socketChannel) throws IOException {
        // Stringbuilder para respuesta
        StringBuilder sb = new StringBuilder();
        sb.setLength(0);

        // Obtenemos la ruta actual
        String actualDirectory = setDirectory();
        String directory = actualDirectory + "/" + name;
        String directory2 = actualDirectory + "/" + newName;

        File originalPath = new File(directory);
        File newPath = new File(directory2);

        sb.append("\nSERVIDOR: ");

        if(originalPath.renameTo(newPath)){
            System.out.println("Archivo o directorio renombrado correctamente");
            sb.append("Archivo o directorio renombrado correctamente");
        } else {
            System.out.println("Error al renombrar el archivo o directorio");
            sb.append("Error al renombrar el archivo o directorio");
        }

        // Enviamos la respuesta
        ByteBuffer bb = ByteBuffer.wrap(sb.toString().getBytes());
        socketChannel.write(bb);
    }

    private static void deleteFile(String fileName, SocketChannel socketChannel) throws IOException{
        // Stringbuilder para respuesta
        StringBuilder sb = new StringBuilder();
        sb.setLength(0);
        
        // Obtenemos la ruta actual
        String path = setDirectory() + "/" + fileName;
        File file = new File(path);

        sb.append("\nSERVIDOR: ");

        if(file.delete()){
            System.out.println("Archivo eliminado correctamente");
            sb.append("Archivo eliminado correctamente");
        } else {
            System.out.println("No se pudo eliminar el archivo");
            sb.append("No se pudo eliminar el archivo");
        }

        // Enviamos la respuesta
        ByteBuffer bb = ByteBuffer.wrap(sb.toString().getBytes());
        socketChannel.write(bb);
    }

    private static void deleteFolder(String folderName, SocketChannel socketChannel) throws IOException {
        // Stringbuilder para respuesta
        StringBuilder sb = new StringBuilder();
        sb.setLength(0);
        
        // Obtenemos la ruta actual
        String path = setDirectory() + "/" + folderName;
        File folder = new File(path);

        sb.append("\nSERVIDOR: ");

        if(deleteRecursive(folder)){
            System.out.println("Carpeta eliminada correctamente");
            sb.append("Carpeta eliminada correctamente");
        } else {
            System.out.println("No se puso eliminar la carpeta");
            sb.append("No se puso eliminar la carpeta");
        }

        // Enviamos la respuesta
        ByteBuffer bb = ByteBuffer.wrap(sb.toString().getBytes());
        socketChannel.write(bb);
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
