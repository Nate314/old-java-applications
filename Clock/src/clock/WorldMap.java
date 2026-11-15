package clock;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

/**
 * The World tab's map: the whole world flat (longitude across, latitude up), lit where the sun is up and dark where it
 * is night, with a soft band of dusk between, at the time shown. The sun's place comes from the date (the usual
 * low-precision formulas, good to a fraction of a degree); the land is Natural Earth's 1:50m coastline (public domain),
 * sampled on a one-degree grid (a cell is land when any of four points in it is, so small islands such as Oahu show;
 * south of 78 degrees south is all land). The picture is worked out on that grid, once per minute of the time shown,
 * and scaled.
 */
final class WorldMap {
   static final int WIDTH = 360;
   static final int HEIGHT = 180;
   /** The sun this far below the horizon (degrees) is full night; between that and 0 is dusk. */
   static final double DUSK = 12;

   private static final Color SEA = new Color(120, 178, 222);
   private static final Color LAND = new Color(150, 186, 108);
   private static final Color SEA_NIGHT = new Color(14, 22, 58);
   private static final Color LAND_NIGHT = new Color(38, 48, 64);
   private static final Color SUN = new Color(255, 214, 90);
   private static final long MINUTE = Countdown.MINUTE;
   /** 1 January 2000 at noon UTC, the epoch of the sun's formulas. */
   private static final long J2000 = 946_728_000_000L;

