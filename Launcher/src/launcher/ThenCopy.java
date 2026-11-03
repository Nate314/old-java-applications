package launcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The repo as first restored ({@link History#THEN}), unpacked from git history into a folder of its own
 * ({@code out/then}), with that commit's own run.ps1, so a Then app is compiled and started exactly as it was then.
 * Unpacked once; a marker file names the commit it holds.
 */
final class ThenCopy {
   static final String MARKER = ".then-commit";

   private ThenCopy() {
   }

   /** Makes sure {@code dir} holds the first restored tree (everything but web/) and returns it. */
   static synchronized Path ensure(Git git, Path dir) throws IOException {
      Path marker = dir.resolve(MARKER);
      if (Files.isRegularFile(marker)
         && new String(Files.readAllBytes(marker), StandardCharsets.UTF_8).trim().equals(History.THEN)) {
         return dir;
      }
      deleteTree(dir);
      Files.createDirectories(dir);
      Path zip = dir.resolveSibling(dir.getFileName() + ".zip");
      git.run("archive", "--format=zip", "-o", zip.toString(), History.THEN, "--", ".", ":(exclude)web");
      try (InputStream in = Files.newInputStream(zip)) {
         unzip(in, dir);
      } finally {
         Files.deleteIfExists(zip);
      }
      Files.write(marker, History.THEN.getBytes(StandardCharsets.UTF_8));
      return dir;
   }

   /** Unpacks a zip into {@code dir}; an entry that would land outside it is refused. */
   static void unzip(InputStream in, Path dir) throws IOException {
      Path base = dir.toAbsolutePath().normalize();
      try (ZipInputStream zip = new ZipInputStream(in)) {
         for (ZipEntry e; (e = zip.getNextEntry()) != null;) {
            Path to = base.resolve(e.getName()).normalize();
            if (!to.startsWith(base) || to.equals(base)) throw new IOException("unsafe path in archive: " + e.getName());
            if (e.isDirectory()) {
               Files.createDirectories(to);
            } else {
               Files.createDirectories(to.getParent());
               Files.copy(zip, to, StandardCopyOption.REPLACE_EXISTING);
            }
         }
      }
   }

   private static void deleteTree(Path dir) throws IOException {
      if (!Files.exists(dir)) return;
      try (Stream<Path> walk = Files.walk(dir)) {
         for (Path p : (Iterable<Path>) walk.sorted(Comparator.reverseOrder())::iterator) Files.delete(p);
      }
   }
}
