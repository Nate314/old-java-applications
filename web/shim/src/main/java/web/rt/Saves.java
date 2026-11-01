package web.rt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps the files an app writes across page loads. TeaVM's file system lives in memory, so every second (and when the
 * page is hidden) this walks it, and each file written since the last look is copied into localStorage under
 * {@code oldjava.saves.<App>}. On the next load those files are written back over the page's preloaded ones before
 * main() runs, so FlappyBird's best score or StockChecker's edited list come back as they would on a desktop disk.
 */
public final class Saves {
    private static final String PREFIX = "oldjava.saves.";

    /** Last-modified time of every file seen, so unchanged files are not read again. */
    private static final Map<String, Long> seen = new HashMap<>();
    /** Saved files by absolute path, each byte stored as one char. */
    private static final Map<String, String> saved = new HashMap<>();
    private static String key;

    private Saves() {
    }

    /** Writes the saved files back, then starts watching. Call once, after the preloaded files are in place. */
    public static void install() {
        String app = Js.appId();
        if (app == null || app.isEmpty() || !Js.storageAvailable()) {
            return;
        }
        key = PREFIX + app;
        String[] paths = Js.savedPaths(key);
        for (String path : paths) {
            String data = Js.savedData(key, path);
            if (data != null && write(new File(path), data)) {
                saved.put(path, data);
            }
        }
        scan(false);
        Js.every(1000, () -> scan(true));
        Js.onPageHide(() -> scan(true));
    }

    /** Records every file's time; with {@code store}, also copies new or changed files to localStorage. */
    private static void scan(boolean store) {
        List<File> files = new ArrayList<>();
        collect(new File("/"), files);
        boolean changed = false;
        Map<String, Boolean> present = new HashMap<>();
        for (File file : files) {
            String path = file.getAbsolutePath();
            present.put(path, Boolean.TRUE);
            long modified = file.lastModified();
            Long before = seen.put(path, modified);
            if (store && (before == null || before != modified)) {
                String data = read(file);
                if (data != null && !data.equals(saved.get(path))) {
                    saved.put(path, data);
                    changed = true;
                }
            }
        }
        for (String path : new ArrayList<>(saved.keySet())) {
            if (!present.containsKey(path)) {
                saved.remove(path);
                seen.remove(path);
                changed = true;
            }
        }
        if (changed) {
            String[] paths = saved.keySet().toArray(new String[0]);
            String[] data = new String[paths.length];
            for (int i = 0; i < paths.length; i++) {
                data[i] = saved.get(paths[i]);
            }
            Js.storeSaves(key, paths, data);
        }
    }

    private static void collect(File dir, List<File> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collect(child, out);
            } else {
                out.add(child);
            }
        }
    }

    private static String read(File file) {
        try (InputStream in = new FileInputStream(file)) {
            StringBuilder text = new StringBuilder((int) file.length());
            byte[] buffer = new byte[4096];
            for (int n; (n = in.read(buffer)) > 0; ) {
                for (int i = 0; i < n; i++) {
                    text.append((char) (buffer[i] & 0xff));
                }
            }
            return text.toString();
        } catch (IOException e) {
            return null;
        }
    }

    private static boolean write(File file, String data) {
        File dir = file.getAbsoluteFile().getParentFile();
        if (dir != null) {
            dir.mkdirs();
        }
        byte[] bytes = new byte[data.length()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) data.charAt(i);
        }
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(bytes);
            return true;
        } catch (IOException e) {
            Js.consoleError("Could not restore " + file + ": " + e);
            return false;
        }
    }
}
