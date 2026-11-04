package launcher;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Starts an app in its own process. The future completes with the exit code when the app closes. */
public interface Runner {
   CompletableFuture<Integer> start(String appId) throws IOException;

   /**
    * Runs {@code run.ps1 old <App>} with the PowerShell that started the launcher, from the repo root, so each app
    * is compiled and started from its own working folder exactly as from the command line ({@code old} is
    * run.ps1's native mode; without it run.ps1 runs docker compose). Its output goes to the launcher's console.
    */
   static Runner powerShell(String shell, Path repoRoot) {
      return appId -> runScript(shell, repoRoot, null, "old", appId);
   }

   /**
    * Runs the app as first restored: unpacks that commit into {@code thenRoot} (the first time, on a background
    * thread) and runs that commit's own {@code run.ps1 <App>} there (it predates {@code old}). Its home folder is {@code thenHome}, so files
    * the old version saves in the home folder (FlappyBird's high score) stay apart from today's version's.
    */
   static Runner then(String shell, Git git, Path repoRoot, Path thenRoot, Path thenHome) {
      return appId -> CompletableFuture.supplyAsync(() -> {
         try {
            Files.createDirectories(thenHome);
            return runScript(shell, ThenCopy.ensure(git, repoRoot, thenRoot), thenHome, appId);
         } catch (IOException e) {
            throw new UncheckedIOException(e);
         }
      }).thenCompose(f -> f);
   }

   private static CompletableFuture<Integer> runScript(String shell, Path root, Path home, String... scriptArgs)
         throws IOException {
      List<String> command = new ArrayList<>(List.of(shell, "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
         root.resolve("run.ps1").toString()));
      command.addAll(List.of(scriptArgs));
      ProcessBuilder pb = new ProcessBuilder(command);
      pb.directory(new File(root.toString()));
      pb.inheritIO();
      if (home != null) pb.environment().put("JAVA_TOOL_OPTIONS", homeOption(pb.environment().get("JAVA_TOOL_OPTIONS"), home));
      return pb.start().onExit().thenApply(Process::exitValue);
   }

   /**
    * JAVA_TOOL_OPTIONS that every java the script starts picks up, javac included: any options already set, then
    * the home folder, quoted so a folder with spaces stays one option.
    */
   static String homeOption(String before, Path home) {
      return ((before == null ? "" : before) + " \"-Duser.home=" + home.toAbsolutePath() + "\"").trim();
   }
}
