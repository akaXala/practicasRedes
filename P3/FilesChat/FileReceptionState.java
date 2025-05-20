package FilesChat;
import java.util.Map;
import java.util.TreeMap;

public class FileReceptionState {
    String fileId;          // ID de transferencia
    String senderNickname;  // Quien lo esta enviando
    String receiverNickname;    // Para quien es si es privado
    String fileName;        // Nombre del archivo
    String fileType;        // Tipo del archivo
    int totalPackets;       // Cantidad de paquetes
    Map<Integer, byte[]> packets = new TreeMap<>(); // Mapa para ingresar los paquetes

    // Constructor
    public FileReceptionState(String fileId, String senderNickname, String receiverNickname, String fileName, String fileType, int totalPackets) {
        this.fileId = fileId;
        this.senderNickname = senderNickname;
        this.receiverNickname = receiverNickname;
        this.fileName = fileName;
        this.fileType = fileType;
        this.totalPackets = totalPackets;
    }

    // Metodo para agregar paquetes
    public synchronized void addPacket(int seq, byte[] data) {
        packets.put(seq, data);
    }

    // Metodo para comprobar si ya son todos los paquetes
    public synchronized boolean isComplete() {
        if (totalPackets == 0 && packets.isEmpty()) return true;
        return packets.size() == totalPackets && totalPackets > 0;
    }

    public synchronized Map<Integer, byte[]> getPackets () {
        return new TreeMap<>(packets);
    }
}