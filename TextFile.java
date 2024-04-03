import java.io.File;

public class TextFile {
    
    private File file;
    private Client owner;

    public TextFile(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }
    
    public Client getOwner() {
        return owner;
    }

    public void setOwner(Client owner) {
        this.owner = owner;
    }
}