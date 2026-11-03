package launcher;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Runs git in the repo and returns what it prints. A git that is missing or fails throws IOException. */
@FunctionalInterface
public interface Git {
   /** Runs {@code git args} and returns its output. */
   String run(String... args) throws IOException;

   static Git in(Path repoRoot) {
      return args -> {
         List<String> cmd = new ArrayList<>();
         cmd.add("git");
         cmd.addAll(Arrays.asList(args));
         ProcessBuilder pb = new ProcessBuilder(cmd);
         pb.directory(new File(repoRoot.toString()));
         pb.redirectError(ProcessBuilder.Redirect.DISCARD);
         Process p = pb.start();
         p.getOutputStream().close();
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         try (InputStream s = p.getInputStream()) {
            s.transferTo(out);
         }
         try {
            int code = p.waitFor();
            if (code != 0) throw new IOException("git " + args[0] + " failed with exit code " + code);
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted", e);
         }
         return new String(out.toByteArray(), StandardCharsets.UTF_8);
      };
   }
}
