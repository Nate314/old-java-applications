
interface RecordRule { file: string; show(text: string, saved: (name: string) => string | null): string }
type NewsStory = Pick<NonNullable<Window['oldjavaNews']>, 'story'>;

let RECORDS: Record<string, RecordRule>;
let newsPromise: Promise<NewsStory> | null;

// ---- title screen ----
// An app page opens on a title screen while its files load: the app's thumbnail, name, category, hint and
// passport stamp, a boot log of the files as they arrive (real names and sizes) and a Play button. The app
// starts on the visitor's click or key press, so its first sounds (MusicTheoryExpert's demo, the site's
// start sound) are allowed to play. Run again skips it (see overlay). Returns { loaded(file), ready(start) }.
export const AGAIN_KEY = 'oldjava.again';

export const CATS: Record<string, string> = { game: 'Game', music: 'Music', toy: 'Toy', tool: 'Tool' };

export function size(bytes: number) {
  return bytes >= 1048576 ? (bytes / 1048576).toFixed(1) + ' MB' : Math.max(1, Math.round(bytes / 1024)) + ' KB';
}

// What an app saved on earlier visits: web.rt.Saves keeps the files it writes under oldjava.saves.<App>, each byte
// one character. Apps with a score file get their record read out; any other saves are named.
// NAME=stars,score rows (MusicTheoryExpert's drills, NumberGuess's ranges, Star Catcher's nights): how many have a
// star, and the stars.
function starRows(text: string) {
  var won = 0, stars = 0;
  text.split('\n').forEach(function (row) {
    var n = parseInt((row.split('=')[1] || '').split(',')[0], 10);
    if (/^[A-Z_]+=/.test(row) && n >= 0 && n <= 3) { won += n > 0 ? 1 : 0; stars += n; }
  });
  return { won: won, stars: stars };
}

// BibleDownloader's reading plan (audiobibledownloader.Plan): its name, days and chapters, the day it is on today
// (planstart is its day 1, in local time) and the chapters read for it.
const BIBLE_PLANS: Record<string, [string, number, number]> = { GOSPELS: ['The Gospels in 30 days', 30, 89], PROVERBS: ['Proverbs in a month', 31, 31],
  PSALMS: ['Psalms in 50 days', 50, 150], NEW: ['The New Testament in 90 days', 90, 260], YEAR: ['The Bible in a year', 365, 1189] };

function biblePlan(text: string, count: (m: RegExpExecArray | null) => number) {
  var id = /^plan=(\w+)$/m.exec(text), start = /^planstart=(\d+)-(\d+)-(\d+)$/m.exec(text);
  var plan = id && BIBLE_PLANS[id[1]];
  if (!plan || !start) return '';
  var now = new Date();
  var day = Math.round(((new Date(now.getFullYear(), now.getMonth(), now.getDate()) as unknown as number) - (new Date(+start[1], (start[2] as unknown as number) - 1, +start[3]) as unknown as number)) / 864e5);
  var read = count(/^plandone=(.*)$/m.exec(text));
  return '; reading plan: ' + plan[0] + (day >= 0 && day < plan[1] ? ', day ' + (day + 1) + ' of ' + plan[1] : '')
    + ', ' + read + ' of ' + plan[2].toLocaleString('en-US') + ' chapters';
}

// <body data-tab-title>: the browser tab shows the app's window title whenever it says more than the app's name
// (Clock's "4:59 left - Clock"), so a running timer can be watched from another tab.
export function followTitle(desktop: Element) {
  var base = document.title;
  new MutationObserver(function () {
    var bar = desktop.querySelector('.titlebar .title');
    var text = bar ? bar.textContent : '';
    var next = text && text !== base ? text : base;
    if (document.title !== next) document.title = next;
  }).observe(desktop, { subtree: true, childList: true, characterData: true });
}

