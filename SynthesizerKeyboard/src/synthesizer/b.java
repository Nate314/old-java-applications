package synthesizer;

import java.util.HashMap;
import java.util.Map;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

public final class b {
   private final Synthesizer a;
   private final MidiChannel[] b;
   private Map c = new HashMap();
   private String[] d = new String[]{"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
   private d e;

   public b(d var1) {
      this.e = var1;

      try {
         this.a = MidiSystem.getSynthesizer();
         this.b = this.a.getChannels();
         this.a.open();
      } catch (MidiUnavailableException var2) {
         throw new RuntimeException(var2);
      }

      for (int var3 = -36; var3 < 132; var3++) {
         this.c.put(this.d[Math.abs(var3) % 12] + var3 / 12, var3 + 12);
      }
   }

   public final void a(int var1, int var2) {
      this.b[0].programChange(var2);
   }

   public final void a(int var1, int var2, int var3, int var4, int var5, int var6) {
      byte var7 = 0;
      byte var14 = 0;
      byte var12 = 0;
      byte var11 = 10;
      short var9 = 150;
      this.b(var7, var1);
      if (var14 != 0) {
         var14 = var14;
         short var13 = var9;
         b var10 = this;
         this.b[0].setChannelPressure(var12);

         for (int var16 = var14; var16 > 0; var16 -= var14 / var13) {
            var10.b[0].setPitchBend(var10.b[0].getPitchBend() - var14 / var13);
            var10.a(1L);
         }
      } else {
         this.a((long)var9);
      }

      this.c(var7, var1);
      this.a((long)var11);
   }

   public final void b(int var1, int var2) {
      this.b[var1].noteOn(var2, 1000);
   }

   public final void c(int var1, int var2) {
      this.b[var1].noteOff(var2);
   }

   private void a(long var1) {
      long var3 = var1 + this.e.a;

      while (this.e.a < var3) {
         try {
            Thread.sleep(1L);
         } catch (InterruptedException var5) {
            var5.printStackTrace();
         }
      }
   }
}
