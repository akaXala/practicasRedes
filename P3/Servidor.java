import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Servidor {
    // Variables de control global
    private static String address = "229.1.2.3";
    private static int temporalPort = 9999;
    private static int portMultiaddressSocket;
    private static String userAdmin;
    private static ArrayList<String> users;
    public static void main(String[] args) {
        users = new ArrayList<>();

        // Inicializamos un socket para recibir el puerto y el admin
        try (ServerSocket serverSocket = new ServerSocket(temporalPort)) {
            Socket socket = serverSocket.accept();

            // Flujo de entrada
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Leemos las variables
            portMultiaddressSocket = Integer.parseInt(br.readLine());
            userAdmin = br.readLine();

            // Cerramos el flujo de entrada y el socket
            br.close();
            socket.close();
            serverSocket.close();
            
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            MulticastSocket serverChat = new MulticastSocket(portMultiaddressSocket);
            serverChat.setReuseAddress(true);
            serverChat.setTimeToLive(255);
            InetAddress gpo = InetAddress.getByName(address);

            serverChat.joinGroup(gpo);

            System.out.println("Servidor creado en el puerto " + portMultiaddressSocket + " por " + userAdmin);
            System.out.println("\nRegistro: ");

            // Tamaño del buffer
            byte[] buffer = new byte[65535];

            while (true) {
                // Recibimos el paquete
                DatagramPacket p = new DatagramPacket(buffer, buffer.length);
                serverChat.receive(p);

                // Abrimos flujo de lectura para el paquete
                DataInputStream dis = new DataInputStream(new ByteArrayInputStream(p.getData()));

                // Leemos el paquete
                String typePacket = dis.readUTF();  // Tipo del paquete
                String userName = "";

                if (!typePacket.equals("PrivateFile") || !typePacket.equals("File")) {
                    userName = dis.readUTF();    // Quien envia el paquete
                    dis.close();
                }

                if (typePacket.equals("Join")) {
                    users.add(userName);
                    System.out.println(userName + " se ha unido");

                    // Escribimos un paquete para informar que usuario se ha unido
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);

                    // Metadatos
                    dos.writeUTF("JoinAlert");   // Tipo del 
                    
                    // Mensaje
                    dos.writeUTF(userName);         // Para quien es
                    dos.flush();

                    // Creamos y enviamos el paquete
                    byte[] b = baos.toByteArray();
                    DatagramPacket packet = new DatagramPacket(b, b.length, gpo, portMultiaddressSocket);
                    serverChat.send(packet);

                    // Cerramos los flujos de salida
                    dos.close();
                    baos.close();

                } else if (typePacket.equals("Message")) {
                    String menssage = dis.readUTF();    // Contenido del mensaje
                    System.out.println(userName + ": " + menssage);

                } else if (typePacket.equals("Users")) {
                    // Escribimos un paquete con todos los usuarios
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);

                    // Metadatos
                    dos.writeUTF("UserList");   // Tipo del mensaje
                    dos.writeUTF(userName);         // Para quien es
                    dos.writeInt(users.size());     // Cuantos usuarios

                    // Enlistamos todos los usuarios
                    for (int i = 0; i < users.size(); i++) {
                        dos.writeUTF(users.get(i)); // Enlistamos el usuario
                    }

                    dos.flush();    // Terminamos de escribir

                    // Creamos y enviamos el paquete
                    byte[] b = baos.toByteArray();
                    DatagramPacket packet = new DatagramPacket(b, b.length, gpo, portMultiaddressSocket);
                    serverChat.send(packet);

                    // Cerramos los flujos de salida
                    dos.close();
                    baos.close();

                } else if (typePacket.equals("ExitAlert")) {
                    users.remove(userName);
                    System.out.println(userName + " ha salido");
                    
                } else if (typePacket.equals("EndChat")) {
                    // Escribimos un paquete para informar que no puede realizar la operacion
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);

                    if (userAdmin.equals(userName)) {
                        dos.writeUTF("FinishChat");
                    } else {
                        // Metadatos
                        dos.writeUTF("NotAllowed");    // Tipo del mensaje
                        dos.writeUTF(userName);             // Para quien es
                    }

                    dos.flush();

                    // Creamos y enviamos el paquete
                    byte[] b = baos.toByteArray();
                    DatagramPacket packet = new DatagramPacket(b, b.length, gpo, portMultiaddressSocket);
                    serverChat.send(packet);

                    // Cerramos los flujos de salida
                    dos.close();
                    baos.close();

                    if (userAdmin.equals(userName)) break;
                }

                dis.close();
            }
            serverChat.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
