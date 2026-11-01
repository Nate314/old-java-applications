package tankgame;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/** The player's matches won and lost against the computer, kept in one line of a text file: {@code wins,losses}. */
public class Record
{
    private final File file;
    private int        wins;
    private int        losses;

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
        try (Writer writer = new FileWriter(file))
        {
            writer.write(wins + "," + losses + "\n");
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
