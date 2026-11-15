# Records the home page's preview clips (web/public/clips/<App>.mp4): a few seconds of each app running, which the
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
# does something) have no clip; their cards keep their still thumbnail. A card plays a clip when its app has
# "clip": true in web/src/shared/apps.json (its gallery card then has data-clip).
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
# Flappy Bird has no computer player, so the clip has a small one: every 16 ms it finds the bird (teal) and the next
# pipe (copper, from the top edge of the game's canvas; the copper ground is below the band searched) and flaps
# while the bird is below the gap's middle.
$FlappyPilot = @'
js:(function () {
  var c = document.querySelector('#desktop canvas'), g = c.getContext('2d'), last = 0, prev = 0;
  function flap() { ['keydown', 'keyup'].forEach(function (t) { document.dispatchEvent(new KeyboardEvent(t, { key: ' ', code: 'Space', keyCode: 32, which: 32, bubbles: true })); }); }
  function pipe(d, i) { return d[i] > d[i + 1] + 40 && d[i + 1] > d[i + 2] + 15 && d[i] > 110; }
  function tick() {
    var W = c.width, H = c.height, x0 = Math.round(W * 0.2), bw = Math.round(W * 0.14), bh = Math.round(H * 0.87);
    var band = g.getImageData(x0, 0, bw, bh).data, n = 0, sy = 0, sx = 0;
    for (var y = 0; y < bh; y += 2) for (var x = 0; x < bw; x += 2) {
      var i = (y * bw + x) * 4;
      if (band[i + 1] > band[i] + 80 && band[i + 2] > band[i] + 60 && band[i + 1] > 120) { n++; sy += y; sx += x; }
    }
    if (n < 3) return;
    var by = sy / n, bx = x0 + sx / n, row = g.getImageData(0, 3, W, 1).data, p0 = -1, p1 = W;
    for (var x = Math.max(0, Math.round(bx - W * 0.035)); x < W; x++) {
      var on = pipe(row, x * 4);
      if (on && p0 < 0) p0 = x;
      if (!on && p0 >= 0) { p1 = x; break; }
    }
    var target = H * 0.45;
    if (p0 >= 0 && p1 < W) {
      // The gap is the longest run without pipe down the pipe's middle (rivets and rims break the copper up).
      var col = g.getImageData(Math.round((p0 + p1) / 2), 0, 1, bh).data, run = 0, best = 0, end = 0;
      for (var y = 0; y < bh; y++) if (pipe(col, y * 4)) run = 0; else if (++run > best) { best = run; end = y; }
      target = end - best / 2 + H * 0.06;
    }
    var t = performance.now();
    var fall = by - prev;
    prev = by;
    if (by + 2 * fall > target && t - last > 180) { flap(); last = t; }
  }
  setInterval(tick, 16); // not requestAnimationFrame: it can run far slower than the game's own timers
})()
'@
$Recipes = [ordered]@{
    Minesweeper         = @{ Steps = @('wait:800', 'key:O', 'key:W', 'wait:1500', 'rec') }
    FlappyBird          = @{ Steps = @('wait:800', 'key:Space', $FlappyPilot, 'wait:3000', 'rec') }
    DinoGame            = @{ Steps = @('key:E', 'wait:2500', 'rec') }
    UltimateTicTacToe   = @{ Query = 'watch'; Steps = @('wait:2500', 'rec') }
    RubiksCube2D        = @{ Steps = @('wait:800', 'key:G', 'key:2', 'wait:900', 'rec', 'key:2') }
    SynthesizerKeyboard = @{ Steps = $SynthLoop + @('wait:600', 'rec') }
    Clock               = @{ Steps = @('wait:800', 'key:W', 'wait:300', (ClockAt 6), 'wait:300', 'rec') + (Repeat 10 @('wait:550', 'key:ArrowRight')) }
    SpriteFloatyStars   = @{ Steps = @('wait:800', (Pick 'Mode 6'), (Click '^OK$'), 'wait:3000', $StarsSuns, 'wait:7000', 'rec') }
    TankGame            = @{ Query = 'watch'; Steps = @('wait:3000', 'rec') }
    MusicTheoryExpert   = @{ Steps = @('wait:800', (Click 'Songwriter'), 'wait:500', 'key:Space', 'wait:1200', 'rec') }
    CircleSpawner       = @{ Steps = @('wait:2500', 'rec', 'wait:400', (Pick 'Heart'), 'wait:1600', (Click '^Spin$')) }
    ScreenSaver         = @{ Steps = @('wait:1500', 'key:9', 'wait:2500', 'rec', 'wait:1200', 'key:F') }
    StockChecker        = @{ Steps = @('wait:800', 'key:F', 'wait:1000', 'key:B', 'key:-', 'key:=', 'rec') }
    GroupCreator        = @{ Steps = @('wait:800', 'rec', (Click 'Make groups'), 'wait:1500', 'key:W', 'wait:1200', 'key:P', 'wait:600', 'key:Space') }
    SimpleAlgorithms    = @{ Steps = @('wait:800', 'key:5', 'rec') }
}
if (-not $Apps) { $Apps = @($Recipes.Keys) }

$site = Join-Path $PSScriptRoot 'target/site'
$out = Join-Path $PSScriptRoot 'public/clips'
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
