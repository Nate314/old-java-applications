package stockchecker;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLConnection;

public class WebpageDownloader {
   public void DownloadWebpage(String filename, String urlString) throws MalformedURLException, IOException {
      BufferedInputStream in = null;
      FileOutputStream fout = null;

      try {
         URLConnection connection = URI.create(urlString).toURL().openConnection();
         connection.setConnectTimeout(10000);
         connection.setReadTimeout(10000);
         in = new BufferedInputStream(connection.getInputStream());
         fout = new FileOutputStream(filename);
         byte[] data = new byte[1024];

         int count;
         while ((count = in.read(data, 0, 1024)) != -1) {
            fout.write(data, 0, count);
         }
      } finally {
         if (in != null) {
            in.close();
         }

         if (fout != null) {
            fout.close();
         }
      }
   }
}
