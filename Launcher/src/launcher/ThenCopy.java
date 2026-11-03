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
 *
 * One thing is not as it was then: the art. The images FlappyBird, BibleDownloader and NathaNOS loaded then were
 * taken from other apps or from clip art of unknown origin (Flappy Bird's sprites, a Bible app's icon, the Windows
 * logo and Minesweeper mine), so they are
 * replaced by today's original art ({@link #ART}) and the ones nothing reads any more are left out ({@link #GONE}).
 */
final class ThenCopy {
   static final String MARKER = ".then-commit";
   /** What the marker holds: the commit, and that the art has been swapped in. */
   static final String STAMP = History.THEN + " original-art";
   /** Today's art, copied from the repo over the first restored files of the same name. */
   static final String[] ART = {
      "FlappyBird/src/bg.jpg", "FlappyBird/src/ground.jpg", "FlappyBird/src/topPipe.png", "FlappyBird/src/bottomPipe.png",
      "FlappyBird/src/bird.png", "BibleDownloader/src/assets/bible.png", "NathaNOS/assets/Minesweeper.png",
      "NathaNOS/assets/NumberGuess.png", "NathaNOS/assets/StockChecker.png", "NathaNOS/assets/SynthesizerKeyboard.png",
      "NathaNOS/assets/UltimateTicTacToe.png", "NathaNOS/assets/clock.png", "NathaNOS/assets/cursor.png" };
   /** First restored images that are removed (stockChecker.png comes back as StockChecker.png from ART). */
   static final String[] GONE = {
      "NathaNOS/assets/windowsLogo.png", "NathaNOS/assets/calculator.png", "NathaNOS/assets/converter.png",
      "NathaNOS/assets/pingPong.png", "NathaNOS/assets/stockChecker.png" };

   private ThenCopy() {
   }

   /** Makes sure {@code dir} holds the first restored tree (everything but web/, with the art of {@code repoRoot}). */
   static synchronized Path ensure(Git git, Path repoRoot, Path dir) throws IOException {
      Path marker = dir.resolve(MARKER);
      if (Files.isRegularFile(marker)
         && new String(Files.readAllBytes(marker), StandardCharsets.UTF_8).trim().equals(STAMP)) {
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
      for (String gone : GONE) {
         Files.deleteIfExists(dir.resolve(gone));
      }
      for (String art : ART) {
         Files.createDirectories(dir.resolve(art).getParent());
         Files.copy(repoRoot.resolve(art), dir.resolve(art), StandardCopyOption.REPLACE_EXISTING);
      }
      Files.write(marker, STAMP.getBytes(StandardCharsets.UTF_8));
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