export function savedRecord(id: string) {
  var saves;
  try { saves = JSON.parse(localStorage.getItem('oldjava.saves.' + id) || '{}'); } catch (e) { return ''; }
  return recordOf(id, saves);
}

// The record line for an app's saved files ({ path: contents }), wherever they came from: this browser's store, or
// saves brought from another device (the home page's Your records, src/pages/home/Records.tsx).
function recordOf(id: string, saves: Record<string, string>) {
  var paths = Object.keys(saves || {});
  var rule = RECORDS[id];
  // The text of a saved file by its name, or null.
  function saved(name: string) {
    for (var i = 0; i < paths.length; i++) {
      if (paths[i].split('/').pop() === name) return saves[paths[i]];
    }
    return null;
  }
  if (rule) {
    var text = saved(rule.file);
    return text === null ? '' : rule.show(text, saved);
  }
  return paths.length ? 'Saved here from last time: ' + paths.map(function (p) { return p.split('/').pop(); }).join(', ') : '';
}

export function newsScript() {
  if (!newsPromise) newsPromise = new Promise<NewsStory>(function (resolve, reject) {
    import('./lazy/news').then(function () { resolve(window.oldjavaNews!); }, function () { reject(new Error('news.js did not load')); });
  }).catch(function (e) { console.warn(e.message); return { story: function () {} }; });
  return newsPromise;
}

