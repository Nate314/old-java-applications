# Clock

A desktop clock window. It shows an analog clock face (hour, minute and red second hand) or a digital readout of the day, date and 12-hour time, updated once a second.

## Controls / usage

- **Click** anywhere in the window (on mouse release) to toggle between analog and digital mode.
- No keyboard controls or command-line arguments.
- The window is a square one quarter of the screen width wide. The current time text is also printed to stdout every second.

## Code layout

- `src/clock/Main.java`: the `JFrame`; holds the current date/time fields and starts a 1-second Swing `Timer`.
- `src/clock/ClockTicker.java`: the timer's `ActionListener`; reads the current `Date`, formats the time text, prints it and repaints.
- `src/clock/ClockPanel.java`: draws the analog or digital view and toggles the mode on mouse release.

## Running

From the repo root: `./run.ps1 Clock` (or `./run.ps1 Clock -Build` to build a jar).

## Known quirks

- The second hand is the same length as the minute hand (`secondHandLength = (radius << 1) / 3`, same as `minuteHandLength`).
- The hour hand only moves on the hour; its angle uses `hours` alone, not minutes.
- The face is drawn at x = `leftMargin / 2` but the hands are centered at `leftMargin + radius`, so the hands sit slightly right of the face's center.
- In digital mode the font shrinks only while one third of the text width exceeds the window width, so the text can still overflow.
- The `Main(Boolean)` constructor is private.
