package JSONChat;
public class Sala {
    private String nombreSala;
    private String admin;
    private int puertoSala;

    public Sala(String nombreSala, String admin, int puertoSala) {
        this.nombreSala = nombreSala;
        this.admin = admin;
        this.puertoSala = puertoSala;
    }

    public String getNombreSala() {
        return nombreSala;
    }

    public void setNombreSala(String nombreSala) {
        this.nombreSala = nombreSala;
    }

    public String getAdminSala() {
        return admin;
    }

    public void setAdminSala(String admin) {
        this.admin = admin;
    }

    public int getPuertoSala() {
        return puertoSala;
    }

    public void setPuertoSala(int puertoSala) {
        this.puertoSala = puertoSala;
    }
}