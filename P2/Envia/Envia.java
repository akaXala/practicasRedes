package Envia;

import java.net.*;
import java.io.*;
import java.util.Random;
import java.util.Scanner;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Envia {
    // Puertos de los sockets
    private static int portS = 7777;
    private static int portR = 7778;

    // Configuraciones de envio
    private static int packetSize = 4096*2;
    private static int windowSize = 10;
    private static final int TIMEOUT = 1000; // Timeout en milisegundos para cada paquete

    // Variables para simular errores en el emisor
    private static boolean PACKETNOTSENT = true;   // CASO 1: El paquete no se envia en el primer intento
    private static boolean PACKETDUPLICATE = true; // CASO 2: El paquete se envia dos veces en el primer intento
    public static void main(String[] args) {
        // Scanner para entrada de datos
        Scanner sc = new Scanner(System.in);

        while(true){
            // Selección del archivo
            System.out.println("Seleccione un archivo, <EXIT> para terminar la ejecución");
            listLocalFiles();
            System.out.print("Archivo seleccionado: ");
            String name = sc.nextLine();

            if(name.equals("EXIT")){
                break;
            } else {
                // Enviamos el archivo
                sendFile(name);
            }
        }

        // Cerramos el Scanner
        sc.close();
    }

    private static void listLocalFiles(){
        // Mostramos los archivos del cliente
        System.out.println("--- Mostrando archivos ---");
        File directorio = new File("./Envia/Archivos");
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

    private static void sendFile(String name){
        // Ubicación del archivo
        String filePath = "./Envia/Archivos/" + name;
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
            // Leemos el archivo completo en memoria
            FileInputStream fis = new FileInputStream(file);
            byte[] fileData = new byte[(int) file.length()];
            fis.read(fileData);
            fis.close();

            // Calculamos el total de paquetes a enviar
            int totalPackets = (int) Math.ceil((double) fileData.length / packetSize);
            System.out.println("Total de paquetes a enviar: " + totalPackets);

            // Estructuras para llevar el control:             
            boolean[] acked = new boolean[totalPackets];    // "acked" indica si cada paquete fue confirmado
            long[] sendTime = new long[totalPackets];       // "sendTime" almacena la última vez que se envió cada paquete

            // Variables para simular errores
            boolean[] dropSimulated = new boolean[totalPackets];    // Registro de paquetes que ya "fallaron" en el primer intento
            boolean[] duplicateSimulated = new boolean[totalPackets];   // Registro de paquetes que se "duplicaron" en el primer intento

            // Inicializamos la ventana
            int base = 0;  // Primer paquete de la ventana

            // Configuración de envio
            DatagramSocket socketS = new DatagramSocket(portR); // Socket de datagrama
            InetAddress address = InetAddress.getByName("127.0.0.1");   // Dirección del socket
            socketS.setSoTimeout(500); // Fijamos un timeout de 500ms = 0.5s

            // Random para ver que paquete no se envia o se envia duplicado
            Random random = new Random();
            
            // Bucle principal, para enviar paquetes que aún no son confirmados
            while (base < totalPackets) {
                // Envio de paquetes por ventana deslizante
                for (int i = base; i < Math.min(base + windowSize, totalPackets); i++) {
                    // Variable para ver el tiempo
                    LocalDateTime time = LocalDateTime.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSS");  // Formato al tiempo

                    // Enviamos los paquetes que no tienen ACK o su envio supero el timeout
                    if (!acked[i] && (sendTime[i] == 0 || (System.currentTimeMillis() - sendTime[i]) > TIMEOUT)) {

                        // --- CASO 1: NO SE ENVIA EL PAQUETE EN EL PRIMER INTENTO ---
                        if (PACKETNOTSENT && !dropSimulated[i]) {
                            // 20% de probabilidad de error
                            if (random.nextInt(10) < 2) {
                                System.out.println("[" + time.format(formatter) + "] Simulación Error: No se envía el paquete " + i + " en este intento.");
                                dropSimulated[i] = true;
                                // Omitimos el envio para este ciclo, cuando llege que NACK se reintentara
                                continue;
                            }
                        }

                        // Calculamos el inicio y el tamaño
                        int start = i * packetSize;
                        int length = Math.min(packetSize, fileData.length - start);

                        // Flujos de escritura para el paquete
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        DataOutputStream dos = new DataOutputStream(baos);

                        // Metadatos
                        dos.writeInt(i);                // Número de secuencia
                        dos.writeInt(length);           // Total del fragmento actual

                        // Datos que solo necesitamos enviar una vez en el primer paquete
                        if (i == 0){
                            dos.writeUTF(fileName);         // Nombre del archivo
                            dos.writeUTF(fileType);         // Tipo del archivo
                            dos.writeInt(totalPackets);     // Total de paquetes
                        }

                        // Escribimos los datos del fragmento
                        dos.write(fileData, start, length);
                        dos.flush();    // Enviamos inmediatamente

                        // Preparamos el paquete
                        byte[] sendData = baos.toByteArray();
                        DatagramPacket packet = new DatagramPacket(sendData, sendData.length, address, portS);

                        // Enviamos el paquete
                        socketS.send(packet);
                        sendTime[i] = System.currentTimeMillis();
                        System.out.println("[" + time.format(formatter) + "] Enviando paquete " + i + " de " + totalPackets);

                        // CASO 2: EL PAQUETE SE ENVIA DOS VECES
                        if (PACKETDUPLICATE && !duplicateSimulated[i]) {
                            // 20% de probabilidad de error
                            if (random.nextInt(10) > 2) {
                                socketS.send(packet);
                                System.out.println("[" + time.format(formatter) + "] Simulación Error: Envío duplicado para paquete " + i);
                                duplicateSimulated[i] = true;
                            }
                        }

                        // Cerramos los flujos de salida
                        dos.close();
                        baos.close();
                    }
                }

                // Bloque para recibir el ACK
                try {
                    // Variable para ver el tiempo
                    LocalDateTime time = LocalDateTime.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSS");  // Formato al tiempo

                    // Preparamos el paquete de respuesta
                    byte[] receiveData = new byte[1024];
                    DatagramPacket responsePacket = new DatagramPacket(receiveData, receiveData.length);

                    // Bloqueamos hasta que se reciba el paquete o el timeout
                    socketS.receive(responsePacket);
                    
                    // Leemos la respuesta
                    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(responsePacket.getData()));
                    int ackNum = dis.readInt();  // Número de paquete
                    boolean ackReceived = dis.readBoolean(); // True = Se recibio el paquete / False = El paquete no llego

                    // Si se confirmo el paquete, se marca en el arreglo de ACKs
                    if (ackReceived && ackNum >= base && ackNum < totalPackets) {
                        acked[ackNum] = true;
                        System.out.println("[" + time.format(formatter) + "] ACK recibido para paquete " + ackNum);

                        // Deslizamos la ventana
                        while (base < totalPackets && acked[base]) {
                            base++;
                        }
                    }
                    dis.close();
                } catch (Exception e) {
                    // Si se agota el timeout, no se recibió ACK y se reintentará en la siguiente iteración
                    // Los paquetes que no hayan sido confirmados y cuyo tiempo de envío exceda TIMEOUT se reenvían
                }
            }

            // Variable para ver el tiempo
            LocalDateTime time = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSS");  // Formato al tiempo
            
            // Cerramos el socket
            socketS.close();
            System.out.println("[" + time.format(formatter) + "] Archivo enviado exitosamente.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
