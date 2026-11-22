package launcher;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Starts an app in its own process. {@code started} gets the process as soon as it exists (so the launcher can
 * watch it build and open); the future completes with the exit code when the app closes.
 */
public interface Runner {
   CompletableFuture<Integer> start(String appId, Consumer<ProcessHandle> started) throws IOException;

   /** Where a launch has got to, read from the processes run.ps1 has started. */
   enum Phase {
      READY("STARTING", "Getting ready..."), BUILDING("BUILDING", "Building it from source..."),
      OPENING("OPENING", "Opening its window..."), RUNNING("RUNNING", "Running now");

      final String badge;
      final String line;

      Phase(String badge, String line) {
         this.badge = badge;
         this.line = line;
      }
   }

   /** How long a fresh JVM is taken to need before its window shows. */
   long OPENING_MILLIS = 2000;

   /**
    * The phase from the program names under the launch (javac or jar building, then the app's java) and how long
    * its java has run ({@code -1}: none yet). Building wins, since NathaNOS builds its apps' jars around a short
    * java. Before either, PowerShell (or the Then unpack) is still getting ready.
    */
   static Phase phase(List<String> programs, long javaMillis) {
      if (programs.contains("javac") || programs.contains("jar")) return Phase.BUILDING;
      if (javaMillis >= 0) return javaMillis < OPENING_MILLIS ? Phase.OPENING : Phase.RUNNING;
      return Phase.READY;
   }

   /** The phase of a launch whose PowerShell is {@code shell}, from its descendants' program names and ages. */
   static Phase phase(ProcessHandle shell, long nowMillis) {
      List<String> programs = new ArrayList<>();
      long javaMillis = -1;
      for (ProcessHandle p : (Iterable<ProcessHandle>) shell.descendants()::iterator) {
         String name = p.info().command().map(c -> Path.of(c).getFileName().toString().toLowerCase())
            .orElse("").replaceFirst("\\.exe$", "");
         programs.add(name);
         if (name.equals("java") || name.equals("javaw")) {
            long age = p.info().startInstant().map(t -> nowMillis - t.toEpochMilli()).orElse(0L);
            javaMillis = Math.max(javaMillis, age);
         }
      }
      return phase(programs, javaMillis);
   }

   /**
    * Runs {@code run.ps1 old <App>} with the PowerShell that started the launcher, from the repo root, so each app
    * is compiled and started from its own working folder exactly as from the command line ({@code old} is
    * run.ps1's native mode; without it run.ps1 runs docker compose). Its output goes to the launcher's console.
    */
   static Runner powerShell(String shell, Path repoRoot) {
      return (appId, started) -> runScript(started, shell, repoRoot, null, "old", appId);
   }

   /**
    * Runs the app as first restored: unpacks that commit into {@code thenRoot} (the first time, on a background
    * thread) and runs that commit's own {@code run.ps1 <App>} there (it predates {@code old}). Its home folder is {@code thenHome}, so files
    * the old version saves in the home folder (FlappyBird's high score) stay apart from today's version's.
    */
   static Runner then(String shell, Git git, Path repoRoot, Path thenRoot, Path thenHome) {
      return (appId, started) -> CompletableFuture.supplyAsync(() -> {
         try {
            Files.createDirectories(thenHome);
            return runScript(started, shell, ThenCopy.ensure(git, repoRoot, thenRoot), thenHome, appId);
         } catch (IOException e) {
            throw new UncheckedIOException(e);
         }
      }).thenCompose(f -> f);
   }

   private static CompletableFuture<Integer> runScript(Consumer<ProcessHandle> started, String shell, Path root,
         Path home, String... scriptArgs) throws IOException {
      List<String> command = new ArrayList<>(List.of(shell, "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
         root.resolve("run.ps1").toString()));
      command.addAll(List.of(scriptArgs));
      ProcessBuilder pb = new ProcessBuilder(command);
      pb.directory(new File(root.toString()));
      pb.inheritIO();
      if (home != null) pb.environment().put("JAVA_TOOL_OPTIONS", homeOption(pb.environment().get("JAVA_TOOL_OPTIONS"), home));
      Process p = pb.start();
      started.accept(p.toHandle());
      return p.onExit().thenApply(Process::exitValue);
   }

   /**
    * JAVA_TOOL_OPTIONS that every java the script starts picks up, javac included: any options already set, then
    * the home folder, quoted so a folder with spaces stays one option.
    */
   static String homeOption(String before, Path home) {
      return ((before == null ? "" : before) + " \"-Duser.home=" + home.toAbsolutePath() + "\"").trim();
   }
}
