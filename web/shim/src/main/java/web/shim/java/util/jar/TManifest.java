package web.shim.java.util.jar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Parses the main section of a manifest ("Name: value" lines, continuation lines start with a space). */
public class TManifest {
    private final TAttributes main = new TAttributes();

    public TManifest(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        for (int n = in.read(chunk); n > 0; n = in.read(chunk)) {
            buf.write(chunk, 0, n);
        }
        String key = null;
        StringBuilder value = new StringBuilder();
        for (String line : new String(buf.toByteArray(), StandardCharsets.UTF_8).split("\r\n|\r|\n")) {
            if (line.startsWith(" ") && key != null) {
                value.append(line.substring(1));
                continue;
            }
            if (key != null) {
                main.putValue(key, value.toString());
                key = null;
            }
            if (line.isEmpty()) {
                break; // end of the main section
            }
            int colon = line.indexOf(": ");
            if (colon > 0) {
                key = line.substring(0, colon);
                value.setLength(0);
                value.append(line.substring(colon + 2));
            }
        }
        if (key != null) {
            main.putValue(key, value.toString());
        }
    }

    public TAttributes getMainAttributes() {
        return main;
    }
}
