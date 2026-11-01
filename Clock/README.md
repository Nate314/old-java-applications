# Clock

A desktop clock window. It shows an analog clock face (hour, minute and red second hand) or a digital readout of the day, date and 12-hour time, updated several times a second so the second hand sweeps smoothly.

## Controls / usage

- **Left click** anywhere in the window (on mouse release) to toggle between analog and digital mode.
- **Right click** to toggle the digital readout between 12-hour and 24-hour time.
- No keyboard controls or command-line arguments.
- The window is a square one quarter of the screen width wide. The current time text is also printed to stdout each time it changes (once a second).

## Code layout

- `src/clock/Main.java`: the `JFrame`; holds the current date/time fields and starts a 50 ms Swing `Timer`.
- `src/clock/ClockTicker.java`: the timer's `ActionListener`; reads the current time from a `Calendar`, formats the time text, prints it and repaints.
- `src/clock/ClockPanel.java`: draws the analog or digital view and toggles the mode on mouse release.

## Running

From the repo root: `./run.ps1 Clock` (or `./run.ps1 Clock -Build` to build a jar).
