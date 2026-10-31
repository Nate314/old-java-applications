package web.shim.java.net;

import java.net.URL;

/**
 * There is no class loading in the browser: every class was compiled into the page ahead of time, so
 * loadClass looks the name up among them (TeaVM keeps names only for classes listed in ShimReflectionPolicy).
 */
public class TURLClassLoader {
    private final URL[] urls;

    public TURLClassLoader(URL[] urls) {
        this.urls = urls;
    }

    public static TURLClassLoader newInstance(URL[] urls) {
        return new TURLClassLoader(urls);
    }

    public URL[] getURLs() {
        return urls.clone();
    }

    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }

    public void close() {
    }
}