   /** Four rows of one degree per string, north to south, split by ";": each row's runs of sea and land, sea first. */
   private static final String[] LAND_RUNS = {
      "360;360;360;360",
      "360;360;137,1,1,15,206;94,3,1,21,5,2,1,1,1,3,2,24,202",
      "86,30,3,49,67,4,36,1,84;84,28,1,53,33,2,1,1,1,2,20,16,1,2,27,6,82;74,3,7,25,5,48,28,10,1,6,65,1,1,6,1,2,77;76,9,1,20,1,52,1,1,29,1,1,11,76,6,1,1,73",
      "60,6,1,3,4,2,1,2,5,3,2,1,2,10,7,54,31,5,2,4,64,1,11,5,74;57,10,3,2,4,26,8,50,1,1,34,1,44,1,1,6,26,2,2,15,35,1,30;61,21,2,17,20,41,75,8,23,26,21,1,1,8,1,4,30;55,5,1,2,3,4,15,2,1,12,23,38,74,5,26,27,25,3,39",
      "55,11,6,1,1,2,3,4,1,6,1,13,20,36,73,5,12,2,8,39,4,6,12,3,36;54,21,2,9,1,1,2,15,19,34,74,4,12,5,2,1,2,52,9,5,1,4,31;2,21,1,62,1,18,1,1,2,138,1,65,4,1,1,6,1,3,2,28,1;18,14,20,1,9,17,4,6,2,21,13,34,43,9,23,6,7,6,1,86,20",
      "16,26,2,17,2,16,2,8,1,1,3,20,11,32,40,19,12,2,9,6,2,113;1,13,51,17,1,4,2,1,1,4,2,10,4,18,21,48,19,21,1,2,6,2,4,2,2,1,83,1,18;4,12,49,3,2,3,4,2,1,1,1,1,1,1,7,2,4,10,7,14,17,50,24,4,1,4,23,3,105;0,1,4,1,2,11,53,1,21,13,5,2,3,10,15,1,1,50,19,4,3,5,24,1,1,2,107",
      "0,4,3,7,76,1,1,2,1,12,5,3,1,13,11,20,5,28,8,5,8,7,139;5,2,6,5,1,80,3,13,13,12,16,10,24,11,3,155,1;9,3,5,73,3,3,1,3,1,2,4,9,12,12,19,4,25,13,1,157,1;14,74,8,2,2,1,1,5,2,6,14,9,35,1,11,13,3,159",
      "14,73,13,1,1,7,21,8,47,13,3,137,2,3,1,10,6;13,73,16,10,3,1,16,1,1,4,40,1,6,15,1,135,4,1,1,10,8;18,9,1,2,9,46,17,9,3,3,68,14,4,132,5,4,16;18,1,1,8,14,45,14,16,56,4,8,5,1,6,5,120,16,6,16",
      "21,7,16,44,15,15,55,5,11,8,1,1,2,2,1,116,17,6,17;19,4,22,47,8,2,1,16,55,4,10,3,1,5,4,118,17,8,16;17,3,26,51,5,19,51,2,1,4,9,7,6,116,18,7,18;15,2,30,1,2,48,2,22,48,5,1,4,8,5,3,124,2,1,12,7,5,1,12",
      "11,1,1,1,33,1,1,49,3,23,46,4,1,6,4,138,13,4,20;5,1,42,1,1,48,1,1,1,24,44,5,1,7,2,140,12,3,21;2,2,48,72,46,2,3,7,1,138,1,2,12,2,22;51,70,1,2,50,5,2,140,1,2,11,2,23",
      "53,60,3,2,4,5,51,143,1,3,35;55,61,5,6,48,145,2,1,37;55,60,5,8,48,144,2,1,37;56,64,6,1,51,39,1,12,3,86,3,2,36",
      "56,64,59,31,3,15,3,87,3,2,5,2,30;56,57,1,3,61,15,2,14,4,2,2,10,3,87,4,3,1,1,1,1,32;55,55,4,1,56,17,2,4,2,13,10,9,3,85,5,5,34;55,54,62,12,5,7,3,10,13,7,4,80,1,1,6,4,36",
      "56,54,61,12,5,2,2,5,2,10,2,6,4,8,3,1,1,76,10,2,38;55,52,64,10,7,2,4,11,1,24,2,69,1,8,10,2,38;56,50,64,10,2,2,4,2,6,1,2,5,2,24,3,67,1,7,12,2,38;56,49,47,1,17,10,15,2,4,4,1,23,5,64,7,4,10,3,38",
      "57,48,49,1,16,8,10,1,2,3,5,3,3,25,3,65,1,3,3,4,8,3,39;58,46,69,3,1,1,2,11,3,1,12,95,4,4,3,1,2,5,39;58,47,69,3,1,13,12,4,6,1,2,84,6,4,2,9,39;59,45,69,19,20,2,1,86,5,1,2,1,1,8,41",
      "62,39,70,20,24,86,5,1,2,7,44;62,38,63,1,6,26,4,3,12,87,6,1,1,2,48;63,36,71,27,3,8,2,3,1,88,7,3,48;64,2,1,32,71,132,58",
      "64,22,1,4,3,1,1,3,70,60,1,72,58;64,3,1,17,12,3,62,2,4,61,2,71,7,1,50;65,3,1,14,14,3,66,48,1,15,2,69,59;67,2,1,13,15,2,65,49,2,14,3,3,1,63,7,2,51",
      "68,1,1,13,15,2,65,50,2,15,3,65,60;68,2,2,11,18,2,61,52,1,15,2,3,10,52,1,2,58;69,2,2,10,81,52,2,21,9,50,2,2,58;74,8,13,7,3,1,57,54,2,21,9,46,5,1,59",
      "74,9,7,3,4,1,2,4,59,54,2,21,9,21,2,20,68;23,1,50,9,6,4,9,4,57,54,2,20,11,1,2,14,5,15,3,1,69;24,1,49,10,5,4,13,5,52,55,3,17,14,13,8,13,2,3,69;76,17,8,3,2,6,1,2,49,55,2,16,16,12,9,12,2,3,9,3,57",
      "78,14,23,1,48,55,3,14,17,11,10,13,13,3,57;80,12,26,1,44,56,3,12,19,10,11,14,12,3,57;83,1,2,10,60,1,6,57,2,10,21,8,16,12,11,2,58;87,10,65,60,1,7,24,6,18,11,11,3,57",
      "90,7,21,1,44,65,26,7,17,12,10,5,55;92,5,11,1,54,62,30,5,12,1,5,2,1,9,11,1,1,3,54;93,4,9,6,52,60,4,3,24,5,12,1,5,2,3,6,13,4,54;94,3,7,10,1,4,46,66,25,4,18,2,3,5,11,1,2,2,56",
      "94,4,2,2,2,4,1,10,47,65,25,5,17,2,5,2,11,1,3,4,54;96,25,46,64,25,2,2,2,16,3,16,1,5,4,53;98,2,1,21,45,63,30,2,17,2,16,1,4,5,53;102,21,45,61,31,2,18,2,14,2,6,2,54",
      "102,26,42,10,5,44,46,2,3,3,12,5,5,1,54;102,27,42,3,11,43,47,4,1,4,10,5,61;102,27,59,40,48,4,1,3,4,1,4,5,62;101,29,60,37,48,1,1,7,7,7,10,1,51",
      "22,1,78,29,59,36,52,7,5,10,2,1,2,1,2,2,51;100,31,58,35,53,1,1,5,5,9,2,5,2,2,51;88,2,9,34,56,34,57,4,5,9,1,1,3,1,3,2,2,3,1,1,44;99,37,53,33,56,1,1,6,4,7,2,4,1,1,5,4,2,3,41",
      "99,39,51,32,58,1,1,7,2,7,2,3,10,3,1,6,38;99,43,48,30,61,5,6,5,2,4,3,1,1,3,1,12,7,1,28;98,45,48,29,62,4,13,4,12,11,5,2,27;99,46,47,28,64,2,13,2,1,1,9,1,1,1,2,10,1,4,2,1,25",
      "99,46,47,28,65,4,1,2,22,1,3,10,1,2,4,1,24;100,45,48,27,66,9,11,1,4,1,6,10,8,3,21;101,44,48,27,71,8,1,2,1,4,10,2,1,4,2,3,8,1,2,1,19;101,44,48,27,79,2,2,3,16,1,4,4,9,2,18",
      "102,42,49,28,82,1,25,2,11,1,17;102,41,51,27,2,1,86,4,8,1,37;103,39,51,28,8,1,80,7,4,3,36;7,1,95,38,51,29,7,2,80,7,4,3,36",
      "104,37,51,29,6,3,75,3,1,8,4,4,35;105,36,51,29,5,5,73,13,4,5,20,1,13;106,35,51,28,4,6,72,17,2,5,22,1,9,2;30,1,77,33,50,27,6,6,72,24,31,2,1",
      "109,32,51,25,7,6,71,26,33;109,32,51,24,8,5,71,28,32;110,30,53,22,9,5,8,1,58,33,15,1,2,1,12;110,29,54,23,7,6,6,1,59,35,15,2,13",
      "109,30,55,22,7,5,65,38,15,1,13;109,27,58,22,7,5,65,39,28;109,24,61,22,7,5,65,41,26;109,23,63,19,10,2,68,40,26",
      "109,23,63,18,80,40,27;109,23,63,18,81,40,26;108,24,64,17,81,40,26;108,23,66,15,83,39,26",
      "108,22,67,14,84,38,27;108,21,69,12,85,38,27;108,20,70,11,86,13,5,20,27;108,20,70,10,87,9,10,18,28",
      "108,19,72,3,93,5,15,16,29;107,16,193,15,22,2,5;107,17,195,11,24,2,4;106,17,197,10,24,5,1",
      "106,16,199,7,26,5,1;106,12,236,3,3;106,12,206,1,30,2,3;106,9,209,5,22,5,4",
      "106,11,208,3,23,3,6;3,1,101,10,211,1,22,4,7;105,10,232,5,8;106,7,234,4,9",
      "105,8,234,3,10;105,9,246;105,9,246;104,9,136,2,109",
      "104,8,248;105,6,9,2,238;105,7,8,1,239;106,7,247",
      "108,8,27,1,216;111,2,247;360;360",
      "360;360;360;360",
      "360;119,6,235;116,5,1,1,237;115,3,242",
      "112,7,109,1,1,7,33,19,1,6,7,4,2,12,22,1,16;111,4,110,21,15,66,33;113,4,2,1,93,1,6,30,8,77,25;104,1,3,10,78,1,15,6,1,31,6,84,20",
      "105,2,2,9,56,1,1,2,3,1,2,15,2,4,1,1,1,41,3,95,13;104,15,49,81,2,100,9;77,1,1,6,3,1,2,1,16,12,45,1,1,183,10;53,1,2,3,18,26,1,16,39,1,4,183,13",
      "44,2,6,4,1,10,1,2,8,40,41,1,2,183,15;35,81,40,187,17;29,83,37,194,17;21,86,38,199,2,1,13",
      "0,360;0,360;0,360;0,360",
      "0,360;0,360;0,360;0,360",
      "0,360;0,360;0,360;0,360"
   };
   private static boolean[][] land;

