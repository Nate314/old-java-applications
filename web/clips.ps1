# Records the home page's preview clips (web/site/clips/<App>.mp4): a few seconds of each app running, which the
# gallery plays in a card's thumbnail while the pointer is on it (or, on touch screens, while the card is in the
# middle of the screen).
# Each app page is opened in clip mode (?embed&clip) in headless Chrome, driven over the DevTools protocol. Clip
# mode is the page's own instant replay recorder (boot.js startReplay) set to a fixed 640x400, silent, small
# clip. The script waits for the app to start, follows the app's recipe below (keys to press, the computer
# player for the four games that have one), throws away what was recorded so far at "rec", and keeps the next
# -Seconds seconds.
# Build first (mvn -f web/pom.xml package), then run from the repo root:
#   ./web/clips.ps1
#   ./web/clips.ps1 -Apps FlappyBird,ScreenSaver -Port 8101
# Apps without a recipe (NathaNOS, BibleDownloader, NumberGuess, Windows, WindowsE: nothing moves until the visitor
# does something) have no clip; their cards keep their still thumbnail. A card plays a clip when its <li> has
# data-clip (web/site/index.html).
param(
    [string[]]$Apps,
    [int]$Port = 8101,
    [int]$DebugPort = 9301,
    [string]$Chrome = "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
    [int]$Seconds = 6
)
$ErrorActionPreference = 'Stop'

# Steps are those of web/cdp.ps1, plus 'rec', where the kept clip starts. Query is added to the page address.
. (Join-Path $PSScriptRoot 'cdp.ps1')
# Flappy Bird has no computer player, so the clip has a small one: it finds the bird (orange) and the next pipe
# (green, from the top edge of the game's canvas) every frame and flaps while the bird is below the gap's middle
# (the gap is twice a pipe's width, as in Pipes.java).
$FlappyPilot = @'
js:(function () {
  var c = document.querySelector('#desktop canvas'), g = c.getContext('2d'), last = 0, prev = 0;
  function flap() { ['keydown', 'keyup'].forEach(function (t) { document.dispatchEvent(new KeyboardEvent(t, { key: ' ', code: 'Space', keyCode: 32, which: 32, bubbles: true })); }); }
  function green(d, i) { return d[i + 1] > d[i] + 40 && d[i + 1] > d[i + 2] + 20 && d[i + 1] > 120; }
  function tick() {
    requestAnimationFrame(tick);
    var W = c.width, H = c.height, x0 = Math.round(W * 0.2), bw = Math.round(W * 0.14), bh = Math.round(H * 0.87);
    var band = g.getImageData(x0, 0, bw, bh).data, n = 0, sy = 0, sx = 0;
    for (var y = 0; y < bh; y += 2) for (var x = 0; x < bw; x += 2) {
      var i = (y * bw + x) * 4;
      if (band[i] > 200 && band[i + 1] > 90 && band[i + 1] < 190 && band[i + 2] < 80) { n++; sy += y; sx += x; }
    }
    if (n < 3) return;
    var by = sy / n, bx = x0 + sx / n, row = g.getImageData(0, 3, W, 1).data, p0 = -1, p1 = W;
    for (var x = Math.max(0, Math.round(bx - W * 0.035)); x < W; x++) {
      var on = green(row, x * 4);
      if (on && p0 < 0) p0 = x;
      if (!on && p0 >= 0) { p1 = x; break; }
    }
    var target = H * 0.45;
    if (p0 >= 0 && p1 < W) {
      var col = g.getImageData(p0 + 4, 0, 1, H).data, top = 0;
      while (top < H && green(col, top * 4)) top++;
      var bottom = top + Math.round(H * 0.15);
      while (bottom < bh && !green(col, bottom * 4)) bottom++;
      target = (top + bottom) / 2 + H * 0.06;
    }
    var t = performance.now();
    var fall = by - prev;
    prev = by;
    if (by + 2 * fall > target && t - last > 180) { flap(); last = t; }
  }
  tick();
})()
'@
# SynthesizerKeyboard's looper: clear any old loop (Delete twice), record a melody layer after the count-in, then a
# bass layer an octave down at the next bar. Keys are held (keydown, then keyup after ms) so notes get their lengths.
function SynthHold([string]$key, [int]$code, [int]$ms) {
    "js:(function (t) { function f(type) { t.dispatchEvent(new KeyboardEvent(type, { key: '$key', keyCode: $code, which: $code, bubbles: true })); } f('keydown'); setTimeout(function () { f('keyup'); }, $ms); })(document.activeElement || document.body)"
}
$SynthLoop = @('wait:800', 'key:O', (SynthHold 'Delete' 46 20), (SynthHold 'Delete' 46 20), 'wait:200', 'key:Space', 'wait:2450') +
    (@(@('z', 90), @('c', 67), @('b', 66), @('c', 67), @('z', 90), @('c', 67), @('b', 66), @(',', 188)) | ForEach-Object { (SynthHold $_[0] $_[1] 450), 'wait:625' }) +
    @('wait:400', 'key:ArrowLeft', 'key:Space', 'wait:2300') +
    (@(@('z', 90), @('b', 66), @('v', 86), @('c', 67)) | ForEach-Object { (SynthHold $_[0] $_[1] 1100), 'wait:1250' })
