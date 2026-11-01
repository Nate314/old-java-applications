package web.rt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.teavm.jso.JSObject;

/**
 * Copies the files boot.js fetched before main() (listed in the page's data-files) into TeaVM's in-memory file
 * system, so the apps' java.io code finds them by the same relative paths it uses on the desktop. Then {@link Saves}
 * writes back what the app saved on earlier visits.
 */
public final class Files {
    private static boolean installed;

    private Files() {
    }

    public static void install() {
        if (installed) {
            return;
        }
        installed = true;
        for (String name : Js.preloadedNames()) {
            JSObject bytes = Js.preloadedBytes(name);
            byte[] data = new byte[Js.length(bytes)];
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) Js.byteAt(bytes, i);
            }
            File file = new File(name);
            File dir = file.getAbsoluteFile().getParentFile();
            if (dir != null) {
                dir.mkdirs();
            }
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(data);
            } catch (IOException e) {
                Js.consoleError("Could not preload " + name + ": " + e);
            }
        }
        Saves.install();
    }
}
