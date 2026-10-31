package audiobibledownloader;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class Main extends JFrame {
   private JLabel bookLabel = new JLabel("Book:");
   private JTextField bookTextField = new JTextField("Luke");
   private JLabel chapterLabel = new JLabel("Chapters:");
   private JTextField chapterTextField1 = new JTextField("1");
   private JLabel chapterLabelDash = new JLabel("-");
   private JTextField chapterTextField2 = new JTextField("24");
   private JLabel downloadLabel = new JLabel("Download:");
   private JButton downloadAudioButton = new JButton("Audio");
   private JButton downloadTextButton = new JButton("Text");
   private JCheckBox allInOneCheckbox = new JCheckBox("Separate", false);
   private JCheckBox showVerseAndChapterCheckbox = new JCheckBox("Ch&V", true);
   public static int percentage = 100;
   public static JPanel progressBar = new JPanel() {
      public void setText(String text) {
         this.getGraphics().drawString(text, this.getWidth() / 2, (int)((double)this.getHeight() * 0.75));
      }

      @Override
      public void paint(Graphics g) {
         g.setColor(new Color(255, 0, 0));
         g.fillRect(0, 0, Main.progressBar.getWidth(), Main.progressBar.getHeight());
         g.setColor(new Color(0, 255, 0));
         g.fillRect(0, 0, Main.progressBar.getWidth() * Main.percentage / 100, Main.progressBar.getHeight());
         g.setColor(new Color(0, 0, 0));
         g.setFont(new Font("Times", 1, (int)((double)this.getHeight() * 0.75)));
         g.drawString(Main.percentage + "%", 0, (int)((double)this.getHeight() * 0.75));
      }
   };
   private JPanel progressBarBackground = new JPanel();
   private int xMod = 0;
   private int yMod = 0;

   public Main(Boolean visibility) {
      this.setVisible(visibility);
      this.setTitle("BibleDownloader1.3.1");
      this.setIconImage(Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/assets/bible.png")));
      this.setLayout(null);
      this.setResizable(false);
      this.setDefaultCloseOperation(3);
      this.setSize(325, 150);
      this.xMod = this.getWidth() / 8;
      this.yMod = (int)((double)this.getHeight() / 5.5);
      this.mySetBounds(this.bookLabel, 0.0, 0.0, 2.0, 1.0);
      this.mySetBounds(this.bookTextField, 2.0, 0.125, 6.0, 0.75);
      this.mySetBounds(this.chapterLabel, 0.0, 1.0, 2.0, 1.0);
      this.mySetBounds(this.chapterTextField1, 2.0, 1.125, 2.75, 0.75);
      this.mySetBounds(this.chapterLabelDash, 4.75, 1.125, 0.5, 0.5);
      this.mySetBounds(this.chapterTextField2, 5.25, 1.125, 2.75, 0.75);
      this.mySetBounds(this.downloadLabel, 0.0, 2.0, 2.0, 1.5);
      this.mySetBounds(this.downloadAudioButton, 2.0, 2.0, 2.0, 1.5);
      this.mySetBounds(this.downloadTextButton, 4.0, 2.0, 2.0, 1.5);
      this.mySetBounds(this.allInOneCheckbox, 6.0, 2.0, 2.0, 0.75);
      this.mySetBounds(this.showVerseAndChapterCheckbox, 6.0, 2.75, 2.0, 0.75);
      this.mySetBounds(progressBar, 0.0, 3.5, 8.0, 1.0);
      Component[] components = new Component[]{
         this.bookLabel,
         this.bookTextField,
         this.chapterLabel,
         this.chapterTextField1,
         this.chapterLabelDash,
         this.chapterTextField2,
         this.downloadLabel,
         this.downloadAudioButton,
         this.downloadTextButton,
         this.allInOneCheckbox,
         this.showVerseAndChapterCheckbox,
         this.progressBarBackground,
         progressBar
      };

      for (int i = 0; i < components.length; i++) {
         this.add(components[i]);
      }

      this.downloadAudioButton
         .addActionListener(
            new ActionListener() {
               @Override
               public void actionPerformed(ActionEvent e) {
                  if (Integer.parseInt(Main.this.chapterTextField2.getText()) - Integer.parseInt(Main.this.chapterTextField1.getText()) >= 0) {
                     DownloadManager.downloadAudio(
                        Main.this.bookTextField.getText(),
                        Integer.parseInt(Main.this.chapterTextField1.getText()),
                        Integer.parseInt(Main.this.chapterTextField2.getText())
                     );
                  }
               }
            }
         );
      this.downloadTextButton
         .addActionListener(
            new ActionListener() {
               @Override
               public void actionPerformed(ActionEvent e) {
                  if (Integer.parseInt(Main.this.chapterTextField2.getText()) - Integer.parseInt(Main.this.chapterTextField1.getText()) >= 0) {
                     DownloadManager.text = "";
                     DownloadManager.allInOne = !Main.this.allInOneCheckbox.isSelected();
                     DownloadManager.showVerseAndChapter = Main.this.showVerseAndChapterCheckbox.isSelected();
                     DownloadManager.downloadText(
                        Main.this.bookTextField.getText(),
                        Integer.parseInt(Main.this.chapterTextField1.getText()),
                        Integer.parseInt(Main.this.chapterTextField2.getText())
                     );
                  }
               }
            }
         );
      DownloadManager.allInOne = !this.allInOneCheckbox.isSelected();
      DownloadManager.showVerseAndChapter = this.showVerseAndChapterCheckbox.isSelected();
      progressBar.repaint();
      this.setSize(this.getWidth() + 5, this.getHeight() + 2);
   }

   private void mySetBounds(Component c, double x, double y, double width, double height) {
      c.setBounds((int)(x * (double)this.xMod), (int)(y * (double)this.yMod), (int)(width * (double)this.xMod), (int)(height * (double)this.yMod));
   }

   public static void main(String[] args) {
      new Main(true);
   }
}
