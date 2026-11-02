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

# Steps: 'wait:ms', 'key:<name>' (a letter, digit, Space, Enter or an arrow key), 'js:<expression>' run in the page,
# and 'rec', where the kept clip starts. Query is added to the page address.
function Repeat([int]$n, [string[]]$steps) { $out = @(); for ($i = 0; $i -lt $n; $i++) { $out += $steps }; $out }
# Clicks the app's button (or tab) whose text matches, and picks a combo box entry, as a visitor would.
function Click([string]$text) { "js:[].slice.call(document.querySelectorAll('#desktop button')).filter(function (b) { return /$text/.test(b.textContent); })[0].click()" }
function Pick([string]$option) {
    "js:(function (s) { s.value = '$option'; s.dispatchEvent(new Event('change')); })([].slice.call(document.querySelectorAll('#desktop select')).filter(function (s) { return [].some.call(s.options, function (o) { return o.value === '$option'; }); })[0])"
}
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
$Recipes = [ordered]@{
    Minesweeper         = @{ Query = 'watch'; Steps = @('wait:1500', 'rec') }
    FlappyBird          = @{ Steps = @('wait:800', 'key:Space', $FlappyPilot, 'wait:1000', 'rec') }
    DinoGame            = @{ Steps = @('key:E', 'wait:2500', 'rec') }
    UltimateTicTacToe   = @{ Query = 'watch'; Steps = @('wait:2500', 'rec') }
    RubiksCube2D        = @{ Steps = @('wait:800', 'rec') + (Repeat 4 @('key:ArrowLeft', 'wait:500', 'key:W', 'wait:500', 'key:ArrowUp', 'wait:500')) }
    SynthesizerKeyboard = @{ Steps = @('wait:800', 'key:L', 'wait:300', 'rec') }
    Clock               = @{ Steps = @('wait:800', 'rec') }
    SpriteFloatyStars   = @{ Steps = @('wait:800', (Click 'Star Catcher'), 'wait:800', 'key:1', 'wait:1200', 'rec') }
    TankGame            = @{ Query = 'watch'; Steps = @('wait:3000', 'rec') }
    MusicTheoryExpert   = @{ Steps = @('wait:800', (Click 'Songwriter'), 'wait:500', 'key:Space', 'wait:1200', 'rec') }
    CircleSpawner       = @{ Steps = @('wait:2500', 'rec', 'wait:400', (Pick 'Heart'), 'wait:2600', (Pick 'Star')) }
    ScreenSaver         = @{ Steps = @('wait:1500', 'rec') }
    StockChecker        = @{ Steps = @('wait:800', 'key:F', 'wait:1000', 'rec') }
    GroupCreator        = @{ Steps = @('wait:800', 'rec', (Click 'Make groups'), 'wait:1500', 'key:P', 'wait:600', 'key:Space') }
    SimpleAlgorithms    = @{ Steps = @('wait:800', 'rec') }
}
if (-not $Apps) { $Apps = @($Recipes.Keys) }

$site = Join-Path $PSScriptRoot 'target/site'
$out = Join-Path $PSScriptRoot 'site/clips'
if (-not (Test-Path (Join-Path $site 'index.html'))) { throw "No built site in $site. Run: mvn -f web/pom.xml package" }
if (-not (Test-Path $Chrome)) { throw "Chrome not found at $Chrome. Pass -Chrome <path>." }
New-Item -ItemType Directory -Force $out | Out-Null

