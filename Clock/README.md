# Clock

A desktop clock window with three tabs. Clock is the original app: an analog clock face (hour, minute and red second hand) or a digital readout of the day, date and time, updated several times a second so the second hand sweeps smoothly. Timer is a kitchen timer on a 60-minute dial: drag the knob round and a red wedge shows the time set, then shrinks back to 12 as it counts down, and a bell rings when it runs out. Stopwatch times to the hundredth of a second, with laps.

The timer and stopwatch count against the wall clock, not the window's ticks, and are saved as they change. Close the window with a timer running and open it again later: it is still counting, or it has rung, and says when.

## Controls / usage

- **Tabs**: click Clock, Timer or Stopwatch at the top, or press **1**, **2**, **3** (or **C**, **T**, **S**). A running timer or stopwatch shows its time on its tab ("Timer 4:59"), so it can be watched from the others. The line at the bottom lists the current tab's controls.
- **Clock**: left click (on mouse release) switches between analog and digital, right click between 12-hour and 24-hour time.
- **Timer**:
  - Drag the knob (or click anywhere on the dial) to set whole minutes from 1 to 60. The knob stops at 0 and at 60 instead of wrapping round past 12. Turning it while the timer runs keeps it running from the new time.
  - The preset buttons set 1, 3, 5, 10, 25 or 60 minutes.
  - **Start**, **Pause** and **Resume** (or **Space** or **Enter**); **Reset** (**R**) goes back to the full time. **Up** / **Down** add or take away a minute.
  - Under the time left it says when the timer will end ("Ends at 3:42 pm"). The window title says the time left too ("4:59 left - Clock"), so it shows on the taskbar.
  - When it runs out the window switches to the Timer tab, flashes pink and rings a tubular bell phrase on the MIDI synthesizer every few seconds, eight times, and says "Rang at 3:42 pm". **Stop** (or a click anywhere else on the tab, or any key but R and the tab keys) silences it and leaves the timer ready at the same time; **Again** (**R**) starts the same time over.
- **Stopwatch**: **Start** / **Stop** (**Space** or **Enter**), **Lap** while running (**L**), **Reset** once stopped (**R**). A ring sweeps once a minute. Laps are listed newest first with each lap's time and the total, the fastest in green and the slowest in red (up to 99 laps).
- No command-line arguments.
- The window is a square one quarter of the screen width wide, but at least 380 pixels (and never bigger than the screen). The current time text is also printed to stdout each time it changes (once a second).

## Saved state

`~/.clock.txt` (in `user.home`) keeps the tab, analog or digital, 12 or 24 hours, the timer (its length, and when it ends while running or the time left while paused) and the stopwatch (when it started while running, the time banked before the last stop, and the laps). A missing or damaged file, or a damaged line, leaves the defaults: the Clock tab and a 5-minute timer.

## Code layout

- `src/clock/Main.java`: the `JFrame`; holds the current date/time fields, loads the state, starts a 50 ms Swing `Timer`, keeps the title up to date and rings when the timer runs out.
- `src/clock/ClockTicker.java`: the timer's `ActionListener`; reads the local time from a `Date`, formats the time text, prints it, checks the countdown and repaints.
- `src/clock/ClockPanel.java`: draws the tab strip and the help line, and routes the mouse and keys to the current tab.
- `src/clock/Tab.java`: what a tab does (label, help line, paint, mouse and keys).
- `src/clock/ClockFace.java`: the original analog and digital clock.
- `src/clock/TimerView.java` and `src/clock/Countdown.java`: the timer's dial and controls, and the countdown itself.
- `src/clock/StopwatchView.java` and `src/clock/Stopwatch.java`: the stopwatch's ring, laps and controls, and the stopwatch itself.
- `src/clock/Chime.java`: the bell, played with Swing timers so the event thread never sleeps (a system beep where there is no synthesizer).
- `src/clock/ClockState.java`: reads and writes `~/.clock.txt`.
- `src/clock/Ui.java`: the shared colors (the original cyan, black, white and red), fonts, buttons and time formats.

The clock used to read the time from `Calendar`. It now reads `Date`'s local-time getters, which give the same time on the desktop and give local time in the browser, where TeaVM's `Calendar` is UTC.

## Running

From the repo root: `./run.ps1 Clock` (or `./run.ps1 Clock -Build` to build a jar).

## Self-check

`test/clock/TimerCheck.java` checks the countdown (start, pause, resume, running out exactly once, the one-hour cap, Up and Down while running), the stopwatch's laps, fastest and slowest, the time formats, and that the state file round-trips a running timer and stopwatch, rings a timer that ran out while closed at its own time, and survives damaged lines. From `Clock/`:

```
javac -d ../out/check/Clock src/clock/*.java test/clock/*.java
java -ea -cp ../out/check/Clock clock.TimerCheck
```
