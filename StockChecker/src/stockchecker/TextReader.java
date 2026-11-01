package stockchecker;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class TextReader {
   private String fileText = "";

   public TextReader(String fileName) {
      try {
         File file = new File(fileName + ".txt");
         FileReader fileReader = new FileReader(file);
         StringBuilder stringBuffer = new StringBuilder();
         char[] charArray = new char[1024];

         int numCharsRead;
         while ((numCharsRead = fileReader.read(charArray)) > 0) {
            stringBuffer.append(charArray, 0, numCharsRead);
         }

         fileReader.close();
         this.fileText = stringBuffer.toString();
      } catch (IOException e) {
         System.err.println("Could not read " + fileName + ".txt: " + e.getMessage());
      }
   }

   public String ReturnFileString() {
      return this.fileText;
   }
}
