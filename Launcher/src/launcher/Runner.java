package launcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/** Starts an app in its own process. The future completes with the exit code when the app closes. */
public interface Runner {
   CompletableFuture<Integer> start(String appId) throws IOException;

   /**
    * Runs {@code run.ps1 <App>} with the PowerShell that started the launcher, from the repo root, so each app is
    * compiled and started from its own working folder exactly as from the command line. Its output goes to the
    * launcher's console.
    */
   static Runner powerShell(String shell, Path repoRoot) {
      return appId -> {
         ProcessBuilder pb = new ProcessBuilder(shell, "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
            repoRoot.resolve("run.ps1").toString(), appId);
         pb.directory(new File(repoRoot.toString()));
         pb.inheritIO();
         return pb.start().onExit().thenApply(Process::exitValue);
      };
   }
}
