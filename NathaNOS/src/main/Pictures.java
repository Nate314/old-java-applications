package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * The pictures painted in Paint, kept in ~/.nathanos-pictures.txt, one line each: "picture=Picture 2:" and the
 * pixels as Picture.encode() writes them. Each one shows as an icon on the desktop. A damaged line is skipped.
 */
final class Pictures {
   static final File FILE = new File(System.getProperty("user.home", "."), ".nathanos-pictures.txt");
   /** As many as fit beside the apps on a small desktop. */
   static final int MAX = 12;
   private final File file;
   private final List<Picture> all = new ArrayList<>();
   private final List<Runnable> listeners = new ArrayList<>();

   private Pictures(File file) {
      this.file = file;
   }

   static Pictures load(File file) {
      Pictures pictures = new Pictures(file);
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"))) {
         for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            int colon = line.lastIndexOf(':');
            if (!line.startsWith("picture=") || colon < 0) {
               continue;
            }
            String name = line.substring("picture=".length(), colon).trim();
            byte[] pixels = Picture.decode(line.substring(colon + 1).trim());
            if (!name.isEmpty() && pixels != null && pictures.find(name) == null && pictures.all.size() < MAX) {
               pictures.all.add(new Picture(name, pixels));
            }
         }
      } catch (IOException | RuntimeException missing) {
         // No pictures yet (or an unreadable file): start with none.
      }
      return pictures;
   }

   /** The pictures in the order they were first saved. */
   List<Picture> all() {
      return new ArrayList<>(this.all);
   }

   Picture find(String name) {
      for (Picture picture : this.all) {
         if (picture.name.equals(name)) {
            return picture;
         }
      }
      return null;
   }

   boolean contains(Picture picture) {
      return this.all.contains(picture);
   }

   boolean isFull() {
      return this.all.size() >= MAX;
   }

   /** "Picture 1", or the first "Picture n" no picture is called. */
   String nextName() {
      for (int n = 1; ; n++) {
         if (this.find("Picture " + n) == null) {
            return "Picture " + n;
         }
      }
   }

   /** Saves the picture (adding it if it is new); false when it is new and there is no room for another. */
   boolean put(Picture picture) {
      if (!this.all.contains(picture)) {
         if (this.isFull()) {
            return false;
         }
         this.all.add(picture);
      }
      this.save();
      this.changed();
      return true;
   }

   void remove(Picture picture) {
      if (this.all.remove(picture)) {
         this.save();
         this.changed();
      }
   }

   /** Runs after every put or remove (the desktop icons and the wallpaper follow the pictures). */
   void onChange(Runnable listener) {
      this.listeners.add(listener);
   }

   private void changed() {
      for (Runnable listener : new ArrayList<>(this.listeners)) {
         listener.run();
      }
   }

   private void save() {
      try (Writer writer = new OutputStreamWriter(new FileOutputStream(this.file), "utf-8")) {
         for (Picture picture : this.all) {
            writer.write("picture=" + picture.name + ":" + picture.encode() + "\n");
         }
      } catch (IOException | RuntimeException exception) {
         System.err.println("Could not save " + this.file + ": " + exception);
      }
   }
}
