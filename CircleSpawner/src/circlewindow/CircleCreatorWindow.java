package circlewindow;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;

/**
 * The creator window, top center of the screen. It builds the chosen shape below itself out of borderless
 * windows, a few per frame so the shape grows in, and pressing any of them scatters the shape.
 */
public final class CircleCreatorWindow extends JFrame {
   private static final String BLUE = "Blue";
   private static final String RAINBOW = "Rainbow";
   private static final String SUNSET = "Sunset";
   private static final int WIDTH = 380;
   private static final int HEIGHT = 112;
   /** About how many frames building a shape takes, whatever its window count. */
   private static final int BUILD_FRAMES = 45;
   private static final int SCATTER_FRAMES = 50;
   private static final int FRAME_MILLIS = 16;

   private final JComboBox<String> shapeBox = new JComboBox<>(Shapes.ALL);
   private final JTextField sizeField = new JTextField("200", 4);
   private final JComboBox<String> colorBox = new JComboBox<>(new String[] {RAINBOW, BLUE, SUNSET});
   private final JTextField wordField = new JTextField("HELLO", 8);
   private final JButton createButton = new JButton("Build it");
   private final JLabel status = new JLabel(" ");
   private final List<CircleSliceWindow> slices = new ArrayList<>();
   private final List<Rectangle> pending = new ArrayList<>();
   private final List<Color> pendingColors = new ArrayList<>();
   private final Random random = new Random();
   private final Timer buildTimer = new Timer(FRAME_MILLIS, event -> this.buildStep());
   private final Timer scatterTimer = new Timer(FRAME_MILLIS, event -> this.scatterStep());
   private String builtShape = "";
   private int built;
   private int scatterFrame;
   private boolean settingShape;

   public CircleCreatorWindow() {
      this.setTitle("Circle Creator");
      this.setSize(WIDTH, HEIGHT);
      this.setDefaultCloseOperation(3);
      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
      this.setLocation(Math.max(0, (screen.width - WIDTH) / 2), 0);

      JPanel panel = new JPanel();
      panel.add(this.shapeBox);
      panel.add(new JLabel("Size"));
      panel.add(this.sizeField);
      panel.add(this.colorBox);
      panel.add(new JLabel("Word"));
      panel.add(this.wordField);
      panel.add(this.createButton);
      panel.add(this.status);
      this.add(panel);

      this.shapeBox.addActionListener(event -> {
         if (!this.settingShape) {
            this.build();
         }
      });
      this.colorBox.addActionListener(event -> this.build());
      this.sizeField.addActionListener(event -> this.build());
      this.wordField.addActionListener(event -> {
         // Enter in the word field builds the word, whatever shape was showing.
         this.settingShape = true;
         this.shapeBox.setSelectedItem(Shapes.WORD);
         this.settingShape = false;
         this.build();
      });
      this.createButton.addActionListener(event -> this.build());
      this.setVisible(true);
      this.build();
   }

   /** Replaces the shape on screen with the chosen one, which then appears a few windows per frame. */
   private void build() {
      this.buildTimer.stop();
      this.scatterTimer.stop();
      this.clear();
      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
      int top = this.getY() + this.getHeight();
      int maxSize = Math.max(2, Math.min(screen.width, screen.height - top));
      int size;
      try {
         size = Integer.parseInt(this.sizeField.getText().trim());
      } catch (NumberFormatException e) {
         size = -1;
      }
      if (size < 2 || size > maxSize) {
         this.status.setText("Size: use 2 to " + maxSize);
         return;
      }
      String shape = String.valueOf(this.shapeBox.getSelectedItem());
      if (Shapes.WORD.equals(shape) && Shapes.cleanWord(this.wordField.getText()).isEmpty()) {
         this.status.setText("Type a word to build");
         return;
      }
      List<Rectangle> pieces = Shapes.build(shape, size, this.wordField.getText(), screen.width / 2, top + (screen.height - top) / 2, screen.width * 9 / 10);
      this.builtShape = shape;
      this.pending.addAll(pieces);
      this.pendingColors.addAll(this.colors(pieces, shape));
      this.built = 0;
      this.status.setText("Building...");
      this.buildTimer.start();
   }

