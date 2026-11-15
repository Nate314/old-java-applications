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
import java.util.Objects;

/**
 * Keeps the files an app writes across page loads. TeaVM's file system lives in memory, so every second (and when the
 * page is hidden) this walks it, and each file written since the last look is copied into localStorage under
 * {@code oldjava.saves.<App>} ({@code <App>.then} on an app's then.html). On the next load those files are written back over the page's preloaded ones before
 * main() runs, so FlappyBird's best score or StockChecker's edited list come back as they would on a desktop disk.
 * Files written to {@code ~/Downloads} are not kept: each is handed to the browser as a download, then deleted.
 * Other pages of the site share the store (another tab, the home page's player), so only the files this page changed
 * are written into it, and files another page saves are picked up from the storage event.
 */
public final class Saves {
    private static final String PREFIX = "oldjava.saves.";
    /** user.home is "/" here, so this is ~/Downloads. */
    private static final String DOWNLOADS = "/Downloads/";

    /** Last-modified time of every file seen, so unchanged files are not read again. */
    private static final Map<String, Long> seen = new HashMap<>();
    /** Saved files by absolute path, each byte stored as one char. */
    private static final Map<String, String> saved = new HashMap<>();
    private static String key;

    private Saves() {
    }

    /** Writes the saved files back, then starts watching. Call once, after the preloaded files are in place. */
    public static void install() {
        String app = Js.saveStore();
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
        Js.onSavesChanged(key, Saves::pickUp);
        Js.every(1000, () -> scan(true));
        Js.onPageHide(() -> scan(true));
    }

    /** Records every file's time; with {@code store}, also copies new or changed files to localStorage. */
    private static void scan(boolean store) {
        List<File> files = new ArrayList<>();
        collect(new File("/"), files);
        List<String> puts = new ArrayList<>();
        List<String> drops = new ArrayList<>();
        Map<String, Boolean> present = new HashMap<>();
        for (File file : files) {
            String path = file.getAbsolutePath();
            if (path.startsWith(DOWNLOADS)) {
                String data = read(file);
                if (data != null) {
                    Js.download(file.getName(), data);
                }
                file.delete();
                continue;
            }
            present.put(path, Boolean.TRUE);
            long modified = file.lastModified();
            Long before = seen.put(path, modified);
            if (store && (before == null || before != modified)) {
                String data = read(file);
                if (data != null && !data.equals(saved.get(path))) {
                    saved.put(path, data);
                    puts.add(path);
                }
            }
        }
        for (String path : new ArrayList<>(saved.keySet())) {
            if (!present.containsKey(path)) {
                saved.remove(path);
                seen.remove(path);
                drops.add(path);
            }
        }
        if (!puts.isEmpty() || !drops.isEmpty()) {
            String[] data = new String[puts.size()];
            for (int i = 0; i < data.length; i++) {
                data[i] = saved.get(puts.get(i));
            }
            Js.storeSaves(key, puts.toArray(new String[0]), data, drops.toArray(new String[0]));
        }
    }

    /**
     * Another page changed the store: its files are written into this page's file system, unless this page changed
     * the same file since the last look. Files it dropped stay in memory here, but are not written back unless the
     * app writes them again.
     */
    private static void pickUp() {
        Map<String, Boolean> now = new HashMap<>();
        for (String path : Js.savedPaths(key)) {
            now.put(path, Boolean.TRUE);
            String data = Js.savedData(key, path);
            File file = new File(path);
            // Changed, made or deleted here since the last look: this page's copy wins on the next scan.
            boolean mine = !Objects.equals(seen.get(path), file.exists() ? file.lastModified() : null);
            if (data != null && !mine && !data.equals(saved.get(path)) && write(file, data)) {
                saved.put(path, data);
                seen.put(path, file.lastModified());
            }
        }
        saved.keySet().retainAll(now.keySet());
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