# SpriteFloatyStars Orbits: two more suns, clicked into the biggest window's field (upper left and lower right).
$StarsSuns = "js:(function (c) { var r = c.getBoundingClientRect(); [[0.27, 0.3], [0.72, 0.7]].forEach(function (p) { var x = r.left + r.width * p[0], y = r.top + r.height * p[1], t = document.elementFromPoint(x, y); ['pointerdown', 'mousedown', 'pointerup', 'mouseup', 'click'].forEach(function (type) { var o = { bubbles: true, cancelable: true, clientX: x, clientY: y, button: 0, buttons: /down/.test(type) ? 1 : 0, detail: 1, pointerType: 'mouse', isPrimary: true, pointerId: 1 }; t.dispatchEvent(/^pointer/.test(type) ? new PointerEvent(type, o) : new MouseEvent(type, o)); }); }); })([].slice.call(document.querySelectorAll('#desktop canvas.paint')).sort(function (a, b) { return b.width * b.height - a.width * a.height; })[0])"
$Recipes = [ordered]@{
    Minesweeper         = @{ Query = 'watch'; Steps = @('wait:1500', 'rec') }
    FlappyBird          = @{ Steps = @('wait:800', 'key:Space', $FlappyPilot, 'wait:1000', 'rec') }
    DinoGame            = @{ Steps = @('key:E', 'wait:2500', 'rec') }
    UltimateTicTacToe   = @{ Query = 'watch'; Steps = @('wait:2500', 'rec') }
    RubiksCube2D        = @{ Steps = @('wait:800', (Click '^Mix$'), 'wait:1500', 'key:T', 'wait:600', 'rec') + (Repeat 12 @($CoachTurn, 'wait:480')) }
    SynthesizerKeyboard = @{ Steps = $SynthLoop + @('wait:600', 'rec') }
    Clock               = @{ Steps = @('wait:800', 'key:F', 'wait:300', 'key:Space', 'wait:600', 'rec') }
    SpriteFloatyStars   = @{ Steps = @('wait:800', (Pick 'Mode 6'), (Click '^OK$'), 'wait:3000', $StarsSuns, 'wait:7000', 'rec') }
    TankGame            = @{ Query = 'watch'; Steps = @('wait:3000', 'rec') }
    MusicTheoryExpert   = @{ Steps = @('wait:800', (Click 'Songwriter'), 'wait:500', 'key:Space', 'wait:1200', 'rec') }
    CircleSpawner       = @{ Steps = @('wait:2500', 'rec', 'wait:400', (Pick 'Heart'), 'wait:2600', (Pick 'Star')) }
    ScreenSaver         = @{ Steps = @('wait:1500', 'key:9', 'wait:2500', 'rec', 'wait:1200', 'key:F') }
    StockChecker        = @{ Steps = @('wait:800', 'key:F', 'wait:1000', 'key:R', 'rec') }
    GroupCreator        = @{ Steps = @('wait:800', 'rec', (Click 'Make groups'), 'wait:1500', 'key:P', 'wait:600', 'key:Space') }
    SimpleAlgorithms    = @{ Steps = @('wait:800', 'key:5', 'rec') }
}
if (-not $Apps) { $Apps = @($Recipes.Keys) }

$site = Join-Path $PSScriptRoot 'target/site'
$out = Join-Path $PSScriptRoot 'site/clips'
New-Item -ItemType Directory -Force $out | Out-Null
$session = Start-Headless $site $Port $DebugPort $Chrome 'clips'
try {
    foreach ($app in $Apps) {
        $recipe = $Recipes[$app]
        if (-not $recipe) { Write-Warning "$app has no recipe; skipped."; continue }
        $query = 'embed&clip' + $(if ($recipe.Query) { '&' + $recipe.Query } else { '' })
        if (-not (Open-App "http://127.0.0.1:$Port/$app/index.html?$query" '!!(window.oldjavaReplay && window.oldjavaReplay.live())')) {
            Write-Warning "$app did not start recording; skipped."; continue
        }
        $mark = @{ Started = $null }
        Invoke-Steps $recipe.Steps {
            param($kind)
            if ($kind -ne 'rec') { throw "Unknown step $kind" }
            # Twice: the recorder keeps two overlapping recordings once it has run 10 seconds.
            Invoke-Js 'oldjavaReplay.take().then(function () { return oldjavaReplay.take(); }).then(function () { return 1; })' -Await | Out-Null
            $mark.Started = Get-Date
        }
        $started = $mark.Started
        if (-not $started) { throw "$app's recipe has no rec step." }
        $left = $Seconds * 1000 - ((Get-Date) - $started).TotalMilliseconds
        if ($left -gt 0) { Start-Sleep -Milliseconds ([int]$left) }
        $clip = Invoke-Js @'
oldjavaReplay.take().then(function (clip) {
  return new Promise(function (done) {
    var reader = new FileReader();
    reader.onload = function () { done(clip.blob.type + ' ' + reader.result.split(',')[1]); };
    reader.readAsDataURL(clip.blob);
  });
})
'@ -Await
        $type, $data = $clip -split ' ', 2
        if ($type -ne 'video/mp4') { Write-Warning "$app recorded $type, not MP4; skipped."; continue }
        $file = Join-Path $out "$app.mp4"
        [IO.File]::WriteAllBytes($file, [Convert]::FromBase64String($data))
        Write-Host ("{0} -> {1} ({2:N0} KB)" -f $app, $file, ((Get-Item $file).Length / 1KB))
    }
} finally {
    Stop-Headless $session
}