# ---- a minimal DevTools protocol client: one page, one request at a time ----
$none = [Threading.CancellationToken]::None
$script:ws = $null
$script:next = 0
function Receive-Message {
    $buf = New-Object byte[] 65536
    $ms = New-Object IO.MemoryStream
    do {
        $r = $script:ws.ReceiveAsync([ArraySegment[byte]]::new($buf), $none).GetAwaiter().GetResult()
        $ms.Write($buf, 0, $r.Count)
    } while (-not $r.EndOfMessage)
    [Text.Encoding]::UTF8.GetString($ms.ToArray())
}
function Invoke-Cdp([string]$method, [hashtable]$params = @{}) {
    $script:next++
    $id = $script:next
    $bytes = [Text.Encoding]::UTF8.GetBytes((@{ id = $id; method = $method; params = $params } | ConvertTo-Json -Depth 8 -Compress))
    $script:ws.SendAsync([ArraySegment[byte]]::new($bytes), [Net.WebSockets.WebSocketMessageType]::Text, $true, $none).GetAwaiter().GetResult() | Out-Null
    while ($true) {
        $reply = Receive-Message | ConvertFrom-Json
        if ($reply.id -ne $id) { continue }   # an event
        if ($reply.error) { throw "$method failed: $($reply.error.message)" }
        return $reply.result
    }
}
function Invoke-Js([string]$expression, [switch]$Await) {
    $result = Invoke-Cdp 'Runtime.evaluate' @{ expression = $expression; awaitPromise = [bool]$Await; returnByValue = $true }
    if ($result.exceptionDetails) { throw "Page script failed: $($result.exceptionDetails.exception.description)" }
    $result.result.value
}
function Send-Key([string]$name) {
    $named = @{ Space = @(' ', 'Space', 32); Enter = @("`r", 'Enter', 13)
        ArrowLeft = @('', 'ArrowLeft', 37); ArrowUp = @('', 'ArrowUp', 38); ArrowRight = @('', 'ArrowRight', 39); ArrowDown = @('', 'ArrowDown', 40) }
    if ($named.ContainsKey($name)) { $text, $code, $vk = $named[$name]; $key = if ($text -eq ' ') { ' ' } else { $name } }
    else {
        $c = [string]$name[0]
        $text = $c.ToLower(); $key = $text; $vk = [int][char]$c.ToUpper()
        $code = if ($c -match '\d') { "Digit$c" } else { "Key$($c.ToUpper())" }
    }
    $down = @{ type = 'keyDown'; key = $key; code = $code; windowsVirtualKeyCode = $vk; nativeVirtualKeyCode = $vk }
    if ($text) { $down.text = $text }
    Invoke-Cdp 'Input.dispatchKeyEvent' $down | Out-Null
    Invoke-Cdp 'Input.dispatchKeyEvent' @{ type = 'keyUp'; key = $key; code = $code; windowsVirtualKeyCode = $vk; nativeVirtualKeyCode = $vk } | Out-Null
}

$server = Start-Process python -ArgumentList '-m', 'http.server', $Port, '--bind', '127.0.0.1', '--directory', $site -PassThru -WindowStyle Hidden
$profileDir = Join-Path ([IO.Path]::GetTempPath()) "oldjava-clips-$Port"
$browser = Start-Process $Chrome -PassThru -ArgumentList '--headless=new', '--disable-gpu', '--hide-scrollbars', '--mute-audio',
    '--autoplay-policy=no-user-gesture-required', "--remote-debugging-port=$DebugPort", "--user-data-dir=$profileDir",
    '--window-size=960,600', 'about:blank'
try {
    $page = $null
    for ($i = 0; $i -lt 50 -and -not $page; $i++) {
        Start-Sleep -Milliseconds 200
        try {
            $targets = Invoke-RestMethod "http://127.0.0.1:$DebugPort/json/list"
            $page = $targets | ForEach-Object { $_ } | Where-Object { $_.type -eq 'page' } | Select-Object -First 1
        } catch { }
    }
    if (-not $page) { throw "Chrome's DevTools port $DebugPort did not answer." }
    $script:ws = New-Object Net.WebSockets.ClientWebSocket
    $script:ws.ConnectAsync([Uri]$page.webSocketDebuggerUrl, $none).GetAwaiter().GetResult() | Out-Null

    foreach ($app in $Apps) {
        $recipe = $Recipes[$app]
        if (-not $recipe) { Write-Warning "$app has no recipe; skipped."; continue }
        $query = 'embed&clip' + $(if ($recipe.Query) { '&' + $recipe.Query } else { '' })
        Invoke-Cdp 'Page.navigate' @{ url = "http://127.0.0.1:$Port/$app/index.html?$query" } | Out-Null
        $live = $false
        for ($i = 0; $i -lt 100 -and -not $live; $i++) {
            Start-Sleep -Milliseconds 200
            try { $live = Invoke-Js '!!(window.oldjavaReplay && window.oldjavaReplay.live())' } catch { }
        }
        if (-not $live) { Write-Warning "$app did not start recording; skipped."; continue }
        $started = $null
        foreach ($step in $recipe.Steps) {
            $kind, $arg = $step -split ':', 2
            switch ($kind) {
                'wait' { Start-Sleep -Milliseconds ([int]$arg) }
                'key' { Send-Key $arg }
                'js' { Invoke-Js $arg | Out-Null }
                'rec' {
                    # Twice: the recorder keeps two overlapping recordings once it has run 10 seconds.
                    Invoke-Js 'oldjavaReplay.take().then(function () { return oldjavaReplay.take(); }).then(function () { return 1; })' -Await | Out-Null
                    $started = Get-Date
                }
            }
        }
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
    if ($script:ws) { $script:ws.Dispose() }
    Stop-Process -Id $browser.Id -ErrorAction SilentlyContinue
    Stop-Process -Id $server.Id -ErrorAction SilentlyContinue
    Start-Sleep -Milliseconds 500
    Remove-Item -Recurse -Force $profileDir -ErrorAction SilentlyContinue
}
