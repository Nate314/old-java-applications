package tankgame;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Random;

/**
 * The air over a ground (Terrain): seeds drifting over the meadow, sand blowing across the desert, snow falling on the
 * snow and rain on the mud, splashing where it lands. Drawn over the field but under the score bar and banners;
 * GamePanel leaves it out when the browser asks for reduced motion.
 */
public class Weather
{
    private final Terrain terrain;
    private final Random  random = new Random();
    /** Each bit's x, y, speed across, speed down, size, and (rain) the y it lands on and its splash's frames left. */
    private final double[][] bits;
    private int           ticks;

    public Weather(Terrain terrain, int width, int height)
    {
        this.terrain = terrain;
        int count = terrain == Terrain.SNOW ? 70 : terrain == Terrain.MUD ? 55 : terrain == Terrain.DESERT ? 34 : 12;
        bits = new double[count][7];
        for (double[] bit : bits) place(bit, width, height, true);
    }

    /** Puts a bit back at the edge it comes in from (or anywhere on the field, at the start). */
    private void place(double[] bit, int width, int height, boolean anywhere)
    {
        double size = random.nextDouble();
        bit[4] = size;
        bit[6] = 0;
        switch (terrain)
        {
            case SNOW:
                bit[2] = 0.3 + size * 0.5;
                bit[3] = 0.6 + size * 1.2;
                break;
            case MUD:
                bit[2] = -1.6;
                bit[3] = 9 + size * 4;
                bit[5] = 30 + random.nextDouble() * Math.max(1, height - 30);
                break;
            case DESERT:
                bit[2] = 4 + size * 4;
                bit[3] = 0.3 + random.nextDouble() * 0.4;
                break;
            default:
                bit[2] = 0.4 + size * 0.5;
                bit[3] = -0.1 + random.nextDouble() * 0.3;
                break;
        }
        boolean fromSide = terrain == Terrain.DESERT || terrain == Terrain.MEADOW;
        bit[0] = anywhere || !fromSide ? random.nextDouble() * (width + 40) - 20 : -20;
        bit[1] = anywhere || fromSide ? random.nextDouble() * height : -20;
    }

    /** Moves every bit one tick; a raindrop that reaches its spot splashes there, then falls again from the top. */
    public void tick(int width, int height)
    {
        ticks++;
        for (double[] bit : bits)
        {
            if (bit[6] > 0)
            {
                if (--bit[6] == 0) place(bit, width, height, false);
                continue;
            }
            // Snow and seeds sway as they go.
            double sway = terrain == Terrain.SNOW || terrain == Terrain.MEADOW ? Math.sin((ticks + bit[4] * 300) / 40.0) * 0.5 : 0;
            bit[0] += bit[2] + sway;
            bit[1] += bit[3];
            if (terrain == Terrain.MUD && bit[1] >= bit[5]) bit[6] = 10;
            else if (bit[0] > width + 20 || bit[0] < -30 || bit[1] > height + 20) place(bit, width, height, false);
        }
    }

    public void paint(Graphics2D g)
    {
        for (double[] bit : bits)
        {
            int x = (int) bit[0];
            int y = (int) bit[1];
            double size = bit[4];
            switch (terrain)
            {
                case SNOW:
                    int s = 2 + (int) (size * 3);
                    g.setColor(new Color(255, 255, 255, 150 + (int) (size * 90)));
                    g.fillOval(x - s / 2, y - s / 2, s, s);
                    break;
                case MUD:
                    if (bit[6] > 0)
                    {
                        // A ring spreading where the drop landed.
                        int r = (int) (12 - bit[6]);
                        g.setColor(new Color(200, 210, 225, (int) (bit[6] * 14)));
                        g.drawOval(x - r, y - r / 2, 2 * r, r);
                    }
                    else
                    {
                        g.setColor(new Color(190, 202, 220, 90));
                        g.drawLine(x, y, (int) (x - bit[2] * 1.5), (int) (y - bit[3] * 1.5));
                    }
                    break;
                case DESERT:
                    g.setStroke(new BasicStroke(1 + (float) size));
                    g.setColor(new Color(244, 226, 180, 60 + (int) (size * 50)));
                    g.drawLine(x, y, (int) (x - bit[2] * 2.5), (int) (y - bit[3] * 2.5));
                    g.setStroke(new BasicStroke(1));
                    break;
                default:
                    // A dandelion seed: a white tuft on a short stalk.
                    g.setColor(new Color(255, 255, 250, 200));
                    g.fillOval(x - 2, y - 2, 5, 5);
                    g.setColor(new Color(255, 255, 250, 120));
                    g.drawLine(x, y + 2, x - 2, y + 6);
                    break;
            }
        }
    }
}