   private BufferedImage image;
   private long imageMinute = Long.MIN_VALUE;

   /** Whether the one-degree cell holding this latitude and longitude is land. */
   static boolean land(double latitude, double longitude) {
      if (land == null) {
         boolean[][] grid = new boolean[HEIGHT][WIDTH];
         int row = 0;
         for (String rows : LAND_RUNS) {
            for (String runs : rows.split(";")) {
               int x = 0;
               boolean isLand = false;
               for (String run : runs.split(",")) {
                  int end = x + Integer.parseInt(run);
                  for (; x < end; x++) {
                     grid[row][x] = isLand;
                  }
                  isLand = !isLand;
               }
               row++;
            }
         }
         land = grid;
      }
      int row = Math.max(0, Math.min(HEIGHT - 1, (int) Math.floor(90 - latitude)));
      int column = Math.floorMod((int) Math.floor(longitude + 180), WIDTH);
      return land[row][column];
   }

   /** Where the sun is overhead at the instant {@code utc}: latitude and longitude in degrees (east positive). */
   static double[] sun(long utc) {
      double n = (utc - J2000) / (double) Zone.DAY;
      double meanLongitude = 280.460 + 0.9856474 * n;
      double anomaly = Math.toRadians(357.528 + 0.9856003 * n);
      double ecliptic = Math.toRadians(meanLongitude + 1.915 * Math.sin(anomaly) + 0.020 * Math.sin(2 * anomaly));
      double obliquity = Math.toRadians(23.439 - 0.0000004 * n);
      double declination = Math.asin(Math.sin(obliquity) * Math.sin(ecliptic));
      double rightAscension = Math.toDegrees(Math.atan2(Math.cos(obliquity) * Math.sin(ecliptic), Math.cos(ecliptic)));
      double siderealDegrees = 280.46061837 + 360.98564736629 * n;
      return new double[]{Math.toDegrees(declination), wrap(rightAscension - siderealDegrees)};
   }

