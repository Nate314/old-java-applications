// The tour's stops: each app in the player, with one thing to try in it (web/README.md, The tour).
import { APPS } from '../../shared/catalog';

export const TOUR: [app: string, tryThis: string][] = ([
  ['Minesweeper', 'The computer is solving a board that never needs a guess. Click the board to take over, and Hint explains the next safe move.'],
  ['FlappyBird', 'Click or press Space to flap. When you crash, press Z to rewind the last seconds and fly on.'],
  ['CircleSpawner', 'Every shape here is made of real windows. Pick another shape and watch them morph, or click one to scatter it.'],
  ['SynthesizerKeyboard', 'Play the keys with the mouse or the keyboard, or pick a song (1 to 5) and play each key as it lights up.'],
  ['DinoGame', 'The old bot is playing by watching a single pixel. Press a key to take over, or E to breed bots that learn.'],
  ['RubiksCube2D', 'Drag a square to turn its layer. Mix the cube, then press T and Teach me shows every turn of the solve.'],
  ['Clock', 'Open the Timer tab and drag the red dial (it follows you to every app page), or open World and find an hour that suits three cities.'],
  ['UltimateTicTacToe', 'Two computer players. The square you take picks the board your opponent plays next; click to take over.'],
  ['ScreenSaver', 'Press N for the next of nine screen savers, 9 for the aquarium, and F to feed the fish.'],
  ['MusicTheoryExpert', 'Open the Songwriter tab and click chords on the circle of fifths: it loops them into a song.'],
  ['TankGame', 'The computer is driving both tanks. Press a key to take the red one: W A S D drive, Q fires.'],
  ['SpriteFloatyStars', 'A thousand stars orbit a sun. Click to place up to four suns and watch them swing between them.'],
  ['SimpleAlgorithms', 'Six sorting algorithms race on the same numbers. Click one to hear it work.'],
  ['NumberGuess', 'Type a guess or click the number line: it lights what is still possible. Magic is a card trick that reads your mind.'],
  ['GroupCreator', 'Deal a class into groups, then deal again: the next round gives people partners they have not had yet.'],
  ['StockChecker', '$10,000 of play money in a market that opens at its last real 2016 quotes. B buys, S sells, R shows the race.'],
  ['BibleDownloader', 'An open book in five public domain translations. Turn the page, or pick a reading plan.'],
  ['NathaNOS', 'A whole fake desktop: open any app from its icons or the start menu, or paint a picture in Paint.'],
  ['Windows', 'It presses the Windows key once, then quits. Here the key opens a pretend Start menu of every app: type to search.'],
  ['WindowsE', 'Win+E as many times as you ask, on a pretend Windows: double-click a folder to play that app.'],
] as [string, string][]).filter(t => APPS.some(a => a.id === t[0]));
