package stockchecker;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class CSVReader {
   public String[][] CSVToDoubleStringArray(String fileName) {
      String[][] CSVString = new String[100][10];

      try {
         BufferedReader br = new BufferedReader(new FileReader(fileName + ".csv"));

         for (int i = 0; i < 100; i++) {
            String line = br.readLine();
            System.out.println(line);
            CSVString[i] = line.split(",");
            System.out.println(CSVString[i][1]);
         }

         br.close();
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }

      return CSVString;
   }
}