   /** How high the sun stands over the horizon at a place, in degrees (below it is negative). */
   static double altitude(double latitude, double longitude, double[] sun) {
      double lat = Math.toRadians(latitude);
      double dec = Math.toRadians(sun[0]);
      double sin = Math.sin(lat) * Math.sin(dec) + Math.cos(lat) * Math.cos(dec) * Math.cos(Math.toRadians(longitude - sun[1]));
      return Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, sin))));
   }

   /** 0 in daylight, 1 at night, between in the dusk. */
   static double dark(double altitude) {
      return Math.max(0, Math.min(1, -altitude / DUSK));
   }

   /** The map at the instant {@code t} in {@code box} (twice as wide as high), with a dot per city. */
   void paint(Graphics2D g, Rectangle box, List<Zone> zones, Color[] colors, long t) {
      double[] sun = sun(t);
      g.drawImage(this.image(t, sun), box.x, box.y, box.width, box.height, null);
      g.setColor(Ui.INK);
      g.setStroke(new BasicStroke(1));
      g.drawRect(box.x, box.y, box.width - 1, box.height - 1);

      // The sun where it is overhead: a disc with short rays.
      int sx = x(box, sun[1]);
      int sy = y(box, sun[0]);
      int radius = Math.max(3, box.height / 26);
      g.setColor(SUN);
      g.setStroke(new BasicStroke(Math.max(1.5f, radius / 3f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      for (int ray = 0; ray < 8; ray++) {
         double a = ray * Math.PI / 4;
         g.drawLine(sx + (int) Math.round(radius * 1.6 * Math.cos(a)), sy + (int) Math.round(radius * 1.6 * Math.sin(a)),
               sx + (int) Math.round(radius * 2.3 * Math.cos(a)), sy + (int) Math.round(radius * 2.3 * Math.sin(a)));
      }
      g.fillOval(sx - radius, sy - radius, 2 * radius, 2 * radius);

      // A dot per city in its hand's color, named beside it where the name has room. Here has no place on the map.
      Font font = new Font(Ui.LABELS, Font.BOLD, Math.max(8, box.height / 13));
      FontMetrics metrics = g.getFontMetrics(font);
      int dot = Math.max(3, box.height / 40);
      List<Rectangle> taken = new ArrayList<>();
      for (int i = 1; i < zones.size(); i++) {
         Zone zone = zones.get(i);
         taken.add(new Rectangle(x(box, zone.longitude) - dot, y(box, zone.latitude) - dot, 2 * dot, 2 * dot));
      }
      for (int i = 1; i < zones.size(); i++) {
         Zone zone = zones.get(i);
         Color color = colors[i % colors.length];
         int cx = x(box, zone.longitude);
         int cy = y(box, zone.latitude);
         g.setColor(Ui.FACE);
         g.fillOval(cx - dot - 1, cy - dot - 1, 2 * dot + 2, 2 * dot + 2);
         g.setColor(color);
         g.fillOval(cx - dot, cy - dot, 2 * dot, 2 * dot);
         int width = metrics.stringWidth(zone.name) + 4;
         int height = metrics.getAscent() + 2;
         // Right of the dot, else left, above or below; kept inside the map.
         int[][] spots = {{cx + dot + 2, cy - height / 2}, {cx - dot - 2 - width, cy - height / 2},
            {cx - width / 2, cy - dot - 2 - height}, {cx - width / 2, cy + dot + 2}};
         for (int[] spot : spots) {
            Rectangle label = new Rectangle(Math.max(box.x + 1, Math.min(box.x + box.width - 1 - width, spot[0])),
                  Math.max(box.y + 1, Math.min(box.y + box.height - 1 - height, spot[1])), width, height);
            if (!WorldView.overlaps(label, taken)) {
               taken.add(label);
               g.setColor(Ui.FACE);
               g.fillRoundRect(label.x, label.y, label.width, label.height, 4, 4);
               g.setColor(color);
               Ui.center(g, zone.name, font, label);
               break;
            }
         }
      }
   }

   /** The picture on the one-degree grid, made again only when the minute shown changes. */
   private BufferedImage image(long t, double[] sun) {
      long minute = Math.floorDiv(t, MINUTE);
      if (this.image != null && minute == this.imageMinute) {
         return this.image;
      }
      if (this.image == null) {
         this.image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
      }
      WritableRaster raster = this.image.getRaster();
      int[] rgb = new int[3];
      for (int row = 0; row < HEIGHT; row++) {
         double latitude = 89.5 - row;
         for (int column = 0; column < WIDTH; column++) {
            double longitude = column - 179.5;
            boolean isLand = land(latitude, longitude);
            double dark = dark(altitude(latitude, longitude, sun));
            Color day = isLand ? LAND : SEA;
            Color night = isLand ? LAND_NIGHT : SEA_NIGHT;
            rgb[0] = mix(day.getRed(), night.getRed(), dark);
            rgb[1] = mix(day.getGreen(), night.getGreen(), dark);
            rgb[2] = mix(day.getBlue(), night.getBlue(), dark);
            raster.setPixel(column, row, rgb);
         }
      }
      this.imageMinute = minute;
      return this.image;
   }

   private static int mix(int from, int to, double amount) {
      return (int) Math.round(from + (to - from) * amount);
   }

   private static int x(Rectangle box, double longitude) {
      return box.x + (int) Math.round((wrap(longitude) + 180) / 360 * box.width);
   }

   private static int y(Rectangle box, double latitude) {
      return box.y + (int) Math.round((90 - latitude) / 180 * box.height);
   }

   /** A longitude brought into -180 to 180. */
   static double wrap(double degrees) {
      return ((degrees + 180) % 360 + 360) % 360 - 180;
   }
}
