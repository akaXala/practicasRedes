package Recibe;

import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class Recibe {
    // Puertos de los sockets
    private static int portR = 7777;
    private static int portS = 7778;

    // Para impresion de texto
    private static boolean firstTime = false;   // Falsa por defecto

    // Variables para simular errores en el receptor
    private static boolean ACKNOTSENT = true;  // CASO 3: No enviar el acuse la primera vez para algunos paquetes
        // Variables para registro de secuencias de las simulaciones del error
        private static HashSet<Integer> ackDropped = new HashSet<>();
        private static Random random = new Random();
    public static void main(String[] args) {
        while (true) {
            try {
                // Socket de datagrama
                DatagramSocket socketR = new DatagramSocket(portR);
                InetAddress address = InetAddress.getByName("127.0.0.1");   // Dirección del socket
    
                // Mensajes para iniciar a recibir datagramas
                if (!firstTime) System.out.println("Servidor iniciado.");
                System.out.println("Esperando datagramas...\n");
    
                // Usamos TreeMap para mantener el orden de los paquetes
                Map<Integer, byte[]> packets = new TreeMap<>();
                int totalPackets = -1;
    
                // Variables para guardar el archivo
                String fileName = "";
                String fileType = "";
    
                while (true) {
                    // Variable para ver el tiempo
                    LocalDateTime time = LocalDateTime.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSS");  // Formato al tiempo

                    // Nos preparamos para recibir el paquete
                    byte[] buffer = new byte[65535];  // Esperamos el peor de los casos
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);  // Creamos el paquete
                    socketR.receive(packet);  // Recibe un nuevo paquete en cada iteración
        
                    // Para leer el archivo
                    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(packet.getData()));
                    
                    // Leemos los metadatos del paquete
                    int seq = dis.readInt();    // Número de secuencia
                    int length = dis.readInt(); // Tamaño del fragmento
    
                    // Datos que solo vamos a recibir una vez
                    if (seq == 0){
                        fileName = dis.readUTF();    // Nombre del archivo
                        fileType = dis.readUTF();    // Tipo de archivo
                        totalPackets = dis.readInt();  // Actualizamos el total de paquetes esperados
                    }                
    
                    // Leemos la información del archivo
                    byte[] fileSegment = new byte[length];
                    dis.readFully(fileSegment);
    
                    // Los agregamos al mapa para mantener un orden
                    packets.put(seq, fileSegment);
                    System.out.println("[" + time.format(formatter) + "] Paquete recibido " + seq + " de " + totalPackets);
    
                    // Cerramos la lectura del archivo
                    dis.close();

                    // Abirmos los flujos para responder (ACK)
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);

                    // Escribimos la respuesta (ACK)
                    dos.writeInt(seq);
                    dos.writeBoolean(true);
                    dos.flush();    // Para que se envie inmediatamente

                    //Preparamos el paquete (ACK)
                    byte[] responseData = baos.toByteArray();
                    DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, address, portS);

                    // --- CASO 3: NO SE ENVIA EL ACK PRIMER INTENTO ---
                    if (ACKNOTSENT) {
                        if (!ackDropped.contains(seq)) {
                            // Error del 20%
                            if (random.nextInt(10) < 2){
                                ackDropped.add(seq);
                                System.out.println("[" + time.format(formatter) + "] Simulación Error: Acuse no enviado para paquete " + seq + " en este intento.");
                                dos.close();
                                baos.close();
                                // Se omite el envío del ACK; se espera un reenvío del paquete por parte del emisor.
                                continue;
                            }
                        }
                    }

                    // Enviamos el paquete
                    socketR.send(responsePacket);
                    System.out.println("[" + time.format(formatter) + "] Mensaje de respuesta del paquete " + seq + " enviado");

                    // Cerramos los flujos de salida
                    dos.close();
                    baos.close();
        
                    // Verificamos que ya sean todos los paquetes
                    if (packets.size() == totalPackets){
                        break;
                    }
                }
    
                // Reensamblamos el archivo
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                for(int i = 0; i < totalPackets; i++){
                    baos.write(packets.get(i));
                }
    
                // Guardamos el archivo reconstruido
                String filePath = "./Recibe/Archivos/" + fileName + "." + fileType;
                FileOutputStream fos = new FileOutputStream(filePath);
                baos.writeTo(fos);
    
                // Cerramos los flujos de salida
                fos.close();
                baos.close();

                // Cerramos el socket
                socketR.close();

                // Variable para ver el tiempo
                LocalDateTime time = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSS");  // Formato al tiempo
    
                // Avisamos de que terminamos de guardar el archivo y que estamos listos para recibir más datagramas
                System.out.println("[" + time.format(formatter) + "] Archivo recibido y guardado como " + "'" + fileName + "." + fileType + "'\n");
                firstTime = true;
            } catch (Exception e) {
                e.printStackTrace();
            }            
        }
    }
}