export function init() {
  RECORDS = {
    // Today's course (.flappybird-today.txt) counts only on the day it was flown.
    FlappyBird: { file: '.flappybird-highscore.txt', show: function (text, saved) {
      var best = parseInt(text, 10);
      if (!(best > 0)) return '';
      var line = 'Your best: ' + best + (best === 1 ? ' pipe' : ' pipes');
      var today = /date=(\d+)\s+best=(\d+)/.exec(saved('.flappybird-today.txt') || '');
      var now = new Date();
      if (today && +today[1] === now.getFullYear() * 10000 + (now.getMonth() + 1) * 100 + now.getDate() && +today[2] > 0) {
        line += '; today\'s course: ' + today[2];
      }
      return line;
    } },
    Minesweeper: { file: '.minesweeper-best.txt', show: function (text) {
      var names: Record<string, string> = { BEGINNER: 'Beginner', CLASSIC: 'Classic', EXPERT: 'Expert' };
      var times = text.split('\n').map(function (row) {
        var kv = row.split('=');
        var ms = +kv[1];
        if (!names[kv[0]] || !(ms > 0)) return '';
        var tenths = Math.floor(ms / 100), secs = Math.floor(tenths / 10);
        return names[kv[0]] + ' ' + Math.floor(secs / 60) + ':' + (secs % 60 < 10 ? '0' : '') + secs % 60 + '.' + tenths % 10;
      }).filter(Boolean);
      return times.length ? 'Your best times: ' + times.join(', ') : '';
    } },
    UltimateTicTacToe: { file: '.ultimatetictactoe-record.txt', show: function (text) {
      var names: Record<string, string> = { EASY: 'Easy', HARD: 'Hard' };
      var rows = text.split('\n').map(function (row) {
        var kv = row.split('=');
        var n = (kv[1] || '').split(',').map(Number);
        if (!names[kv[0]] || n.length !== 3) return '';
        return names[kv[0]] + ' ' + n[0] + (n[0] === 1 ? ' win, ' : ' wins, ') + n[1] + (n[1] === 1 ? ' loss, ' : ' losses, ') + n[2] + (n[2] === 1 ? ' draw' : ' draws');
      }).filter(Boolean);
      return rows.length ? 'Your record vs the computer: ' + rows.join('; ') : '';
    } },
    SynthesizerKeyboard: { file: '.synthesizer-songs.txt', show: function (text) {
      var names: Record<string, string> = { MARY: 'Mary\'s Lamb', TWINKLE: 'Twinkle', ODE: 'Ode to Joy', ROW: 'Row Your Boat', JINGLE: 'Jingle Bells' };
      var rows = text.split('\n').map(function (row) {
        var kv = row.split('=');
        var stars = parseInt(kv[1], 10);
        if (!names[kv[0]] || !(stars > 0)) return '';
        return names[kv[0]] + ' ' + stars + (stars === 1 ? ' star' : ' stars');
      }).filter(Boolean);
      return rows.length ? 'Your songs: ' + rows.join(', ') : '';
    } },
    RubiksCube2D: { file: '.rubikscube-levels.txt', show: function (text) {
      var solved = 0, stars = 0;
      text.split('\n').forEach(function (row) {
        var kv = row.split('=');
        var n = parseInt(kv[1], 10);
        if (/^\d+$/.test(kv[0]) && n >= 1 && n <= 3) { solved++; stars += n; }
      });
      var made = /^patterns=(.+)$/m.exec(text), parts = solved ? ['Your levels: ' + solved + ' of 20 solved, ' + stars + ' of 60 stars'] : [];
      if (made) parts.push((parts.length ? 'patterns' : 'Your patterns') + ': ' + made[1].split(',').length + ' of 7 made');
      return parts.join('; ');
    } },
    MusicTheoryExpert: { file: '.musictheory-drills.txt', show: function (text) {
      var r = starRows(text);
      return r.won ? 'Your ear training: ' + r.won + ' of 6 drills passed, ' + r.stars + ' of 18 stars' : '';
    } },
    // Duels against the computer are a lower-case duel=won,lost,drawn row, which starRows passes by.
    NumberGuess: { file: '.numberguess-record.txt', show: function (text) {
      var r = starRows(text), d = /^duel=(\d+),(\d+),(\d+)$/m.exec(text);
      var parts = r.won ? ['Your ranges: ' + r.won + ' of 5 won, ' + r.stars + ' of 15 stars'] : [];
      if (d) parts.push((parts.length ? 'duels' : 'Your duels') + ' vs the computer: ' + d[1] + ' won, ' + d[2] + ' lost' + (+d[3] ? ', ' + d[3] + ' drawn' : ''));
      return parts.join('; ');
    } },
    SpriteFloatyStars: { file: '.spritefloatystars-nights.txt', show: function (text) {
      var r = starRows(text);
      return r.won ? 'Your Star Catcher nights: ' + r.won + ' of 6 caught, ' + r.stars + ' of 18 stars' : '';
    } },
    GroupCreator: { file: '.groupcreator-class.txt', show: function (text) {
      var names = /^names=(.*)$/m.exec(text);
      var count = names ? names[1].split(',').filter(function (n) { return n.trim(); }).length : 0;
      var rounds = (text.match(/^round=/mg) || []).length;
      var picked = /^picked=(.*)$/m.exec(text);
      var turns = picked ? picked[1].split(',').filter(function (n) { return n.trim(); }).length : 0;
      if (!rounds && !turns) return '';
      var parts = [];
      if (rounds) parts.push(rounds + (rounds === 1 ? ' round' : ' rounds') + ' of groups remembered');
      if (turns) parts.push(turns + ' of ' + count + ' picked by the wheel');
      var apart = (text.match(/^apart=/mg) || []).length;
      if (apart) parts.push(apart + (apart === 1 ? ' pair' : ' pairs') + ' kept apart');
      return 'Your class: ' + count + (count === 1 ? ' name, ' : ' names, ') + parts.join(', ');
    } },
    TankGame: { file: '.tankgame-record.txt', show: function (text) {
      var n = text.trim().split('\n')[0].split(',').map(Number);
      if (n.length !== 2 || !(n[0] + n[1] > 0)) return '';
      return 'Your matches vs the computer: ' + n[0] + (n[0] === 1 ? ' win, ' : ' wins, ') + n[1] + (n[1] === 1 ? ' loss' : ' losses');
    } },
    // Your best run, the bot's best run and the bots you evolved (generations bred, their record).
    DinoGame: { file: '.dinogame.txt', show: function (text) {
      var best = /^best=(\d+)$/m.exec(text), bot = /^bot=(\d+)$/m.exec(text), evo = /^evolved=(\d+),(\d+)$/m.exec(text);
      var you = best ? +best[1] : 0, it = bot ? +bot[1] : 0;
      var bred = evo ? 'your evolved bots: ' + evo[2] + ' after ' + evo[1] + (evo[1] === '1' ? ' generation' : ' generations') : '';
      if (!you && !it) return bred ? bred.charAt(0).toUpperCase() + bred.slice(1) : '';
      return (you ? 'Your best: ' + you : 'No run of your own yet') + (it ? "; the bot's best: " + it : '') + (bred ? '; ' + bred : '');
    } },
    // The reader: the place it is open at, chapters read to the end and highlighted verses, and its reading plan.
    BibleDownloader: { file: '.bibledownloader.txt', show: function (text) {
      var place = /^place=(.+)$/m.exec(text);
      if (!place) return '';
      var read = /^read=(.*)$/m.exec(text);
      var marks = /^marks=(.*)$/m.exec(text);
      var count = function (m: RegExpExecArray | null) { return m ? m[1].split(',').filter(function (r) { return r.trim(); }).length : 0; };
      var n = count(read), h = count(marks);
      // By heart: learn.<verse>=<translation>|<box>|<next review, yyyy-mm-dd>|<text>; box 0 is still being learned.
      var cards = text.match(/^learn\.\S+?=\w+\|\d+\|[\d-]+\|/mg) || [];
      var now = new Date(), today = now.getFullYear() + '-' + ('0' + (now.getMonth() + 1)).slice(-2) + '-' + ('0' + now.getDate()).slice(-2);
      var due = cards.filter(function (c) { var p = c.split('=')[1].split('|'); return p[1] === '0' || p[2] <= today; }).length;
      return 'Your Bible: open at ' + place[1].trim() + ', ' + n + ' of 1,189 chapters read'
        + (h ? ', ' + h + (h === 1 ? ' highlight' : ' highlights') : '') + biblePlan(text, count)
        + (cards.length ? '; by heart: ' + cards.length + (cards.length === 1 ? ' verse' : ' verses') + (due ? ', ' + due + ' to practice today' : '') : '');
    } },
    // The practice desk: the day it is on, the net worth at the last closing bell against the $10,000 start, and the rivals.
    StockChecker: { file: '.stockchecker-desk.txt', show: function (text) {
      var day = /^day=(\d+)$/m.exec(text);
      if (!day) return '';
      var closes = text.match(/^close=[\d.]+$/mg) || [];
      var line = 'Your trading desk: day ' + day[1];
      if (closes.length) {
        var last = +closes[closes.length - 1].slice(6);
        var pct = (last - 10000) / 100;
        line += ', closed day ' + closes.length + ' at $' + Math.round(last).toLocaleString('en-US') + ' (' + (pct >= 0 ? '+' : '') + pct.toFixed(1) + '% since day 1)';
        // The computer traders' last closes (rclose=Name:worth, stockchecker.Desk): how many you are ahead of.
        var rivals: Record<string, number> = {}, m: RegExpExecArray | null, re = /^rclose=(\w+):([\d.]+)$/mg;
        while ((m = re.exec(text))) rivals[m[1]] = +m[2];
        var names = Object.keys(rivals);
        if (names.length) line += ', ahead of ' + names.filter(function (n) { return rivals[n] < last; }).length + ' of ' + names.length + ' rivals';
      }
      return line;
    } },
    // The desktop as it was left: its wallpaper (main.Wallpaper, "picture:<name>" for a Paint picture) and whether its
    // icons were arranged by hand.
    NathaNOS: { file: '.nathanos.txt', show: function (text) {
      var names: Record<string, string> = { photo: 'the original photo', night: 'Night sky', sunset: 'Sunset hills', ocean: 'Deep ocean', aurora: 'Aurora' };
      var wall = /^wallpaper=(.+)$/m.exec(text);
      var moved = /^icon=/m.test(text);
      if (!wall || (wall[1] === 'photo' && !moved)) return '';
      var painted = /^picture:(.+)$/.exec(wall[1]);
      return 'Your desktop: ' + (painted ? 'your painting ' + painted[1] + ' as the' : (names[wall[1]] || 'the original photo'))
        + ' wallpaper' + (moved ? ', icons arranged by hand' : '');
    } },
    // The saver it was left on, in the order of the app's list (screensaver.Main), and whether it cycles.
    ScreenSaver: { file: '.screensaver.txt', show: function (text) {
      var names = ['Green circles', 'Green squares', 'Bright circles', 'Starfield', 'Mystify', 'Pipes', 'Bouncing logo', 'Dot clock',
                   'Aquarium'];
      var saver = /^saver (\d+)$/m.exec(text);
      var name = saver && names[+saver[1]];
      if (!name) return '';
      // The aquarium's fish (screensaver.Tank): "fish <species> <day> <meals> <name>".
      var fish = text.match(/^fish \d+ \d+ \d+ \S+$/gm) || [];
      return 'Your screen saver: ' + name + (/^cycle off$/m.test(text) ? ', kept on screen' : ', then the next every 30 s')
        + (fish.length ? '; ' + fish.length + ' fish in your aquarium' : '');
    } },
    // The timer counts down against the wall clock, so a running one is still running (or has rung) on return.
    Clock: { file: '.clock.txt', show: function (text) {
      var v: Record<string, string> = {};
      text.split('\n').forEach(function (row) {
        var at = row.indexOf('=');
        if (at > 0) v[row.slice(0, at)] = row.slice(at + 1);
      });
      var parts = [];
      var end = +v.timerEnd, left = Math.ceil((end - Date.now()) / 1000);
      if (end > 0 && left > 0) parts.push('timer running, ' + Math.floor(left / 60) + ':' + (left % 60 < 10 ? '0' : '') + left % 60 + ' left');
      else if (end > 0 || +v.timerDone > 0) parts.push('your timer has rung');
      else if (+v.timer > 0) parts.push(Math.round(+v.timer / 60000) + '-minute timer set');
      var laps = (v.laps || '').split(',').filter(Boolean).length;
      if (+v.watchStart > 0) parts.push('stopwatch running');
      if (laps) parts.push(laps + (laps === 1 ? ' lap' : ' laps') + ' on the stopwatch');
      // Focus sessions: the one running, and today's log ("20261102:75:3" is a day, its minutes and its sessions).
      var focusEnd = +v.focusEnd, focusLeft = Math.ceil((focusEnd - Date.now()) / 60000);
      if (focusEnd > 0 && focusLeft > 0) parts.push((+v.focusPhase ? 'on a break, ' : 'focusing, ') + focusLeft + ' min left');
      var d = new Date(), today = String(d.getFullYear() * 10000 + (d.getMonth() + 1) * 100 + d.getDate());
      (v.focusLog || '').split(',').forEach(function (day) {
        var p = day.split(':');
        if (p[0] === today && +p[2] > 0) parts.push(p[2] + (+p[2] === 1 ? ' focus session' : ' focus sessions') + ' today');
      });
      // The World tab's cities, once changed from its first three ("world=London,New York,Tokyo").
      if (v.world && v.world !== 'London,New York,Tokyo') parts.push('world clock: ' + v.world.split(',').join(', '));
      return parts.length ? 'Your clock: ' + parts.join(', ') : '';
    } }
  };
  window.oldjavaRecords = { line: recordOf };
  // news.js (What's new), which the home page loads itself, added to an app page once.
  newsPromise = null;
}
