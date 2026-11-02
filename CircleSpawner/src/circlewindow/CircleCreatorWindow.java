package circlewindow;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Arrays;
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
 * windows, a few per frame so the shape grows in. Choosing another shape, size or colors morphs the windows
 * on screen into it, and drawing on the pad (the Draw shape) reshapes them as you draw. Pressing any window
 * of the shape scatters it.
 */
public final class CircleCreatorWindow extends JFrame {
   private static final String BLUE = "Blue";
   private static final String RAINBOW = "Rainbow";
   private static final String SUNSET = "Sunset";
   private static final int WIDTH = 380;
   private static final int HEIGHT = 112;
   /** The window with the drawing pad beside its controls, or (on a narrow screen) under them. */
   private static final int DRAW_WIDTH = 560;
   private static final int DRAW_HEIGHT = 250;
   private static final int DRAW_BELOW_HEIGHT = 320;
   /** About how many frames building a shape takes, whatever its window count. */
   private static final int BUILD_FRAMES = 45;
   private static final int SCATTER_FRAMES = 50;
   /** Frames for one shape to turn into another, and for the drawing to follow the pen. */
   private static final int MORPH_FRAMES = 40;
   private static final int DRAW_FRAMES = 10;
   private static final int FRAME_MILLIS = 16;

   private final JComboBox<String> shapeBox = new JComboBox<>(Shapes.ALL);
   private final JTextField sizeField = new JTextField("200", 4);
   private final JComboBox<String> colorBox = new JComboBox<>(new String[] {RAINBOW, BLUE, SUNSET});
   private final JTextField wordField = new JTextField("HELLO", 8);
   private final JButton createButton = new JButton("Build it");
   private final JLabel status = new JLabel(" ");
   private final DrawPad pad = new DrawPad(() -> this.show(DRAW_FRAMES));
   private final JButton mirrorButton = new JButton();
   private final JPanel padPanel = new JPanel(new BorderLayout());
   private final List<CircleSliceWindow> slices = new ArrayList<>();
   private final List<Rectangle> pending = new ArrayList<>();
   private final List<Color> pendingColors = new ArrayList<>();
   private final Random random = new Random();
   private final Timer buildTimer = new Timer(FRAME_MILLIS, event -> this.buildStep());
   private final Timer scatterTimer = new Timer(FRAME_MILLIS, event -> this.scatterStep());
   private final Timer morphTimer = new Timer(FRAME_MILLIS, event -> this.morphStep());
   private final Saved saved = Saved.load();
   private Morph morph;
   private String builtShape = "";
   private int built;
   private int scatterFrame;
   private boolean settingUp = true;

   public CircleCreatorWindow() {
      this.setTitle("Circle Creator");
      this.setDefaultCloseOperation(3);
      this.shapeBox.setSelectedItem(this.saved.get("shape", Shapes.CIRCLE));
      this.colorBox.setSelectedItem(this.saved.get("colors", RAINBOW));
      this.sizeField.setText(this.saved.get("size", "200"));
      this.wordField.setText(this.saved.get("word", "HELLO"));
      String drawing = this.saved.get("drawing", "");
      if (drawing.isEmpty()) {
         this.pad.smile();
      } else {
         this.pad.setText(drawing);
      }
      this.pad.setMirror(!"0".equals(this.saved.get("mirror", "1")));

      JPanel panel = new JPanel();
      panel.add(this.shapeBox);
      panel.add(new JLabel("Size"));
      panel.add(this.sizeField);
      panel.add(this.colorBox);
      panel.add(new JLabel("Word"));
      panel.add(this.wordField);
      panel.add(this.createButton);
      panel.add(this.status);

      JButton clearButton = new JButton("Clear");
      JPanel padButtons = new JPanel();
      padButtons.add(clearButton);
      padButtons.add(this.mirrorButton);
      JPanel padHolder = new JPanel();
      padHolder.add(this.pad);
      this.padPanel.add(padHolder, BorderLayout.CENTER);
      this.padPanel.add(padButtons, BorderLayout.SOUTH);
      this.add(panel, BorderLayout.CENTER);
      this.showMirror();

      this.shapeBox.addActionListener(event -> {
         if (!this.settingUp) {
            this.fitWindow();
            this.show(MORPH_FRAMES);
         }
      });
      this.colorBox.addActionListener(event -> this.show(MORPH_FRAMES));
      this.sizeField.addActionListener(event -> this.show(MORPH_FRAMES));
      this.wordField.addActionListener(event -> {
         // Enter in the word field builds the word, whatever shape was showing.
         this.settingUp = true;
         this.shapeBox.setSelectedItem(Shapes.WORD);
         this.settingUp = false;
         this.fitWindow();
         this.show(MORPH_FRAMES);
      });
      this.createButton.addActionListener(event -> this.build());
      clearButton.addActionListener(event -> this.pad.clear());
      this.mirrorButton.addActionListener(event -> {
         this.pad.setMirror(!this.pad.mirror());
         this.showMirror();
         this.remember();
      });
      this.fitWindow();
      this.setVisible(true);
      this.settingUp = false;
      this.build();
   }

