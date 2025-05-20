// Libreria de google para leer un JSON
import com.google.gson.*;

// Incluimos la carpeta para las clases de recepción de archivos
import FilesChat.FileReceiver;

// Inculimos la carpeta para las clases de JSON
import JSONChat.Config;
import JSONChat.Sala;

// Librerias de Java
import java.util.Scanner;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class Chat {
    // Variables para el programa
    private static String userName = "";
    private static String address = "229.1.2.3";
    private static int port = 0;

    // Variables de control global
    private static boolean run = true;

    // Variables para el envio de archivo
    private static int packetSize = 8192;

    public static void main(String[] args) {
        // Scanner para la entrada de datos del usuario
        Scanner sc = new Scanner(System.in);

        System.out.println("Hola, bienvenido a las salas de chat.");
        System.out.print("Ingrese su nombre de usuario: ");
        userName = sc.nextLine();
        System.out.println("\nBienvenido " + userName);
        
        while (true) {
            System.out.println("\nSeleccione una opción:");
            System.out.println("1.\tCrear una sala de chat.");
            System.out.println("2.\tListar salas de chat.");
            System.out.println("3.\tIngresar el sala de chat.");
            System.out.println("4.\tSalir.\n");
            System.out.print("Opción selecionada: ");
            int opt = sc.nextInt();

            if (opt == 1) {
                createServer();
            } else if (opt == 2) {
                listJSON();
            } else if (opt == 3) {
                run = true;
                joinChat();
            } else if (opt == 4) {
                System.out.println("Saliendo...");
                break;
            } else {
                System.out.println("Error, vuelva a intentarlo.");
            }
        }

        // Cerramos el scanner
        sc.close();
    }

    private static void createServer() {
        // Gson para leer el JSON
        String filePath = "./salas.json";
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        Scanner sc = new Scanner(System.in);

        System.out.print("\nNombre de la sala de chat: ");
        String nameChat = sc.nextLine();

        try {
            // Leer el archivo JSON
            FileReader reader = new FileReader(filePath);
            Config config = gson.fromJson(reader, Config.class);
            reader.close();

            List<Integer> puertosSalas = new ArrayList<>();

            for (Sala sala : config.getSalas()) {
                puertosSalas.add(sala.getPuertoSala());
            }

            int min = 7002;
            int max = 7999;

            int randomPort = (int) (Math.random() * (max - min + 1)) + min;

            // Verificamos que el puerto creado no exista
            while (puertosSalas.contains(randomPort)) {
                randomPort = (int) (Math.random() * (max - min + 1)) + min;
            }

            // Agregamos una nueva sala
            config.getSalas().add(new Sala(nameChat, userName, randomPort));
            
            // Escribir el archivo JSON actualizado
            FileWriter writer = new FileWriter(filePath);
            gson.toJson(config, writer);
            writer.close();

            // Iniciamos el server en otra ventana del CMD
            launchCMD();

            // Esperamos 2s a que inicie el servidor
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }

            // Mandamos información para su creación
            Socket socket = new Socket("localhost", 9999);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.println(randomPort);
            out.println(userName);

            out.close();
            socket.close();

            // Damos el nombre de la sala y el puerto asignado
            System.out.println("Sala " + nameChat + " creada en el puerto " + randomPort);

        } catch (IOException e) {
            e.printStackTrace();
        }

        // sc.close();
        System.out.println("");
    }

    private static void modifyJSON() {
        // Gson para leer el JSON
        String filePath = "./salas.json";
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try {
            // Leer el archivo JSON
            FileReader reader = new FileReader(filePath);
            Config config = gson.fromJson(reader, Config.class);
            reader.close();

            config.getSalas().removeIf(sala -> sala.getPuertoSala() == port);

            // Guardar el archivo JSON actualizado
            FileWriter writer = new FileWriter(filePath);
            gson.toJson(config, writer);
            writer.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    
    private static void listJSON() {
        // Gson para leer el JSON
        String filePath = "./salas.json";
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try {
            // Leer el archivo JSON
            FileReader reader = new FileReader(filePath);
            Config config = gson.fromJson(reader, Config.class);
            reader.close();

            // Mostrar salas actuales
            System.out.println("\nSalas actuales:");
            for (Sala sala : config.getSalas()) {
                System.out.println(" - " + sala.getNombreSala() + "\t(Puerto: " + sala.getPuertoSala() + ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void launchCMD() {
        // Comando original que quieres ejecutar en la nueva ventana CMD
        String javaExecutable = "C:\\Program Files\\Java\\jdk-24\\bin\\java.exe";   // Teoricamente todos debemos de tener las mismas, si no cambiala
        String enablePreview = "--enable-preview";
        String showCodeDetails = "-XX:+ShowCodeDetailsInExceptionMessages";
        String cpOption = "-cp";
        // Reemplazar estas con las rutas de su ordenador
        String classPath = "C:\\Users\\titol\\AppData\\Roaming\\Code\\User\\workspaceStorage\\bc899e5fab5123ebbe58d0a89366b537\\redhat.java\\jdt_ws\\Practica3_9ef00555\\bin";
        String mainClass = "Servidor";

        // Construir la lista de comandos para ProcessBuilder
        List<String> commandToExecute = new ArrayList<>();
        commandToExecute.add("cmd.exe");         // El programa que ejecutará el comando 'start'
        commandToExecute.add("/c");              // Opción para que cmd.exe ejecute el comando y luego termine
        commandToExecute.add("start");           // El comando de CMD para abrir una nueva ventana
        commandToExecute.add("\"Servidor chat\""); // Título para la nueva ventana CMD (las comillas son importantes si el título tiene espacios)

        // Ahora agregamos el comando original y sus argumentos.
        // 'start' los tomará como el comando a ejecutar en la nueva ventana.
        commandToExecute.add(javaExecutable);
        commandToExecute.add(enablePreview);
        commandToExecute.add(showCodeDetails);
        commandToExecute.add(cpOption);
        commandToExecute.add(classPath); // ProcessBuilder maneja correctamente los espacios en los argumentos individuales
        commandToExecute.add(mainClass);

        ProcessBuilder processBuilder = new ProcessBuilder(commandToExecute);

        try {
            processBuilder.start(); // Inicia cmd.exe, que a su vez ejecuta 'start'
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void joinChat() {
        // Scanner para registrar el puerto
        Scanner sc = new Scanner(System.in);

        System.out.print("Escriba el puerto de la sala: ");
        port = sc.nextInt();

        try {
            MulticastSocket s = new MulticastSocket(port);
            s.setReuseAddress(true);
            s.setTimeToLive(255);

            InetAddress server = InetAddress.getByName(address);
            s.joinGroup(server);

            System.out.println("\nEscribe /exit para salir y /help para obtener ayuda");

            // Flujos para escribir en el paquete
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            // Escribimos los datos en el paquete
            dos.writeUTF("Join");
            dos.writeUTF(userName);
            dos.flush();

            // Enviamos el paquete
            byte[] b = baos.toByteArray();
            DatagramPacket p = new DatagramPacket(b, b.length, server, port);
            s.send(p);

            // Cerramos los flujos de salida
            dos.close();
            baos.close();

            // Llamamos a la función para enviar y recibir mensajes
            messages(s);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void messages(MulticastSocket socket) {
        // Scanner para escribir
        Scanner sc = new Scanner(System.in);

        // Hilo para recibir mensajes
        Thread receiveMsgThread = new Thread(() -> {
            // Tamaño del buffer
            byte[] buffer = new byte[65535];

            while (run) {
                try {
                    // Recibimos el paquete
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    if (!run) break;    // En caso de que se salga de la sala de chat

                    // Abrimos el flujo de lectura
                    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(packet.getData()));

                    // Variables que siempre se van a leer
                    String typeMsg = dis.readUTF();
                    dis.close();
                    
                    if (typeMsg.equals("JoinAlert")) {
                        joinAlert(packet);
                    } else if (typeMsg.equals("Message") || typeMsg.equals("PrivateMessage")) {
                        receiveMessage(packet);

                    } else if (typeMsg.equals("File") || typeMsg.equals("PrivateFile")) {
                        receiveFile(packet);

                    } else if (typeMsg.equals("UserList")) {
                        receiveListUsers(packet);

                    } else if (typeMsg.equals("ExitAlert")) {
                        exitAlert(packet);

                    } else if (typeMsg.equals("NotAllowed")) {
                        notAllowed(packet);

                    } else if (typeMsg.equals("FinishChat")) {
                        run = false;
                        modifyJSON();
                        System.out.println("El administrador ha cerrado la sala");

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                
            }
        });
        receiveMsgThread.start();

        // Hilo para enviar mensajes
        try {
            while (true) {
                String message = sc.nextLine();

                if (message.startsWith("/")) {
                    String line[] = message.split(" ", 2);
                    String command = line[0];
                    String arguments = (line.length > 1) ? line[1] : "";

                    if (command.equals("/msg")) {
                        // Separamos en usuario y mensaje
                        String[] line2 = arguments.split(" ", 2);
                        String user = line2[0];
                        String messageP = (line2.length > 1) ? line2[1] : "";

                        DatagramPacket packet = sendMessage(messageP, user);
                        socket.send(packet);
                    } else if (command.equals("/msgf")) {
                        // Separamos en usuario y archivo
                        String[] line2 = arguments.split(" ", 2);
                        String user = line2[0];
                        String fileP = (line2.length > 1) ? line2[1] : "";
                        sendFile(fileP, user, socket);

                    } else if (command.equals("/file")) {
                        sendFile(arguments, null, socket);

                    } else if (command.equals("/users")) {
                        DatagramPacket packet = listUsers();
                        socket.send(packet);

                    } else if (command.equals("/help")) {
                        System.out.println("/file 'route'\t\t\tPara enviar un archivo");
                        System.out.println("/msg 'username'\t\t\tPara enviar un mensaje privado");
                        System.out.println("/msgf 'username' 'route'\tPara enviar un archivo privado");
                        System.out.println("/users\t\t\tPara ver los usuarios conectados");
                        System.out.println("/exit\t\t\t\tPara salir");
                        System.out.println("/quit\t\t\t\tPara cerrar la sala de chat (Solo admin)");

                    } else if (command.equals("/exit")) {
                        DatagramPacket packet = exitChat();
                        socket.send(packet);
                        run = false;
                        break;

                    } else if (command.equals("/quit")) {
                        DatagramPacket packet = endChat();
                        socket.send(packet);

                        Thread.sleep(1000);

                        if (run == false) {
                            break;
                        }
                    }
                } else {
                    DatagramPacket packet = sendMessage(message, null);
                    socket.send(packet);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static DatagramPacket sendMessage(String message, String receiver) throws IOException {
        // Dirrecion
        InetAddress server = InetAddress.getByName(address);

        // Escribimos el paquete
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // Metadatos
        if (receiver == null) {
            dos.writeUTF("Message");
        } else {
            dos.writeUTF("PrivateMessage");
        }
        dos.writeUTF(userName);         // Quien envia el mensaje
        if (!(receiver == null)) dos.writeUTF(receiver);  // Quien recibe

        // Contenido del mensaje
        dos.writeUTF(message);
        dos.flush();

        byte[] b = baos.toByteArray();
        DatagramPacket p = new DatagramPacket(b, b.length, server, port);

        return p;
    }

    private static void receiveMessage(DatagramPacket packet) throws IOException{
        // Abrimos el flujo de lectura
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(packet.getData()));

        // Variables que siempre se van a leer
        String typeMessage = dis.readUTF();                  // Descartamos algo que ya se leyo
        String senderNickname = dis.readUTF();    // Leemos de quien es el mensaje
        String receiverNickname = (typeMessage.equals("PrivateMessage")) ? dis.readUTF() : null;

        // Leemos el mensaje
        String message = dis.readUTF();

        // Cerramos el flujo de lectura
        dis.close();

        

        if (!userName.equals(senderNickname)) {
            if (typeMessage.equals("PrivateMessage")) {
                if (userName.equals(receiverNickname)) {
                    // Imprimimos el mensaje privado
                    System.out.println("Mensaje privado de " + senderNickname + ": " + message);
                }
            } else {
                // Imprimimos el mensaje
                System.out.println(senderNickname + ": " + message);
            }
        }  
    }

        private static void sendFile(String name, String receiver, MulticastSocket socket) {
        Thread sendFileThread = new Thread(() -> {
            // Ubicación del archivo
            String filePath = "./carga/" + name;
            File file = new File(filePath);

            // Verificamos que el archivo exista
            if(!file.exists()){
                System.out.println("\nEl archivo no existe.");
                return;
            }

            // Obtenemos el nombre y tipo de archivo
            String[] parts = name.split("\\.", 2);
            String fileName = parts[0];
            String fileType = parts[1];

            try {
                // Configuración de IP para el envio
                InetAddress server = InetAddress.getByName(address);

                // Leemos el archivo completo en memoria
                FileInputStream fis = new FileInputStream(file);
                byte[] fileData = new byte[(int) file.length()];
                fis.read(fileData);
                fis.close();

                // Calculamos el total de paquetes
                int totalPackets = (int) Math.ceil((double) fileData.length / packetSize);

                // Tipo de archivo
                String typeFile;

                if (receiver == null) {
                    typeFile = "File";
                } else {
                    typeFile =  "PrivateFile";
                }

                // Enviamos los paquetes del archivo
                for (int i = 0; i < totalPackets; i++) {
                    // Calculamos el inicio y el tamaño
                    int start = i * packetSize;
                    int length = Math.min(packetSize, fileData.length - start);

                    // Flujos para escribir el paquete
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);

                    // Metadatos
                    dos.writeUTF(typeFile);   // Tipo del mensaje
                    dos.writeInt(i);            // Número de secuencia
                    dos.writeInt(length);       // Total del fragmento actual

                    // Datos que solo necesitamos enviar una vez en el primer paquete
                    if (i == 0) {
                        dos.writeUTF(userName); // Quien envia el archivo
                        if (typeFile.equals("PrivateFile")) dos.writeUTF(receiver); // Para quien es el archivo si es privado
                        dos.writeUTF(fileName); // Nombre del archivo
                        dos.writeUTF(fileType); // Tipo del archivo
                        dos.writeInt(totalPackets); // Total de paquets
                    }

                    // Escribrimos los datos del fragmento
                    dos.write(fileData, start, length);
                    dos.flush();

                    // Preparamos el paquete
                    byte[] sendData = baos.toByteArray();
                    DatagramPacket packet = new DatagramPacket(sendData, sendData.length, server, port);

                    // Enviamos el paquete
                    socket.send(packet);
                    
                    // Cerramos los flujos de salida
                    dos.close();
                    baos.close();
                }                
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        sendFileThread.start();
    }

    private static void receiveFile(DatagramPacket packet) throws IOException {
        FileReceiver file = new FileReceiver();
        file.setUserName(userName);
        file.receiveFile(packet);
    }

    private static DatagramPacket listUsers() throws IOException {
        // Dirrecion
        InetAddress server = InetAddress.getByName(address);

        // Escribimos el paquete
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // Metadatos
        dos.writeUTF("Users");  // Tipo de mensaje
        dos.writeUTF(userName);     // Remitente

        dos.flush();

        byte[] b = baos.toByteArray();
        DatagramPacket p = new DatagramPacket(b, b.length, server, port);

        return p;
    }

    private static void receiveListUsers(DatagramPacket packet) throws IOException {
        // Abrimos el flujo de lectura
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(packet.getData()));

        // Variables que siempre se van a leer
        dis.readUTF();                              // Descartamos algo que ya se leyo
        String receiverNickname = dis.readUTF();    // Leemos para quien en el mensaje
        int userSize = dis.readInt();               // Leemos cuantos usuarios 
        dis.close();

        if (userName.equals(receiverNickname)) {
            System.out.println("\nUsuarios: ");
        }

        for (int i = 0; i < userSize; i++) {
            String user = dis.readUTF();  // Leemos el usuario

            if (userName.equals(receiverNickname)) {
                System.out.println("- " + user);
            }
        }
    }

    private static void joinAlert(DatagramPacket packet) throws IOException {
        // Abrimos el flujo de lectura
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(packet.getData()));

        // Variables que siempre se van a leer
        dis.readUTF();                              // Descartamos algo que ya se leyo
        String joinNickname = dis.readUTF();    // Leemos quien se unio
        dis.close();

        // Imprimimos el texto
        if (!joinNickname.equals(userName)) {
            System.out.println(joinNickname + " se ha unido");
        }
    }

    private static DatagramPacket exitChat() throws IOException {
        // Dirrecion
        InetAddress server = InetAddress.getByName(address);

        // Escribimos el paquete
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // Metadatos
        dos.writeUTF("ExitAlert");  // Tipo de mensaje
        dos.writeUTF(userName);     // Remitente
        
        dos.flush();

        byte[] b = baos.toByteArray();
        DatagramPacket p = new DatagramPacket(b, b.length, server, port);

        return p;
    }

    private static void exitAlert(DatagramPacket packet) throws IOException {
        // Abrimos el flujo de lectura
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(packet.getData()));

        // Variables que siempre se van a leer
        dis.readUTF();                              // Descartamos algo que ya se leyo
        String joinNickname = dis.readUTF();        // Leemos quien se salio
        dis.close();

        // Imprimimos el texto
        if (!joinNickname.equals(userName)) {
            System.out.println(joinNickname + " ha salido");
        }
    }

    private static DatagramPacket endChat() throws IOException {
        // Dirrecion
        InetAddress server = InetAddress.getByName(address);

        // Escribimos el paquete
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // Metadatos
        dos.writeUTF("EndChat");  // Tipo de mensaje
        dos.writeUTF(userName);     // Remitente

        dos.flush();

        byte[] b = baos.toByteArray();
        DatagramPacket p = new DatagramPacket(b, b.length, server, port);

        return p;
    }

    private static void notAllowed(DatagramPacket packet) throws IOException {
        // Abrimos el flujo de lectura
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(packet.getData()));

        // Variables que siempre se van a leer
        dis.readUTF();                              // Descartamos algo que ya se leyo
        String nickname = dis.readUTF();        // Leemos para quien es
        dis.close();

        // Imprimimos el texto
        if (nickname.equals(userName)) {
            System.out.println("No eres el administrador, no puedes cerrar la sala de chat");
        }
    }
}
