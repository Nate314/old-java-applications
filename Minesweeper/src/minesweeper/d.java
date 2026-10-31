package minesweeper;

import java.util.HashMap;
import java.util.Map;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

public final class d {
   private final Synthesizer a;
   private final MidiChannel[] b;
   private Map c = new HashMap();
   private String[] d = new String[]{"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

   public d() {
      try {
         this.a = MidiSystem.getSynthesizer();
         this.b = this.a.getChannels();
         this.a.open();
      } catch (MidiUnavailableException var2) {
         throw new RuntimeException(var2);
      }

      for (int var1 = 0; var1 < 100; var1++) {
         this.c.put(this.d[var1 % 12] + var1 / 12, var1 + 12);
      }
   }

   public final void a(int var1, int var2, int var3, int var4, int var5) {
      byte var6 = 0;
      byte var9 = 0;
      byte var8 = 2;
      byte var7 = 20;
      this.b[0].noteOn(var1, 1000);
      if (var6 != 0) {
         this.a(var7, var6, var9);
      } else {
         a((long)var7);
      }

      a((long)var7);
      this.b[0].noteOff(var1);
      a((long)var8);
   }

   public final void a(int[] var1, int var2, int var3, int var4, int var5) {
      short var6 = 4096;
      byte var12 = 127;
      byte var11 = 10;
      short var10 = 750;
      int[] var9 = var1;
      d var8 = this;

      for (int var7 = 0; var7 < var9.length; var7++) {
         var8.b[0].noteOn(var9[var7], 1000);
      }

      if (var6 != 0) {
         var8.a(var10, var6, var12);
      } else {
         a((long)var10);
      }

      for (int var13 = 0; var13 < var9.length; var13++) {
         var8.b[0].noteOff(var9[var13]);
      }

      a((long)var11);
   }

   private void a(int var1, int var2, int var3) {
      this.b[0].setChannelPressure(var3);
      var3 = var2;

      while (var3 > 0) {
         this.b[0].setPitchBend(this.b[0].getPitchBend() - var2 / var1);
         a(1L);
         var3 -= var2 / var1;
      }
   }

   private static void a(long var0) {
      try {
         long var2 = System.currentTimeMillis() + var0;

         while (true) {
            try {
               Thread.sleep(var2 - System.currentTimeMillis());
            } catch (InterruptedException var4) {
            }
         }
      } catch (IllegalArgumentException var5) {
      }
   }
}
