package audiobibledownloader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public class WebpageDownloader {
   /** Reads a page as UTF-8. A missing page (404) throws FileNotFoundException. */
   public String readHtmlPage(String urlString) throws IOException {
      try (InputStream in = open(urlString)) {
         return new String(in.readAllBytes(), StandardCharsets.UTF_8);
      }
   }

   public void downloadWebpage(String filename, String urlString) throws IOException {
      try (InputStream in = open(urlString); OutputStream fout = new FileOutputStream(filename)) {
         in.transferTo(fout);
      }
   }

   private static InputStream open(String urlString) throws IOException {
      URLConnection connection = URI.create(urlString).toURL().openConnection();
      connection.setConnectTimeout(10000);
      connection.setReadTimeout(30000);
      connection.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
      return connection.getInputStream();
   }
}
