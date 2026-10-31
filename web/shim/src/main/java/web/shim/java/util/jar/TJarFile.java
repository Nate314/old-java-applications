package web.shim.java.util.jar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Just enough of JarFile for NathaNOS to read a jar's Main-Class. Replaces TeaVM's JarFile, whose random-access
 * reader fails with EOFException on the in-memory file system; this one streams the jar instead.
 */
public class TJarFile {
    private final File file;

    public TJarFile(String name) throws IOException {
        this(new File(name));
    }

    public TJarFile(File file) throws IOException {
        if (!file.isFile()) {
            throw new FileNotFoundException(file.getPath() + " (The system cannot find the file specified)");
        }
        this.file = file;
    }

    public TManifest getManifest() throws IOException {
        try (ZipInputStream in = new ZipInputStream(new FileInputStream(file))) {
            for (ZipEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
                if (entry.getName().equalsIgnoreCase("META-INF/MANIFEST.MF")) {
                    return new TManifest(in);
                }
            }
        }
        return null;
    }

    public String getName() {
        return file.getPath();
    }

    public void close() {
    }
}
