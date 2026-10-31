package rubikscube;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.KeyboardFocusManager;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public final class CubeFrame extends JFrame implements ActionListener {
   private String[] buttonLabels = new String[]{"", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
   private JButton[][] stickerButtons = new JButton[][]{
      {
            new JButton(this.buttonLabels[1]),
            new JButton(this.buttonLabels[2]),
            new JButton(this.buttonLabels[3]),
            new JButton(this.buttonLabels[4]),
            new JButton(this.buttonLabels[5]),
            new JButton(this.buttonLabels[6]),
            new JButton(this.buttonLabels[7]),
            new JButton(this.buttonLabels[8]),
            new JButton(this.buttonLabels[9])
      },
      {
            new JButton(this.buttonLabels[1]),
            new JButton(this.buttonLabels[2]),
            new JButton(this.buttonLabels[3]),
            new JButton(this.buttonLabels[4]),
            new JButton(this.buttonLabels[5]),
            new JButton(this.buttonLabels[6]),
            new JButton(this.buttonLabels[7]),
            new JButton(this.buttonLabels[8]),
            new JButton(this.buttonLabels[9])
      },
      {
            new JButton(this.buttonLabels[1]),
            new JButton(this.buttonLabels[2]),
            new JButton(this.buttonLabels[3]),
            new JButton(this.buttonLabels[4]),
            new JButton(this.buttonLabels[5]),
            new JButton(this.buttonLabels[6]),
            new JButton(this.buttonLabels[7]),
            new JButton(this.buttonLabels[8]),
            new JButton(this.buttonLabels[9])
      },
      {
            new JButton(this.buttonLabels[1]),
            new JButton(this.buttonLabels[2]),
            new JButton(this.buttonLabels[3]),
            new JButton(this.buttonLabels[4]),
            new JButton(this.buttonLabels[5]),
            new JButton(this.buttonLabels[6]),
            new JButton(this.buttonLabels[7]),
            new JButton(this.buttonLabels[8]),
            new JButton(this.buttonLabels[9])
      },
      {
            new JButton(this.buttonLabels[1]),
            new JButton(this.buttonLabels[2]),
            new JButton(this.buttonLabels[3]),
            new JButton(this.buttonLabels[4]),
            new JButton(this.buttonLabels[5]),
            new JButton(this.buttonLabels[6]),
            new JButton(this.buttonLabels[7]),
            new JButton(this.buttonLabels[8]),
            new JButton(this.buttonLabels[9])
      },
      {
            new JButton(this.buttonLabels[1]),
            new JButton(this.buttonLabels[2]),
            new JButton(this.buttonLabels[3]),
            new JButton(this.buttonLabels[4]),
            new JButton(this.buttonLabels[5]),
            new JButton(this.buttonLabels[6]),
            new JButton(this.buttonLabels[7]),
            new JButton(this.buttonLabels[8]),
            new JButton(this.buttonLabels[9])
      }
   };
   private JButton[] moveButtons = new JButton[]{new JButton("w"), new JButton("a"), new JButton("s"), new JButton("d")};
   private JButton mixButton = new JButton("Mix");
   private JLabel[] mixLabels = new JLabel[]{new JLabel("Moves:"), new JLabel("Secs:")};
   private JTextField[] mixFields = new JTextField[]{new JTextField("1000"), new JTextField("0.5")};
   public int selectedCubie = 0;
   private JPanel panel = new JPanel();
   private int frameWidth = 0;
   private int frameHeight = 0;
   private int cellWidth = 0;
   private int cellHeight = 0;
   protected Cube cube = null;

   public CubeFrame() {
      this.setTitle("Rubik's Cube");
      this.setSize(550, 700);
      this.frameWidth = this.getWidth();
      this.frameHeight = this.getHeight();
      this.cellWidth = this.frameWidth / 11;
      this.cellHeight = this.frameHeight / 14;
      this.cube = new Cube(this, this.stickerButtons, this.cellWidth, this.cellHeight);
      this.setLocationRelativeTo(null);
      this.setDefaultCloseOperation(3);
      this.setResizable(true);
      this.add(this.panel);
      this.panel.setLayout(null);
      this.panel.setBackground(new Color(255, 255, 255));
      KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new MoveKeyDispatcher(this));
      this.moveButtons[0].addActionListener(this);
      this.moveButtons[1].addActionListener(this);
      this.moveButtons[2].addActionListener(this);
      this.moveButtons[3].addActionListener(this);
      this.moveButtons[0]
         .setBounds(
            this.cellWidth + (int)(2.75 * (double)this.cellWidth / 3.0), (int)(1.75 * (double)this.cellHeight), (int)(2.75 * (double)this.cellWidth / 3.0), (int)(2.75 * (double)this.cellHeight / 3.0)
         );
      this.moveButtons[1].setBounds(this.moveButtons[0].getX() - this.moveButtons[0].getWidth(), this.moveButtons[0].getY() + this.moveButtons[0].getHeight(), this.moveButtons[0].getWidth(), this.moveButtons[0].getHeight());
      this.moveButtons[2].setBounds(this.moveButtons[1].getX() + this.moveButtons[1].getWidth(), this.moveButtons[1].getY(), this.moveButtons[0].getWidth(), this.moveButtons[0].getHeight());
      this.moveButtons[3].setBounds(this.moveButtons[2].getX() + this.moveButtons[2].getWidth(), this.moveButtons[1].getY(), this.moveButtons[0].getWidth(), this.moveButtons[0].getHeight());
      this.panel.add(this.moveButtons[0]);
      this.panel.add(this.moveButtons[1]);
      this.panel.add(this.moveButtons[2]);
      this.panel.add(this.moveButtons[3]);
      this.mixLabels[0].setBounds((int)(7.5 * (double)this.cellWidth), this.cellHeight, (int)(1.25 * (double)this.cellWidth), this.cellHeight / 2);
      this.mixLabels[1].setBounds((int)(8.75 * (double)this.cellWidth), this.cellHeight, (int)(1.25 * (double)this.cellWidth), this.cellHeight / 2);
      this.panel.add(this.mixLabels[0]);
      this.panel.add(this.mixLabels[1]);
      this.mixFields[0].setBounds((int)(7.5 * (double)this.cellWidth), (int)(1.5 * (double)this.cellHeight), (int)(1.25 * (double)this.cellWidth), this.cellHeight / 2);
      this.mixFields[1].setBounds((int)(8.75 * (double)this.cellWidth), (int)(1.5 * (double)this.cellHeight), (int)(1.25 * (double)this.cellWidth), this.cellHeight / 2);
      this.panel.add(this.mixFields[0]);
      this.panel.add(this.mixFields[1]);
      this.mixButton.addActionListener(this);
      this.mixButton.setBounds((int)(7.5 * (double)this.cellWidth), 2 * this.cellHeight, (int)(2.5 * (double)this.cellWidth), (int)(1.5 * (double)this.cellHeight));
      this.panel.add(this.mixButton);
      this.cube.topFace.panel.setBounds(4 * this.cellWidth, this.cellHeight, 3 * this.cellWidth, 3 * this.cellHeight);
      this.cube.frontFace.panel.setBounds(4 * this.cellWidth, 4 * this.cellHeight, 3 * this.cellWidth, 3 * this.cellHeight);
      this.cube.rightFace.panel.setBounds(7 * this.cellWidth, 4 * this.cellHeight, 3 * this.cellWidth, 3 * this.cellHeight);
      this.cube.bottomFace.panel.setBounds(4 * this.cellWidth, 7 * this.cellHeight, 3 * this.cellWidth, 3 * this.cellHeight);
      this.cube.backFace.panel.setBounds(4 * this.cellWidth, 10 * this.cellHeight, 3 * this.cellWidth, 3 * this.cellHeight);
      this.cube.leftFace.panel.setBounds(this.cellWidth, 4 * this.cellHeight, 3 * this.cellWidth, 3 * this.cellHeight);
      this.cube.topFace.panel.setBackground(new Color(0, 0, 0));
      this.cube.frontFace.panel.setBackground(new Color(0, 0, 0));
      this.cube.rightFace.panel.setBackground(new Color(0, 0, 0));
      this.cube.bottomFace.panel.setBackground(new Color(0, 0, 0));
      this.cube.backFace.panel.setBackground(new Color(0, 0, 0));
      this.cube.leftFace.panel.setBackground(new Color(0, 0, 0));
      this.panel.add(this.cube.topFace.panel);
      this.panel.add(this.cube.frontFace.panel);
      this.panel.add(this.cube.rightFace.panel);
      this.panel.add(this.cube.bottomFace.panel);
      this.panel.add(this.cube.backFace.panel);
      this.panel.add(this.cube.leftFace.panel);
   }

   @Override
   public final void actionPerformed(ActionEvent event) {
      if (event.getSource() == this.moveButtons[0]) {
         try {
            Robot robot;
            (robot = new Robot()).keyPress(87);
            robot.keyRelease(87);
         } catch (AWTException e) {
            e.printStackTrace();
         }
      }

      if (event.getSource() == this.moveButtons[1]) {
         try {
            Robot robot;
            (robot = new Robot()).keyPress(65);
            robot.keyRelease(65);
         } catch (AWTException e) {
            e.printStackTrace();
         }
      }

      if (event.getSource() == this.moveButtons[2]) {
         try {
            Robot robot;
            (robot = new Robot()).keyPress(83);
            robot.keyRelease(83);
         } catch (AWTException e) {
            e.printStackTrace();
         }
      }

      if (event.getSource() == this.moveButtons[3]) {
         try {
            Robot robot;
            (robot = new Robot()).keyPress(68);
            robot.keyRelease(68);
         } catch (AWTException e) {
            e.printStackTrace();
         }
      }

      if (event.getSource() == this.mixButton) {
         new Scrambler(Integer.parseInt(this.mixFields[0].getText()), Double.parseDouble(this.mixFields[1].getText()), this.cube);
      }
   }
}