   private boolean drawing() {
      return Shapes.DRAW.equals(this.shapeBox.getSelectedItem());
   }

   private void showMirror() {
      this.mirrorButton.setText(this.pad.mirror() ? "Mirror: on" : "Mirror: off");
   }

   /** Sizes the window for the chosen shape (wider and taller with the drawing pad) and keeps it top center. */
   private void fitWindow() {
      boolean draw = this.drawing();
      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
      boolean beside = screen.width >= DRAW_WIDTH;
      int width = draw && beside ? DRAW_WIDTH : WIDTH;
      int height = !draw ? HEIGHT : beside ? DRAW_HEIGHT : DRAW_BELOW_HEIGHT;
      this.remove(this.padPanel);
      if (draw) {
         this.add(this.padPanel, beside ? BorderLayout.EAST : BorderLayout.SOUTH);
      }
      this.setBounds(Math.max(0, (screen.width - width) / 2), 0, width, height);
      this.validate();
      this.repaint();
   }

   /**
    * The windows of the chosen shape, centered in the room under this window, with the status line set; null
    * (and a message) when the size or word will not do. An empty drawing gives no windows.
    */
   private List<Rectangle> target() {
      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
      int top = this.getY() + this.getHeight();
      int maxSize = Math.max(2, Math.min(screen.width, screen.height - top));
      int size;
      try {
         size = Integer.parseInt(this.sizeField.getText().trim());
      } catch (NumberFormatException e) {
         size = -1;
      }
      boolean draw = this.drawing();
      if (draw && size > maxSize) {
         // The pad makes this window taller: a drawing shrinks to the room left rather than refusing.
         size = maxSize;
      }
      if (size < 2 || size > maxSize) {
         this.status.setText("Size: use 2 to " + maxSize);
         return null;
      }
      String shape = String.valueOf(this.shapeBox.getSelectedItem());
      if (Shapes.WORD.equals(shape) && Shapes.cleanWord(this.wordField.getText()).isEmpty()) {
         this.status.setText("Type a word to build");
         return null;
      }
      this.builtShape = shape;
      this.remember();
      List<Rectangle> pieces = Shapes.build(shape, size, this.wordField.getText(), this.pad.cells(),
         screen.width / 2, top + (screen.height - top) / 2, screen.width * 9 / 10);
      if (pieces.isEmpty()) {
         this.status.setText("Draw on the pad and the windows follow.");
      }
      return pieces;
   }

   /** Saves the current choices and drawing for next time. */
   private void remember() {
      this.saved.put("shape", String.valueOf(this.shapeBox.getSelectedItem()));
      this.saved.put("colors", String.valueOf(this.colorBox.getSelectedItem()));
      this.saved.put("size", this.sizeField.getText().trim());
      this.saved.put("word", this.wordField.getText());
      this.saved.put("drawing", this.pad.text());
      this.saved.put("mirror", this.pad.mirror() ? "1" : "0");
      this.saved.store();
   }

