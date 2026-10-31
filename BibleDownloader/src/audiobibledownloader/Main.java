package audiobibledownloader;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

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
   // Written by the download thread, painted on the event thread.
   private volatile int percentage = 100;
   private volatile String status = "";
   private JPanel progressBar = new JPanel() {
      @Override
      public void paint(Graphics g) {
         int height = this.getHeight();
         ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
         g.setColor(new Color(255, 0, 0));
         g.fillRect(0, 0, this.getWidth(), height);
         g.setColor(new Color(0, 255, 0));
         g.fillRect(0, 0, this.getWidth() * Main.this.percentage / 100, height);
         g.setColor(new Color(0, 0, 0));
         g.setFont(new Font("Times", 1, (int)((double)height * 0.75)));
         g.drawString(Main.this.percentage + "%", 0, (int)((double)height * 0.75));
         g.setFont(new Font("Times", 0, height / 2));
         g.drawString(Main.this.status, (int)((double)height * 2.5), (int)((double)height * 0.7));
      }
   };
   private int xMod = 0;
   private int yMod = 0;

   public Main(Boolean visibility) {
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
      this.mySetBounds(this.progressBar, 0.0, 3.5, 8.0, 1.0);
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
         this.progressBar
      };

      for (int i = 0; i < components.length; i++) {
         this.add(components[i]);
      }

      this.downloadAudioButton.addActionListener(e -> this.startDownload(true));
      this.downloadTextButton.addActionListener(e -> this.startDownload(false));
      this.setSize(this.getWidth() + 5, this.getHeight() + 2);
      this.setVisible(visibility);
   }

   private void startDownload(boolean audio) {
      int chapter1;
      int chapter2;
      try {
         chapter1 = Integer.parseInt(this.chapterTextField1.getText().trim());
         chapter2 = Integer.parseInt(this.chapterTextField2.getText().trim());
      } catch (NumberFormatException ex) {
         this.showProgress(this.percentage, "chapters must be whole numbers");
         return;
      }
      if (chapter2 < chapter1) {
         return;
      }

      // One download at a time, so two downloads never share the progress bar.
      this.downloadAudioButton.setEnabled(false);
      this.downloadTextButton.setEnabled(false);
      String book = this.bookTextField.getText().trim();
      if (audio) {
         DownloadManager.downloadAudio(this, book, chapter1, chapter2);
      } else {
         DownloadManager.downloadText(this, book, chapter1, chapter2, !this.allInOneCheckbox.isSelected(), this.showVerseAndChapterCheckbox.isSelected());
      }
   }

   /** Safe to call from the download thread. */
   void showProgress(int percentage, String status) {
      this.percentage = percentage;
      this.status = status;
      this.progressBar.repaint();
   }

   /** Called from the download thread when the whole range is done. */
   void downloadFinished(String status) {
      SwingUtilities.invokeLater(() -> {
         this.showProgress(100, status);
         this.downloadAudioButton.setEnabled(true);
         this.downloadTextButton.setEnabled(true);
      });
   }

   private void mySetBounds(Component c, double x, double y, double width, double height) {
      c.setBounds((int)(x * (double)this.xMod), (int)(y * (double)this.yMod), (int)(width * (double)this.xMod), (int)(height * (double)this.yMod));
   }

   public static void main(String[] args) {
      SwingUtilities.invokeLater(() -> new Main(true));
   }
}
