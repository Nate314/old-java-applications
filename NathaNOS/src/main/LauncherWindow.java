package main;

import java.awt.Color;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicInternalFrameUI;

public class LauncherWindow extends JInternalFrame implements ActionListener {
   private Main screen;
   public JPanel panel = new JPanel();
   private ArrayList<JButton> buttons = new ArrayList<>();
   private FileGatherer fileGatherer;
   private Class[] myApplications;
   private String[] applicationNames = new String[]{
      "START", "Minesweeper", "UltimateTickTackToe", "AWTPong", "StockChecker", "Clock", "Piano", "Calculator", "NumberGuesser", "Rubik's Cube"
   };
   private String[] iconPaths = new String[]{
      "assets/NathaNOS.png",
      "assets/mine.png",
      "assets/tickTackToe.png",
      "assets/pingPong.png",
      "assets/stockGraph.png",
      "assets/clock.png",
      "assets/piano.png",
      "assets/calculator.png",
      "assets/numberGuesser.png",
      "assets/cube.png"
   };

   public LauncherWindow(Main screen) {
      super("Launcher", false, false, false);
      this.screen = screen;
      this.setResizable(true);
      this.setDefaultCloseOperation(3);
      this.setBorder(null);
      this.setSize(screen.getWidth(), screen.getHeight() / 20);
      this.setLocation(0, screen.getHeight() - this.getHeight());
      this.setVisible(true);
      this.setCursor(screen.getCursor());
      ((BasicInternalFrameUI)this.getUI()).setNorthPane(null);
      this.add(this.panel);
      this.fileGatherer = new FileGatherer();
      this.panel.setBackground(new Color(0, 150, 255));
      this.panel.setLayout(null);

      for (int i = 0; i < this.fileGatherer.applicationNames.size(); i++) {
         this.buttons.add(new JButton(this.fileGatherer.applicationNames.get(i)));
      }

      int startButtonWidth = Toolkit.getDefaultToolkit().getScreenSize().width / 8;
      int buttonWidth = (this.getWidth() - startButtonWidth) / this.buttons.size();
      this.buttons.get(0).setBounds(0, 0, startButtonWidth, this.getHeight());
      this.buttons.get(0).addActionListener(this);
      this.buttons
         .get(0)
         .setIcon(
            new ImageIcon(new ImageIcon(this.fileGatherer.iconPaths.get(0)).getImage().getScaledInstance(this.getHeight() - 20, this.getHeight() - 20, 4))
         );
      this.panel.add(this.buttons.get(0));
      this.buttons.get(0).setBackground(new Color(0, 150, 255));

      for (int i = 1; i < this.buttons.size(); i++) {
         this.buttons.get(i).setBounds(startButtonWidth + (i - 1) * buttonWidth, 0, buttonWidth, this.getHeight());
         this.buttons.get(i).addActionListener(this);
         this.buttons
            .get(i)
            .setIcon(
               new ImageIcon(new ImageIcon(this.fileGatherer.iconPaths.get(i)).getImage().getScaledInstance(this.getHeight() - 20, this.getHeight() - 20, 4))
            );
         this.buttons.get(i).setBackground(new Color(0, 255, 255));
         this.panel.add(this.buttons.get(i));
      }

      this.myApplications = this.fileGatherer.getAllJFrameClasses();
   }

   private void createNewWindow(JFrame window, boolean resizable, Image iconImage) {
      GenericAppFrame appWindow = new GenericAppFrame(window.getTitle(), window.getContentPane(), resizable, iconImage, this.screen);
      appWindow.setSize(window.getWidth(), window.getHeight());
      appWindow.setPosition(this.screen.getWidth() / 2 - window.getWidth() / 2, this.screen.getHeight() / 2 - window.getHeight() / 2);
      this.screen.desktopPane.add(appWindow);

      try {
         this.screen.desktopPane.getAllFrames()[this.screen.desktopPane.getAllFrames().length - 1].setSelected(true);
      } catch (PropertyVetoException exception) {
         exception.printStackTrace();
      }
   }

   @Override
   public void actionPerformed(ActionEvent e) {
      if (e.getSource() == this.buttons.get(0)) {
         this.screen.startMenu.toggleVisibility();
      }

      for (int i = 1; i < this.buttons.size(); i++) {
         if (e.getSource() == this.buttons.get(i)) {
            System.out.println("buttonPressed");

            try {
               this.createNewWindow(
                  (JFrame)this.myApplications[i - 1].getConstructor(Boolean.class).newInstance(false),
                  false,
                  new ImageIcon(this.fileGatherer.iconPaths.get(i)).getImage()
               );
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | InstantiationException exception) {
               System.out.println("FFFFAAAAIIIILLL");
               exception.printStackTrace();
            }
         }
      }
   }
}