   /** One frame of building: shows the next few windows. */
   private void buildStep() {
      int perFrame = Math.max(1, (this.pending.size() + BUILD_FRAMES - 1) / BUILD_FRAMES);
      for (int i = 0; i < perFrame && this.built < this.pending.size(); i++, this.built++) {
         // The original kept every 10th slice of its circle always on top; the other shapes do not overlap.
         boolean onTop = Shapes.CIRCLE.equals(this.builtShape) && (this.built + 1) % 10 == 0;
         this.slices.add(new CircleSliceWindow(this.pending.get(this.built), this.pendingColors.get(this.built),
            String.valueOf(this.built + 1), onTop, this::scatter));
      }
      if (this.built >= this.pending.size()) {
         this.buildTimer.stop();
         this.status.setText(this.builtShape + " built from " + this.slices.size() + " windows. Click it to scatter.");
      }
   }

   /** Colors along the shape: the circle's slices by order (they share one center), the others from left to right. */
   private List<Color> colors(List<Rectangle> pieces, String shape) {
      String scheme = String.valueOf(this.colorBox.getSelectedItem());
      Rectangle area = new Rectangle(pieces.get(0));
      for (Rectangle r : pieces) {
         area.add(r);
      }
      boolean byOrder = Shapes.CIRCLE.equals(shape);
      List<Color> colors = new ArrayList<>();
      for (int i = 0; i < pieces.size(); i++) {
         Rectangle r = pieces.get(i);
         float t = byOrder ? i / (float)pieces.size() : (r.x + r.width / 2f - area.x) / Math.max(1, area.width);
         if (RAINBOW.equals(scheme)) {
            colors.add(Color.getHSBColor(0.85f * t, 0.75f, 1.0f));
         } else if (SUNSET.equals(scheme)) {
            colors.add(new Color(255, (int)(210 - 170 * t), (int)(60 + 90 * t)));
         } else {
            colors.add(new Color(0, 0, 255));
         }
      }
      return colors;
   }

   /** Sends every window of the shape flying out from its middle and falling, then closes them. */
   private void scatter() {
      if (this.scatterTimer.isRunning() || this.slices.isEmpty()) {
         return;
      }
      this.buildTimer.stop();
      this.pending.clear();
      this.pendingColors.clear();
      double centerX = 0;
      double centerY = 0;
      for (CircleSliceWindow slice : this.slices) {
         centerX += slice.x + slice.getWidth() / 2.0;
         centerY += slice.y + slice.getHeight() / 2.0;
      }
      centerX /= this.slices.size();
      centerY /= this.slices.size();
      for (CircleSliceWindow slice : this.slices) {
         double dx = slice.x + slice.getWidth() / 2.0 - centerX;
         double dy = slice.y + slice.getHeight() / 2.0 - centerY;
         slice.velocityX = dx * 0.08 + (this.random.nextDouble() - 0.5) * 8.0;
         slice.velocityY = dy * 0.08 - 6.0 - this.random.nextDouble() * 6.0;
      }
      this.scatterFrame = 0;
      this.status.setText("Scattered! Pick a shape or press Build it.");
      this.scatterTimer.start();
   }

   /** One frame of scattering: moves each window along its path, with gravity. */
   private void scatterStep() {
      this.scatterFrame++;
      if (this.scatterFrame > SCATTER_FRAMES) {
         this.scatterTimer.stop();
         this.clear();
         return;
      }
      for (CircleSliceWindow slice : this.slices) {
         slice.velocityY += 0.9;
         slice.x += slice.velocityX;
         slice.y += slice.velocityY;
         slice.setLocation((int)slice.x, (int)slice.y);
      }
   }

   /** Closes the shape's windows. */
   private void clear() {
      for (CircleSliceWindow slice : this.slices) {
         slice.dispose();
      }
      this.slices.clear();
      this.pending.clear();
      this.pendingColors.clear();
   }
}
