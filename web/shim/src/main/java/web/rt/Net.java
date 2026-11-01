package web.rt;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * App calls to {@code URL.openStream()} and {@code URLConnection.getInputStream()} are redirected here at compile
 * time (see {@code web.teavm.ShimPlugin}). TeaVM's XHR connection reports a failed request (network or CORS
 * error) as an empty response; the JDK throws IOException, and StockChecker's fallback depends on that.
 */
public final class Net {
    private Net() {
    }

    public static InputStream openStream(URL url) throws IOException {
        return getInputStream(url.openConnection());
    }

    public static InputStream getInputStream(URLConnection connection) throws IOException {
        if (connection instanceof HttpURLConnection) {
            int code = ((HttpURLConnection) connection).getResponseCode();
            if (code < 0) {
                throw new IOException("Connection failed: " + connection.getURL());
            }
            if (code >= 400) {
                throw new IOException("Server returned HTTP response code: " + code + " for URL: " + connection.getURL());
            }
        }
        return connection.getInputStream();
    }
}
