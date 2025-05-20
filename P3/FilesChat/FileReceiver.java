package FilesChat;

// Librerias de java
import java.io.*;
import java.net.DatagramPacket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Incluimos las carpetas de archivos

public class FileReceiver {
    private static String userName = "DefaultUser";
    private static final Map<String, FileReceptionState> activeFileReceptions = new ConcurrentHashMap<>();

    // Metodo para establecer el userName
    public static void setUserName(String name) {
        userName = name;
    }

    public static void receiveFile(DatagramPacket packet) {
        DataInputStream dis = null;
        String fileTransferId = null;

        try {
            dis = new DataInputStream(new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength()));

            // Leemos el identificador unico
            fileTransferId = dis.readUTF();

            // Leemos la metadata
            int seq = dis.readInt();    // Número de secuencia
            int length = dis.readInt(); // Cuanto mide el paquete

            FileReceptionState fileState;

            // Leemos datos de un solo uso
            if (seq == 0) {
                final String currentSenderNickname = dis.readUTF(); // Quien envia
                final String currentReceiverNickname = (fileTransferId.equals("PrivateFile")) ? dis.readUTF() : null;
                final String currentFileName = dis.readUTF();       // Nombre del archivo
                final String currentFileType = dis.readUTF();       // Tipo de archivo
                final int currentTotalPacketsExpected = dis.readInt();  // Total de paquetes
                final String finalFileTransferId = fileTransferId;  // Definimos bien el usuario

                fileState = activeFileReceptions.computeIfAbsent(finalFileTransferId, id -> {
                    return new FileReceptionState(id, currentSenderNickname, currentReceiverNickname, currentFileName, currentFileType, currentTotalPacketsExpected);
                });
            } else {
                fileState = activeFileReceptions.get(fileTransferId);
                if (fileTransferId == null) {
                    return;
                }
            }

            // Leer el segmento de datos del archivo del paquete actual.
            byte[] fileSegment = new byte[length];
            dis.readFully(fileSegment); // Asegura leer 'length' bytes.

            // Añadir el segmento al estado del archivo.
            fileState.addPacket(seq, fileSegment);

            // Verificar si todos los paquetes para este archivo han sido recibidos.
            if (fileState.isComplete()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Map<Integer, byte[]> allPackets = fileState.getPackets(); // Obtiene una copia ordenada.

                for (int i = 0; i < fileState.totalPackets; i++) {
                    byte[] segment = allPackets.get(i);
                    if (segment != null) {
                        baos.write(segment);
                    } else {
                        baos.close();
                        activeFileReceptions.remove(fileTransferId); // Limpiar estado incompleto.
                        return;
                    }
                }

                // Guardar el archivo reconstruido
                if (!fileState.senderNickname.equals(userName)) {
                    // Si es privado, solo el destinatario debe de guardar el archivo
                    if (!"PrivateFile".equals(fileTransferId) || (fileState.receiverNickname != null && fileState.receiverNickname.equals(userName))) {
                        String filePath = "./descargas/" + fileState.fileName + "." + fileState.fileType;
                        FileOutputStream fos = null;

                        try {
                            fos = new FileOutputStream(filePath);
                            baos.writeTo(fos);
                            System.out.println("INFO: Archivo '" + filePath + "' guardado exitosamente.");
                        } catch (IOException e) {
                            System.err.println("ERROR: Error al guardar el archivo '" + filePath + "': " + e.getMessage());
                            e.printStackTrace();
                        } finally {
                            if (fos != null) {
                                fos.close();
                            }
                        }
                    }
                } else {
                    System.out.println("INFO: Archivo '" + fileState.fileName + "' (ID: " + fileTransferId + ") recibido de '" + fileState.senderNickname + "', que es el usuario actual. No se guarda localmente.");
                }

                baos.close();
                // Remover el estado de la transferencia completada.
                activeFileReceptions.remove(fileTransferId);
            }

        } catch (EOFException eofe){
            System.err.println("ERROR: Fin de flujo inesperado al leer el paquete para transferencia ID '" + 
                               (fileTransferId != null ? fileTransferId : "DESCONOCIDO") + 
                               "'. El paquete podría estar malformado o incompleto. " + eofe.getMessage());
        } catch (IOException e) {
            System.err.println("ERROR: Error de E/S al procesar paquete para transferencia ID '" + 
                               (fileTransferId != null ? fileTransferId : "DESCONOCIDO") + 
                               "': " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException e) {
                    // Ignorar error al cerrar DataInputStream.
                }
            }
        }
    }
}
