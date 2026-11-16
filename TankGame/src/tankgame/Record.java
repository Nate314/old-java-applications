package tankgame;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * The player's matches won and lost against the computer, kept in the first line of a text file ({@code wins,losses}),
 * whether supply drops are on ({@code supplies=off}), whether the sound is on ({@code sound=off}) and the battlefield the player built ({@code field=}, see
 * {@link Arena#text}).
 */
public class Record
{
    private final File file;
    private int        wins;
    private int        losses;
    private boolean    supplies = true;
    private boolean    sound    = true;
    private String     field    = "";

    private Record(File file)
    {
        this.file = file;
    }

    /** Reads the record, starting from 0-0 if the file is missing or unreadable. */
    public static Record load(File file)
    {
        Record record = new Record(file);
        try (BufferedReader reader = new BufferedReader(new FileReader(file)))
        {
            String[] parts = reader.readLine().split(",");
            record.wins = Math.max(0, Integer.parseInt(parts[0].trim()));
            record.losses = Math.max(0, Integer.parseInt(parts[1].trim()));
            for (String line; (line = reader.readLine()) != null;)
            {
                line = line.trim();
                if (line.equals("supplies=off")) record.supplies = false;
                if (line.equals("sound=off")) record.sound = false;
                if (line.startsWith("field=")) record.field = line.substring(6);
            }
        }
        catch (IOException | RuntimeException e)
        {
            record.wins = record.losses = 0;
        }
        return record;
    }

    /** Counts a finished match and saves the record. */
    public void add(boolean won)
    {
        if (won) wins++;
        else losses++;
        save();
    }

    /** Whether supply drops are on (the default); saved as a second line {@code supplies=off} when they are not. */
    public boolean supplies()
    {
        return supplies;
    }

    public void setSupplies(boolean on)
    {
        supplies = on;
        save();
    }

    /** Whether the battle's sounds are on (the default); saved as a line {@code sound=off} when they are not. */
    public boolean sound()
    {
        return sound;
    }

    public void setSound(boolean on)
    {
        sound = on;
        save();
    }

    /** The player's battlefield as {@link Arena#text}, or empty for none. */
    public String field()
    {
        return field;
    }

    public void setField(String text)
    {
        field = text;
        save();
    }

    private void save()
    {
        try (Writer writer = new FileWriter(file))
        {
            writer.write(wins + "," + losses + "\n" + (supplies ? "" : "supplies=off\n") + (sound ? "" : "sound=off\n")
                            + (field.isEmpty() ? "" : "field=" + field + "\n"));
        }
        catch (IOException e)
        {
            System.err.println("Could not save the record to " + file + ": " + e.getMessage());
        }
    }

    public int wins()
    {
        return wins;
    }

    public int losses()
    {
        return losses;
    }

    @Override
    public String toString()
    {
        return wins + (wins == 1 ? " win, " : " wins, ") + losses + (losses == 1 ? " loss" : " losses");
    }
}