   /** Replaces the shape on screen with the chosen one, which then appears a few windows per frame. */
   private void build() {
      this.stopAll();
      this.clear();
      List<Rectangle> pieces = this.target();
      if (pieces == null || pieces.isEmpty()) {
         return;
      }
      this.pending.addAll(pieces);
      this.pendingColors.addAll(this.colors(pieces, this.builtShape));
      this.built = 0;
      this.status.setText("Building...");
      this.buildTimer.start();
   }

   /**
    * Turns the shape on screen into the chosen one over the given number of frames. With nothing on screen
    * (or while it is flying apart) the new shape is built from scratch instead.
    */
   private void show(int frames) {
      if (this.settingUp) {
         return;
      }
      if (this.scatterTimer.isRunning() || this.slices.isEmpty()) {
         this.build();
         return;
      }
      List<Rectangle> pieces = this.target();
      if (pieces == null) {
         return;
      }
      this.stopAll();
      this.pending.clear();
      this.pendingColors.clear();
      this.morph = new Morph(this.slices, pieces, this.colors(pieces, this.builtShape), frames,
         (bounds, color) -> new CircleSliceWindow(bounds, color, "", false, this::scatter));
      if (frames > DRAW_FRAMES) {
         this.status.setText("Changing into " + this.shapeName() + "...");
      }
      this.morphTimer.start();
   }

   private String shapeName() {
      return Shapes.DRAW.equals(this.builtShape) ? "your drawing" : "a " + this.builtShape.toLowerCase();
   }

   /** One frame of a morph; at the end the windows are numbered and layered as a freshly built shape. */
   private void morphStep() {
      if (!this.morph.step()) {
         return;
      }
      this.morphTimer.stop();
      this.morph = null;
      for (int i = 0; i < this.slices.size(); i++) {
         CircleSliceWindow slice = this.slices.get(i);
         slice.setTitle(String.valueOf(i + 1));
         slice.setAlwaysOnTop(this.onTop(i));
      }
      this.showBuilt();
   }

   /** The original kept every 10th slice of its circle always on top; the other shapes do not overlap. */
   private boolean onTop(int index) {
      return Shapes.CIRCLE.equals(this.builtShape) && (index + 1) % 10 == 0;
   }

   private void showBuilt() {
      if (this.slices.isEmpty()) {
         this.status.setText("Draw on the pad and the windows follow.");
         return;
      }
      String name = this.shapeName();
      this.status.setText(Character.toUpperCase(name.charAt(0)) + name.substring(1) + ", " + this.slices.size()
         + " windows. Click it to scatter.");
   }

   /** One frame of building: shows the next few windows. */
   private void buildStep() {
      int perFrame = Math.max(1, (this.pending.size() + BUILD_FRAMES - 1) / BUILD_FRAMES);
      for (int i = 0; i < perFrame && this.built < this.pending.size(); i++, this.built++) {
         this.slices.add(new CircleSliceWindow(this.pending.get(this.built), this.pendingColors.get(this.built),
            String.valueOf(this.built + 1), this.onTop(this.built), this::scatter));
      }
      if (this.built >= this.pending.size()) {
         this.buildTimer.stop();
         this.showBuilt();
      }
   }

   /** Colors along the shape: the circle's slices by order (they share one center), the others from left to right. */
   private List<Color> colors(List<Rectangle> pieces, String shape) {
      if (pieces.isEmpty()) {
         return Arrays.asList();
      }
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
      this.stopAll();
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

   /** Stops building, scattering and morphing; a morph's leftover windows close where they are. */
   private void stopAll() {
      this.buildTimer.stop();
      this.scatterTimer.stop();
      this.morphTimer.stop();
      if (this.morph != null) {
         this.morph.finish();
         this.morph = null;
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
