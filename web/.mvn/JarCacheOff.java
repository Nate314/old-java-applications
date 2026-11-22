import java.net.URLConnection;

// An Ant task (any class with execute()) that web/pom.xml runs inside Maven's JVM before any TeaVM compile. TeaVM 0.16
// compiles in Maven's JVM and reads class files through jar: URLs, which by default share one cached JarFile per jar
// JVM-wide; a finished compile closes its class loader and with it that shared JarFile, so a part still compiling
// failed with "zip file closed". Without the cache each read opens and closes its own JarFile, so no compile can close
// another's. (Antrun's <java fork="false"> can't run it: Ant 1.10.12 sets a SecurityManager, refused on Java 18+.)
public class JarCacheOff {
  public void execute() {
    URLConnection.setDefaultUseCaches("jar", false);
    System.out.println("jar: URL cache off for this build (parallel TeaVM compiles share no JarFile)");
  }
}
