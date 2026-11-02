package web.shim.javax.sound.sampled;

/** The PCM part of javax.sound.sampled.AudioFormat: rate, sample size, channels, signed and byte order. */
public class TAudioFormat {
    private final float sampleRate;
    private final int sampleSizeInBits;
    private final int channels;
    private final boolean signed;
    private final boolean bigEndian;

    public TAudioFormat(float sampleRate, int sampleSizeInBits, int channels, boolean signed, boolean bigEndian) {
        this.sampleRate = sampleRate;
        this.sampleSizeInBits = sampleSizeInBits;
        this.channels = channels;
        this.signed = signed;
        this.bigEndian = bigEndian;
    }

    public float getSampleRate() {
        return sampleRate;
    }

    public float getFrameRate() {
        return sampleRate;
    }

    public int getSampleSizeInBits() {
        return sampleSizeInBits;
    }

    public int getChannels() {
        return channels;
    }

    public int getFrameSize() {
        return (sampleSizeInBits + 7) / 8 * channels;
    }

    public boolean isBigEndian() {
        return bigEndian;
    }

    @Override
    public String toString() {
        return "PCM_" + (signed ? "SIGNED " : "UNSIGNED ") + sampleRate + " Hz, " + sampleSizeInBits + " bit, "
                + channels + " channels, " + (bigEndian ? "big-endian" : "little-endian");
    }
}
