package audiobibledownloader;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.JOptionPane;

public class WebpageDownloader {
   public String readHtmlPage(String urlString) {
      StringBuilder parsedContentFromUrl = new StringBuilder();

      try {
         try {
            URL url = new URL(urlString);
            URLConnection uc = url.openConnection();
            uc.connect();
            uc = url.openConnection();
            uc.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
            uc.getInputStream();
            BufferedInputStream in = new BufferedInputStream(uc.getInputStream());

            int ch;
            while ((ch = in.read()) != -1) {
               parsedContentFromUrl.append((char)ch);
            }
         } catch (FileNotFoundException ex) {
            JOptionPane.showConfirmDialog(null, DownloadManager.book + " " + DownloadManager.currentChapter + " does not exist!\nTry again", "Press OK", 2, 1);
            Main.percentage = 100;
            Main.progressBar.repaint();
         }
      } catch (MalformedURLException ex) {
         ex.printStackTrace();
      } catch (IOException ex) {
         ex.printStackTrace();
      }

      return parsedContentFromUrl.toString();
   }

   public void downloadWebpage(String filename, String urlString) throws MalformedURLException, IOException {
      BufferedInputStream in = null;
      FileOutputStream fout = null;

      try {
         in = new BufferedInputStream(new URL(urlString).openStream());
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
