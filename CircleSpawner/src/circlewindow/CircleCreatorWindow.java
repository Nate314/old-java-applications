package circlewindow;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
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
 * on screen into it, and drawing on the pad (the Draw shape) reshapes them as you draw. Clicking any window
 * of the shape scatters it; dragging one carries the whole shape like a jelly, and it can be thrown. A drawing
 * with several frames plays as a flipbook: the windows glide from one frame
 * to the next, those of the parts that did not change staying where they are.
 */
public final class CircleCreatorWindow extends JFrame {
   private static final String BLUE = "Blue";
   private static final String RAINBOW = "Rainbow";
   private static final String SUNSET = "Sunset";
   static final String[] COLOR_NAMES = {RAINBOW, BLUE, SUNSET};
   private static final int WIDTH = 380;
   private static final int HEIGHT = 130;
   /** The window with the drawing pad beside its controls, or (on a narrow screen) under them. */
   private static final int DRAW_WIDTH = 620;
   private static final int DRAW_HEIGHT = 290;
   private static final int DRAW_BELOW_HEIGHT = 388;
   /** About how many frames building a shape takes, whatever its window count. */
   private static final int BUILD_FRAMES = 45;
   private static final int SCATTER_FRAMES = 50;
   /** Frames for one shape to turn into another, and for the drawing to follow the pen. */
   private static final int MORPH_FRAMES = 40;
   private static final int DRAW_FRAMES = 10;
   /** Frames for the Clock to turn into the next second, so it settles well before the one after. */
   private static final int TICK_FRAMES = 24;
   private static final int FRAME_MILLIS = 16;
   /** How often the Clock looks at the time. */
   private static final int TICK_MILLIS = 100;
   /** A flipbook frame every PLAY_MILLIS, the windows gliding into it over PLAY_FRAMES frames and then holding. */
   private static final int PLAY_MILLIS = 300;
   private static final int PLAY_FRAMES = 9;

