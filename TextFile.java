import java.io.File;
import java.io.Serializable;
import java.net.URI;

public class TextFile extends File implements Serializable {

    private String filename;
    private String owner;

    public TextFile(String pathname) {
        super(pathname);
    }

    public TextFile(URI uri) {
        super(uri);
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
