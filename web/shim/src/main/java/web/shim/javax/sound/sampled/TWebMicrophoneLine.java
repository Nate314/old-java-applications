package web.shim.javax.sound.sampled;

import org.teavm.jso.JSBody;
import web.rt.Exit;

/**
 * The page's microphone as a 16-bit mono line. Opening asks for the microphone (getUserMedia) and waits for the
 * answer; reading waits for the sound to arrive. Both wait with Thread.sleep, so call them off the event thread, as
 * on the desktop. The capture code lives in boot.js (window.__shimMic); the line's sample rate is the page's audio
 * rate (usually 48000 Hz), whatever rate was asked for. The line closes itself when the app exits.
 */
final class TWebMicrophoneLine implements TTargetDataLine {
    private TAudioFormat format;
    private boolean open;
    private boolean bigEndian;

    TWebMicrophoneLine(TAudioFormat format) {
        this.format = format;
    }

    @Override
    public void open() throws TLineUnavailableException {
        open(format);
    }

    @Override
    public void open(TAudioFormat wanted) throws TLineUnavailableException {
        if (open) {
            return;
        }
        if (wanted.getSampleSizeInBits() != 16 || wanted.getChannels() != 1) {
            throw new TLineUnavailableException("Only 16-bit mono sound can be recorded in the browser");
        }
        jsOpen();
        String state;
        while ((state = jsState()).equals("asking")) {
            pause(50);
        }
        if (!state.equals("open")) {
            jsClose();
            throw new TLineUnavailableException(state.equals("denied")
                    ? "The microphone is blocked: allow it in the address bar"
                    : "No microphone found");
        }
        bigEndian = wanted.isBigEndian();
        format = new TAudioFormat(jsRate(), 16, 1, true, bigEndian);
        open = true;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void close() {
        if (open) {
            open = false;
            jsClose();
        }
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public TAudioFormat getFormat() {
        return format;
    }

    @Override
    public int available() {
        return open ? jsQueued() * 2 : 0;
    }

    /** Fills {@code length} bytes (whole samples), waiting for the sound as needed; returns -1 once closed. */
    @Override
    public int read(byte[] bytes, int offset, int length) {
        int filled = 0;
        length &= ~1;
        while (filled < length) {
            if (Exit.isExited()) {
                close();   // the app has quit: let the microphone go
            }
            if (!open) {
                return filled == 0 ? -1 : filled;
            }
            int samples = jsTake((length - filled) / 2);
            if (samples == 0) {
                pause(10);
                continue;
            }
            for (int i = 0; i < samples; i++) {
                int sample = (int) Math.max(-32768, Math.min(32767, Math.round(jsSample(i) * 32768)));
                bytes[offset + filled + (bigEndian ? 1 : 0)] = (byte) sample;
                bytes[offset + filled + (bigEndian ? 0 : 1)] = (byte) (sample >> 8);
                filled += 2;
            }
        }
        return filled;
    }

    private static void pause(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @JSBody(script = "window.__shimMic.open();")
    private static native void jsOpen();

    /** "asking", "open", "denied" or "none". */
    @JSBody(script = "return window.__shimMic.state();")
    private static native String jsState();

    @JSBody(script = "return window.__shimMic.rate();")
    private static native float jsRate();

    @JSBody(script = "return window.__shimMic.queued();")
    private static native int jsQueued();

    /** Moves up to {@code max} samples into the read buffer; returns how many. */
    @JSBody(params = "max", script = "return window.__shimMic.take(max);")
    private static native int jsTake(int max);

    @JSBody(params = "i", script = "return window.__shimMic.sample(i);")
    private static native double jsSample(int i);

    @JSBody(script = "window.__shimMic.close();")
    private static native void jsClose();
}
