package audiobibledownloader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class TextReader {
   private String fileText = new String();

   public TextReader(String fileName) {
      try {
         File file = new File(fileName);
         FileReader fileReader = new FileReader(file);
         StringBuffer stringBuffer = new StringBuffer();
         char[] charArray = new char[1024];

         int numCharsRead;
         while ((numCharsRead = fileReader.read(charArray)) > 0) {
            stringBuffer.append(charArray, 0, numCharsRead);
         }

         fileReader.close();
         this.fileText = stringBuffer.toString();
      } catch (IOException var7) {
         var7.printStackTrace();
      }
   }

   public String ReturnFileString() {
      return this.fileText;
   }
}