   private final JComboBox<String> shapeBox = new JComboBox<>(Shapes.ALL);
   private final JTextField sizeField = new JTextField("200", 4);
   private final JComboBox<String> colorBox = new JComboBox<>(COLOR_NAMES);
   private final JTextField wordField = new JTextField("HELLO", 8);
   private final JButton createButton = new JButton("Build it");
   private final JButton spinButton = new JButton("Spin");
   private final JButton sendButton = new JButton("Send");
   /** Puts a link (or, on the desktop, a code) on the clipboard; the address of this page in the browser, else null. */
   private final Consumer<String> copy;
   private final String page;
   /** True when the app opened on a shape from a friend's link: nothing is saved, so the visitor's own stays. */
   private final boolean fromFriend;
   private final JLabel status = new JLabel(" ");
   private final DrawPad pad = new DrawPad(() -> {
      this.stopPlaying();
      this.show(DRAW_FRAMES);
   });
   private final JButton mirrorButton = new JButton();
   private final JButton playButton = new JButton("Play");
   private final JLabel frameLabel = new JLabel();
   private final JPanel padPanel = new JPanel(new BorderLayout());
   private final List<CircleSliceWindow> slices = new ArrayList<>();
   private final List<Rectangle> pending = new ArrayList<>();
   private final List<Color> pendingColors = new ArrayList<>();
   private final Random random = new Random();
   private final Timer buildTimer = new Timer(FRAME_MILLIS, event -> this.buildStep());
   private final Timer scatterTimer = new Timer(FRAME_MILLIS, event -> this.scatterStep());
   private final Timer morphTimer = new Timer(FRAME_MILLIS, event -> this.morphStep());
   private final Timer clockTimer = new Timer(TICK_MILLIS, event -> this.tick());
   private final Timer playTimer = new Timer(PLAY_MILLIS, event -> this.playStep());
   private final Timer jellyTimer = new Timer(FRAME_MILLIS, event -> this.jellyStep());
   private final Timer spinTimer = new Timer(FRAME_MILLIS, event -> this.spinStep());
   private Spin spin;
   /** When the spin began, in nanoTime milliseconds (so the web page's Slow-mo slows it too). */
   private long spinStart;
   /** How far a press must move to pick the shape up rather than click it. */
   private static final int DRAG_START = 5;
   /** Presses and drags on the shape's windows: a click scatters it, a drag picks it up (see Jelly). */
   private final MouseAdapter hand = new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
         CircleCreatorWindow.this.pressX = e.getXOnScreen();
         CircleCreatorWindow.this.pressY = e.getYOnScreen();
         CircleCreatorWindow.this.dragged = false;
      }

      @Override
      public void mouseDragged(MouseEvent e) {
         CircleCreatorWindow.this.drag(e.getXOnScreen(), e.getYOnScreen());
      }

      @Override
      public void mouseReleased(MouseEvent e) {
         CircleCreatorWindow.this.letGo();
      }
   };
   private Jelly jelly;
   private int pressX;
   private int pressY;
   /** True once the press has moved past DRAG_START (so letting go is not a click), and while the shape is held. */
   private boolean dragged;
   private boolean holding;
   /** How far the shape has been carried from its place under this window; Build it and scattering bring it back. */
   private int movedX;
   private int movedY;
   private final Saved saved = Saved.load();
   private Morph morph;
   private String builtShape = "";
   /** The time the Clock on screen spells (or is turning into). */
   private String shownTime = "";
   /** True while the Clock turns into the next second: no messages, nothing saved, windows stay in place. */
   private boolean ticking;
   private int built;
   private int scatterFrame;
   private boolean settingUp = true;

   /** sent: the shape of a friend's link, shown instead of the saved one, or null. */
   public CircleCreatorWindow(Consumer<String> copy, String page, Link sent) {
      this.copy = copy;
      this.page = page;
      this.fromFriend = sent != null;
      this.setTitle("Circle Creator");
      this.setDefaultCloseOperation(3);
      this.shapeBox.setSelectedItem(sent != null ? sent.shape : this.saved.get("shape", Shapes.CIRCLE));
      this.colorBox.setSelectedItem(sent != null ? sent.colors : this.saved.get("colors", RAINBOW));
      this.sizeField.setText(this.saved.get("size", "200"));
      this.wordField.setText(sent != null && sent.word != null ? sent.word : this.saved.get("word", "HELLO"));
      String drawing = sent != null && sent.drawing != null ? sent.drawing : this.saved.get("drawing", "");
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
      panel.add(this.spinButton);
      panel.add(this.sendButton);
      panel.add(this.status);

      JButton clearButton = new JButton("Clear");
      JButton deleteButton = new JButton("Delete");
      JButton previousButton = new JButton("<");
      JButton nextButton = new JButton(">");
      JButton addButton = new JButton("+");
      previousButton.setToolTipText("Previous frame");
      nextButton.setToolTipText("Next frame");
      addButton.setToolTipText("Add a frame: a copy of this one, to change");
      deleteButton.setToolTipText("Delete this frame");
      JPanel frameRow = new JPanel();
      frameRow.add(previousButton);
      frameRow.add(this.frameLabel);
      frameRow.add(nextButton);
      frameRow.add(addButton);
      frameRow.add(this.playButton);
      JPanel editRow = new JPanel();
      editRow.add(clearButton);
      editRow.add(deleteButton);
      editRow.add(this.mirrorButton);
      JPanel padButtons = new JPanel(new BorderLayout());
      padButtons.add(frameRow, BorderLayout.NORTH);
      padButtons.add(editRow, BorderLayout.SOUTH);
      JPanel padHolder = new JPanel();
      padHolder.add(this.pad);
      this.padPanel.add(padHolder, BorderLayout.CENTER);
      this.padPanel.add(padButtons, BorderLayout.SOUTH);
      this.add(panel, BorderLayout.CENTER);
      this.showMirror();
      this.showFrame();

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
      this.createButton.addActionListener(event -> {
         this.movedX = 0;
         this.movedY = 0;
         this.build();
      });
      this.spinButton.setToolTipText("Turn the shape round like a coin");
      this.spinButton.addActionListener(event -> {
         if (this.spin != null) {
            this.endSpin();
            this.showBuilt();
         } else {
            this.startSpin();
         }
      });
      this.sendButton.setToolTipText("Copy a link that builds this shape on a friend's screen");
      this.sendButton.addActionListener(event -> this.send());
      clearButton.addActionListener(event -> this.pad.clear());
      previousButton.addActionListener(event -> this.turnTo(this.pad.frame() - 1));
      nextButton.addActionListener(event -> this.turnTo(this.pad.frame() + 1));
      addButton.addActionListener(event -> {
         this.stopPlaying();
         if (this.pad.addFrame()) {
            this.showFrame();
            this.remember();
            this.status.setText("Frame " + (this.pad.frame() + 1) + ": a copy to change.");
         } else {
            this.status.setText("A flipbook holds " + DrawPad.MAX_FRAMES + " frames.");
         }
      });
      deleteButton.addActionListener(event -> {
         this.stopPlaying();
         if (this.pad.deleteFrame()) {
            this.showFrame();
            this.show(DRAW_FRAMES);
         }
      });
      this.playButton.addActionListener(event -> {
         if (this.playTimer.isRunning()) {
            this.stopPlaying();
         } else if (this.pad.frameCount() < 2) {
            this.status.setText("Press + to add a frame, then Play.");
         } else {
            this.playTimer.start();
            this.playButton.setText("Stop");
            this.showBuilt();
         }
      });
      this.mirrorButton.addActionListener(event -> {
         this.pad.setMirror(!this.pad.mirror());
         this.showMirror();
         this.remember();
      });
      this.fitWindow();
      this.setVisible(true);
      this.settingUp = false;
      this.build();
      this.clockTimer.start();
      if (this.fromFriend && this.drawing() && this.pad.frameCount() > 1) {
         this.playButton.doClick();   // a friend's flipbook plays as it opens
      }
   }

   /** The shape chosen, as a link to this page (or a code on the desktop) on the clipboard. */
   private void send() {
      String shape = String.valueOf(this.shapeBox.getSelectedItem());
      if (Shapes.WORD.equals(shape) && Shapes.cleanWord(this.wordField.getText()).isEmpty()) {
         this.status.setText("Type a word to send");
         return;
      }
      String code = Link.code(shape, String.valueOf(this.colorBox.getSelectedItem()), this.wordField.getText(),
         this.pad.text());
      String text = this.page == null ? code : this.page + "?shape=" + code;
      try {
         this.copy.accept(text);
         this.status.setText(this.page == null ? "Code " + code + " copied." : "Link copied: send it to a friend.");
      } catch (RuntimeException e) {
         this.status.setText("Send this: " + text);   // headless, or the clipboard is busy
      }
   }

   /**
    * Turns the Clock on screen into the time now when the second has changed, once whatever it is doing is
    * done (so a slow screen skips a second rather than never settling).
    */
   private void tick() {
      if (!Shapes.CLOCK.equals(this.builtShape) || !Shapes.CLOCK.equals(this.shapeBox.getSelectedItem())
            || this.slices.isEmpty() || this.buildTimer.isRunning() || this.scatterTimer.isRunning()
            || this.morphTimer.isRunning() || this.jellyTimer.isRunning() || this.spin != null
            || Shapes.clockText(new Date()).equals(this.shownTime)) {
         return;
      }
      this.ticking = true;
      try {
         this.show(TICK_FRAMES);
      } finally {
         this.ticking = false;
      }
   }

   /** Shows another frame of the drawing on the pad and on screen. */
   private void turnTo(int index) {
      this.stopPlaying();
      this.pad.goTo(index);
      this.showFrame();
      this.show(DRAW_FRAMES);
   }

   private void showFrame() {
      this.frameLabel.setText((this.pad.frame() + 1) + " / " + this.pad.frameCount());
   }

   private void stopPlaying() {
      if (this.playTimer.isRunning()) {
         this.playTimer.stop();
         this.playButton.setText("Play");
         this.showBuilt();
      }
   }

   /**
    * Turns the windows into the next frame of the flipbook, pairing them by where they are on screen as a Clock
    * tick does, so the parts the frames share stay still. A frame still gliding (or a shape growing in) waits.
    */
   private void playStep() {
      if (!this.drawing()) {
         this.stopPlaying();
         return;
      }
      if (this.buildTimer.isRunning() || this.scatterTimer.isRunning() || this.morphTimer.isRunning()
            || this.jellyTimer.isRunning()) {
         return;
      }
      this.pad.goTo(this.pad.frame() + 1);
      this.showFrame();
      this.ticking = true;
      try {
         this.show(PLAY_FRAMES);
      } finally {
         this.ticking = false;
      }
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
      String word = this.wordField.getText();
      if (Shapes.CLOCK.equals(shape)) {
         word = Shapes.clockText(new Date());
         this.shownTime = word;
      }
      if (!this.ticking) {
         this.remember();
      }
      List<Rectangle> pieces = Shapes.build(shape, size, word, this.pad.cells(),
         screen.width / 2, top + (screen.height - top) / 2, screen.width * 9 / 10);
      this.carry(pieces, screen);
      if (pieces.isEmpty()) {
         this.status.setText("Draw on the pad and the windows follow.");
      }
      return pieces;
   }

   /** Saves the current choices and drawing for next time. */
   private void remember() {
      if (this.fromFriend) {
         return;
      }
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
      this.morph = new Morph(this.slices, pieces, this.colors(pieces, this.builtShape), frames, this.ticking,
         (bounds, color) -> new CircleSliceWindow(bounds, color, "", false, this.hand));
      if (frames > DRAW_FRAMES && !this.ticking) {
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
      if (this.playTimer.isRunning()) {
         this.status.setText(this.fromFriend ? "Playing a friend's flipbook. Send one back!"
            : "Playing your flipbook, " + this.slices.size() + " windows. Draw to stop.");
         return;
      }
      if (this.spin != null) {
         this.status.setText("Spinning " + this.slices.size() + " windows. Click it to scatter.");
         return;
      }
      String name = this.shapeName();
      if (this.fromFriend) {
         this.status.setText("From a friend: " + (Shapes.DRAW.equals(this.builtShape) ? "a drawing" : name)
            + ". Send one back!");
         return;
      }
      this.status.setText(Character.toUpperCase(name.charAt(0)) + name.substring(1) + ", " + this.slices.size()
         + " windows. Drag it, or click to scatter.");
   }

   /** One frame of building: shows the next few windows. */
   private void buildStep() {
      int perFrame = Math.max(1, (this.pending.size() + BUILD_FRAMES - 1) / BUILD_FRAMES);
      for (int i = 0; i < perFrame && this.built < this.pending.size(); i++, this.built++) {
         this.slices.add(new CircleSliceWindow(this.pending.get(this.built), this.pendingColors.get(this.built),
            String.valueOf(this.built + 1), this.onTop(this.built), this.hand));
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
      this.stopPlaying();
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
      this.movedX = 0;
      this.movedY = 0;
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

   /**
    * Moves the pieces of a new shape to where the shape on screen was carried, kept on the screen if they fit,
    * so morphs, Clock ticks and the flipbook stay where the shape was dropped.
    */
   private void carry(List<Rectangle> pieces, Dimension screen) {
      if (pieces.isEmpty() || this.movedX == 0 && this.movedY == 0) {
         return;
      }
      Rectangle area = new Rectangle(pieces.get(0));
      for (Rectangle r : pieces) {
         area.add(r);
      }
      int dx = clamp(this.movedX, -area.x, screen.width - area.x - area.width);
      int dy = clamp(this.movedY, -area.y, screen.height - area.y - area.height);
      for (Rectangle r : pieces) {
         r.x += dx;
         r.y += dy;
      }
   }

   /** value kept between low and high, or left alone when there is no room between them. */
   private static int clamp(int value, int low, int high) {
      return low > high ? value : Math.max(low, Math.min(high, value));
   }

   /** The pointer pressed on the shape has moved: past DRAG_START it picks the whole shape up. */
   private void drag(int screenX, int screenY) {
      if (!this.dragged) {
         if (Math.hypot(screenX - this.pressX, screenY - this.pressY) < DRAG_START) {
            return;
         }
         this.dragged = true;
         if (this.slices.isEmpty() || this.buildTimer.isRunning() || this.scatterTimer.isRunning()) {
            return;
         }
         // Also catches a shape still gliding from a throw: it is put down where it is, then picked up again.
         this.stopPlaying();
         this.stopAll();
         List<Rectangle> bounds = new ArrayList<>();
         for (CircleSliceWindow slice : this.slices) {
            bounds.add(slice.getBounds());
         }
         Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
         this.jelly = new Jelly(bounds, this.pressX, this.pressY, screen.width, screen.height,
            Boolean.TRUE.equals(Toolkit.getDefaultToolkit().getDesktopProperty("oldjava.reducedMotion")));
         this.holding = true;
         this.status.setText("Drop it anywhere, or throw it.");
         this.jellyTimer.start();
      }
      if (this.holding) {
         this.jelly.hold(screenX - this.pressX, screenY - this.pressY);
      }
   }

   /** The pointer let go of the shape: a click scatters it, a drag drops (or throws) it. */
   private void letGo() {
      if (this.holding) {
         this.holding = false;
         this.jelly.letGo();
      } else if (!this.dragged) {
         this.scatter();
      }
   }

   /** One frame of the shape being carried or thrown; once it is at rest it stays where it landed. */
   private void jellyStep() {
      boolean resting = this.jelly.step();
      this.placeCarried();
      if (resting) {
         this.endJelly();
         this.showBuilt();
      }
   }

   private void placeCarried() {
      for (int i = 0; i < this.slices.size(); i++) {
         CircleSliceWindow slice = this.slices.get(i);
         slice.x = this.jelly.x[i];
         slice.y = this.jelly.y[i];
         slice.setLocation((int)Math.round(slice.x), (int)Math.round(slice.y));
      }
   }

   /** Puts the carried shape down where it has got to, every window in its place in the shape. */
   private void endJelly() {
      if (this.jelly == null) {
         return;
      }
      this.jellyTimer.stop();
      this.holding = false;
      this.jelly.settle();
      this.placeCarried();
      this.movedX += this.jelly.movedX();
      this.movedY += this.jelly.movedY();
      this.jelly = null;
   }

   /** Turns the shape round like a coin until Spin is pressed again or anything else changes it. */
   private void startSpin() {
      if (this.slices.isEmpty() || this.buildTimer.isRunning() || this.scatterTimer.isRunning()) {
         return;
      }
      if (Boolean.TRUE.equals(Toolkit.getDefaultToolkit().getDesktopProperty("oldjava.reducedMotion"))) {
         this.status.setText("Spin is off while reduced motion is on.");
         return;
      }
      this.stopPlaying();
      this.stopAll();
      List<Rectangle> home = new ArrayList<>();
      List<Color> colors = new ArrayList<>();
      for (CircleSliceWindow slice : this.slices) {
         home.add(slice.getBounds());
         colors.add(slice.color());
      }
      this.spin = new Spin(home, colors);
      this.spinStart = System.nanoTime() / 1_000_000;
      this.spinButton.setText("Stop");
      this.showBuilt();
      this.spinTimer.start();
   }

   private void spinStep() {
      double angle = Spin.angle(System.nanoTime() / 1_000_000 - this.spinStart);
      for (int i = 0; i < this.slices.size(); i++) {
         this.slices.get(i).place(this.spin.at(i, angle), this.spin.colorAt(i, angle));
      }
   }

   /** Stops the spin with every window back flat in its place and color. */
   private void endSpin() {
      if (this.spin == null) {
         return;
      }
      this.spinTimer.stop();
      for (int i = 0; i < this.slices.size(); i++) {
         this.slices.get(i).place(this.spin.home(i), this.spin.color(i));
      }
      this.spin = null;
      this.spinButton.setText("Spin");
   }

   /** Stops building, scattering, morphing, carrying and spinning; a morph's leftover windows close where they are. */
   private void stopAll() {
      this.endSpin();
      this.endJelly();
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
