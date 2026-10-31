package groupcreator;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class Main extends JFrame {
   public Main(Boolean visibility) {
      this.setVisible(visibility);
      this.setDefaultCloseOperation(3);
      this.setTitle("Group Creator");
      this.setResizable(false);
      JPanel panel = new JPanel();
      JLabel label = new JLabel(
         "<html>Please enter a list of Names separated by commas:<br>(ex: <b>Steve Jobs,Bill Gates,Stephen Hawking,Morgan Freeman</b>)<html>"
      );
      final JTextField textField = new JTextField();
      textField.setColumns(30);
      JLabel label2 = new JLabel("per group:");
      JLabel minLabel = new JLabel("min: ");
      final JTextField minTextField = new JTextField("1");
      minTextField.setColumns(6);
      JLabel maxLabel = new JLabel("max: ");
      final JTextField maxTextField = new JTextField("3");
      maxTextField.setColumns(6);
      JButton button = new JButton("OK");
      final JFrame frame = this;
      button.addActionListener(
         new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               System.out.println(frame.getWidth() + ", " + frame.getHeight());

               try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("output.txt"), "utf-8"))) {
                  writer.write(
                     Main.this.generateString(textField.getText(), Integer.parseInt(minTextField.getText()), Integer.parseInt(maxTextField.getText()))
                  );
               } catch (IOException var13) {
                  var13.printStackTrace();
               }
            }
         }
      );
      panel.add(label);
      panel.add(textField);
      panel.add(label2);
      panel.add(minLabel);
      panel.add(minTextField);
      panel.add(maxLabel);
      panel.add(maxTextField);
      panel.add(button);
      this.add(panel);
      this.setSize(375, 130);
   }

   private String[] shuffle(String[] array) {
      for (int i = 0; i < array.length; i++) {
         int index = (int)(Math.random() * (double)array.length);
         index = index == array.length ? array.length - 1 : index;
         String temp = array[i];
         array[i] = array[index];
         array[index] = temp;
      }

      return array;
   }

   private String generateString(String list, int min, int max) {
      String[] names = list.split(",");
      names = this.shuffle(names);
      int range = max - min;
      int accounted = 0;
      ArrayList<Integer> numbers = new ArrayList<>();

      while (true) {
         int randomInt = (int)(new Random().nextDouble() * (double)range) + min;
         if (accounted + randomInt > names.length) {
            numbers.add(names.length - accounted);
            randomInt = 0;
            String resault = "";

            for (int i = 0; i < numbers.size(); i++) {
               resault = resault + "\r\nGroup" + i + " --- ";

               for (int j = 0; j < numbers.get(i); j++) {
                  resault = resault + names[randomInt] + ", ";
                  randomInt++;
               }
            }

            System.out.println(resault);
            return resault;
         }

         numbers.add(randomInt);
         accounted += randomInt;
      }
   }

   public static void main(String[] args) {
      new Main(true);
   }
}
