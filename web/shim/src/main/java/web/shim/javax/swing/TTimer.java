package web.shim.javax.swing;

import java.util.ArrayList;
import java.util.List;
import web.rt.Edt;
import web.rt.Exit;
import web.rt.Js;
import web.shim.java.awt.event.TActionEvent;
import web.shim.java.awt.event.TActionListener;

/** javax.swing.Timer on setTimeout. Ticks run on the EDT and coalesce while one is still queued, as in Swing. */
public class TTimer {
    private final List<TActionListener> listeners = new ArrayList<>();
    private int delay;
    private int initialDelay;
    private boolean repeats = true;
    private boolean coalesce = true;
    private boolean running;
    private boolean pending;
    private int timeoutId = -1;
    private String actionCommand;

    public TTimer(int delay, TActionListener listener) {
        this.delay = delay;
        this.initialDelay = delay;
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void addActionListener(TActionListener l) {
        listeners.add(l);
    }

    public void removeActionListener(TActionListener l) {
        listeners.remove(l);
    }

    public void start() {
        if (!running) {
            running = true;
            schedule(initialDelay);
        }
    }

    public void stop() {
        running = false;
        if (timeoutId >= 0) {
            Js.clearTimeout(timeoutId);
            timeoutId = -1;
        }
    }

    public void restart() {
        stop();
        start();
    }

    public boolean isRunning() {
        return running;
    }

    private void schedule(int ms) {
        timeoutId = Js.setTimeout(this::fire, Math.max(ms, 0));
    }

    private void fire() {
        timeoutId = -1;
        if (!running || Exit.isExited()) {
            running = false;
            return;
        }
        if (!pending || !coalesce) {
            pending = true;
            Edt.post(this::deliver);
        }
        if (repeats) {
            schedule(delay);
        } else {
            running = false;
        }
    }

    private void deliver() {
        pending = false;
        TActionEvent e = new TActionEvent(this, TActionEvent.ACTION_PERFORMED, actionCommand, System.currentTimeMillis(), 0);
        for (TActionListener l : new ArrayList<>(listeners)) {
            l.actionPerformed(e);
        }
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public int getInitialDelay() {
        return initialDelay;
    }

    public void setInitialDelay(int initialDelay) {
        this.initialDelay = initialDelay;
    }

    public boolean isRepeats() {
        return repeats;
    }

    public void setRepeats(boolean repeats) {
        this.repeats = repeats;
    }

    public boolean isCoalesce() {
        return coalesce;
    }

    public void setCoalesce(boolean coalesce) {
        this.coalesce = coalesce;
    }

    public String getActionCommand() {
        return actionCommand;
    }

    public void setActionCommand(String command) {
        actionCommand = command;
    }
}
