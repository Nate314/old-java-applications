# Shared by web/shots.ps1, clips.ps1, cards.ps1 and smoke.ps1 (dot-sourced): serves the built site with Python, starts headless Chrome
# with its DevTools port open, and drives one page over the DevTools protocol: load an app page, wait for its app to
# start, then follow a recipe of steps.
#
# Steps: 'wait:ms', 'key:<name>' (a letter, digit, Space, Enter, Escape or an arrow key), 'js:<expression>' run in
# the page, 'click:x,y' and 'rclick:x,y' (a left or right click at that point of the page). A script's own steps
# (clips.ps1's 'rec') go to the -Other block of Invoke-Steps.
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
        if ($reply.id -ne $id) { if ($script:OnEvent) { & $script:OnEvent $reply }; continue }   # an event (smoke.ps1 reads them)
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
    $named = @{ Space = @(' ', 'Space', 32); Enter = @("`r", 'Enter', 13); Escape = @('', 'Escape', 27); Tab = @('', 'Tab', 9)
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

function Send-Click([double]$x, [double]$y, [string]$button = 'left') {
    $buttons = if ($button -eq 'right') { 2 } else { 1 }
    Invoke-Cdp 'Input.dispatchMouseEvent' @{ type = 'mouseMoved'; x = $x; y = $y } | Out-Null
    Invoke-Cdp 'Input.dispatchMouseEvent' @{ type = 'mousePressed'; x = $x; y = $y; button = $button; buttons = $buttons; clickCount = 1 } | Out-Null
    Invoke-Cdp 'Input.dispatchMouseEvent' @{ type = 'mouseReleased'; x = $x; y = $y; button = $button; buttons = 0; clickCount = 1 } | Out-Null
}

# Recipe helpers: click the app's button (or tab) whose text matches, or pick a combo box entry, as a visitor would.
function Click([string]$text) { "js:[].slice.call(document.querySelectorAll('#desktop button')).filter(function (b) { return /$text/.test(b.textContent); })[0].click()" }
function Pick([string]$option) {
    "js:(function (s) { s.value = '$option'; s.dispatchEvent(new Event('change')); })([].slice.call(document.querySelectorAll('#desktop select')).filter(function (s) { return [].some.call(s.options, function (o) { return o.value === '$option'; }); })[0])"
}
# Key events from the page, for keys Send-Key does not name and for held keys: key(type, k, keyCode, t, code) sends one
# keydown or keyup, press(k, keyCode, t, code) both. t defaults to the focused element at each event, code to ''.
$Press = "function key(type, k, c, t, code) { (t || document.activeElement || document.body).dispatchEvent(new KeyboardEvent(type, { key: k, code: code, keyCode: c, which: c, bubbles: true })); } function press(k, c, t, code) { key('keydown', k, c, t, code); key('keyup', k, c, t, code); }"
# Rubik's Cube: makes the turn Teach me asks for next (presses the W/A/S/D key it lit).
$CoachTurn = "js:(function () { $Press var e = [].slice.call(document.querySelectorAll('#desktop span.text')).filter(function (e) { return /^[wasd]$/.test(e.textContent.trim()) && getComputedStyle(e.parentElement).backgroundColor === 'rgb(80, 220, 80)'; })[0]; if (!e) return; var k = e.textContent.trim(); press(k, k.toUpperCase().charCodeAt(0), null, 'Key' + k.toUpperCase()); })()"
# A click at x, y of the page as pointer and mouse events to what is there, for a place only the page can measure.
# Clicks n times (detail 1 to n, as the shim reads a click's count from its detail; Send-Click's clicks are single).
$Tap = "function tap(x, y, n) { var t = document.elementFromPoint(x, y), i; for (i = 1; i <= (n || 1); i++) ['pointerdown', 'mousedown', 'pointerup', 'mouseup', 'click'].forEach(function (type) { var o = { bubbles: true, cancelable: true, clientX: x, clientY: y, button: 0, buttons: /down/.test(type) ? 1 : 0, detail: i, pointerType: 'mouse', isPrimary: true, pointerId: 1 }; t.dispatchEvent(/^pointer/.test(type) ? new PointerEvent(type, o) : new MouseEvent(type, o)); }); }"
# The biggest painted canvas on the desktop: the app's main field.
$Biggest = "[].slice.call(document.querySelectorAll('#desktop canvas.paint')).sort(function (a, b) { return b.width * b.height - a.width * a.height; })[0]"
# Clock's World tab: a click on the strips at an hour of here's day (they span 44% to 97.6% of the panel's width).
function ClockAt([double]$hour) {
    $f = 0.442 + 0.534 * $hour / 24
    "js:(function (c) { $Tap var r = c.getBoundingClientRect(); tap(r.left + r.width * $f, r.top + r.height * 0.62); })($Biggest)"
}
# SpriteFloatyStars Orbits: two more suns, clicked into the biggest window's field (upper left and lower right).
$StarsSuns = "js:(function (c) { $Tap var r = c.getBoundingClientRect(); [[0.27, 0.3], [0.72, 0.7]].forEach(function (p) { tap(r.left + r.width * p[0], r.top + r.height * p[1]); }); })($Biggest)"
function Repeat([int]$n, [string[]]$steps) { $out = @(); for ($i = 0; $i -lt $n; $i++) { $out += $steps }; $out }
# SynthesizerKeyboard's looper: clear any old loop (Delete twice), record a melody layer after the count-in, then a
# bass layer an octave down at the next bar. Keys are held (keydown, then keyup after ms) so notes get their lengths.
function SynthHold([string]$key, [int]$code, [int]$ms) {
    "js:(function (t) { $Press key('keydown', '$key', $code, t); setTimeout(function () { key('keyup', '$key', $code, t); }, $ms); })(document.activeElement || document.body)"
}
$SynthLoop = @('wait:800', 'key:O', (SynthHold 'Delete' 46 20), (SynthHold 'Delete' 46 20), 'wait:200', 'key:Space', 'wait:2450') +
    (@(@('z', 90), @('c', 67), @('b', 66), @('c', 67), @('z', 90), @('c', 67), @('b', 66), @(',', 188)) | ForEach-Object { (SynthHold $_[0] $_[1] 450), 'wait:625' }) +
    @('wait:400', 'key:ArrowLeft', 'key:Space', 'wait:2300') +
    (@(@('z', 90), @('b', 66), @('v', 86), @('c', 67)) | ForEach-Object { (SynthHold $_[0] $_[1] 1100), 'wait:1250' })

function Invoke-Steps([string[]]$steps, [scriptblock]$Other) {
    foreach ($step in $steps) {
        $kind, $arg = $step -split ':', 2
        switch ($kind) {
            'wait' { Start-Sleep -Milliseconds ([int]$arg) }
            'key' { Send-Key $arg }
            'js' { Invoke-Js $arg | Out-Null }
            'click' { $x, $y = $arg -split ','; Send-Click $x $y }
            'rclick' { $x, $y = $arg -split ','; Send-Click $x $y 'right' }
            default { if ($Other) { & $Other $kind $arg } else { throw "Unknown step $step" } }
        }
    }
}

# Starts the site's server and headless Chrome, and connects to Chrome's page. Returns what Stop-Headless needs.
# Chrome's profile is a new folder under ProfileRoot (default %TEMP%) that Stop-Headless deletes.
function Start-Headless([string]$site, [int]$Port, [int]$DebugPort, [string]$Chrome, [string]$name, [int]$Width = 960, [int]$Height = 600,
    [string]$ProfileRoot = [IO.Path]::GetTempPath()) {
    if (-not (Test-Path (Join-Path $site 'index.html'))) { throw "No built site in $site. Run: mvn -f web/pom.xml package" }
    if (-not (Test-Path $Chrome)) { throw "Chrome not found at $Chrome. Pass -Chrome <path>." }
    # http.server with a longer listen queue: its default of 5 refused some of a page's bursts of requests on Windows.
    # A miss answers the site's 404.html with status 404, as GitHub Pages and nginx.conf do (the smoke test's lost page).
    $py = 'import sys, functools, http.server as h; h.ThreadingHTTPServer.request_queue_size = 128; R = h.SimpleHTTPRequestHandler; o = R.send_error; R.send_error = lambda s, c, *a, **k: o(s, c, *a, **k) if c != 404 else (lambda b: (s.send_response(404), s.send_header(''Content-Type'', ''text/html; charset=utf-8''), s.send_header(''Content-Length'', str(len(b))), s.end_headers(), s.command != ''HEAD'' and s.wfile.write(b)))(open(sys.argv[2] + ''/404.html'', ''rb'').read()); h.test(functools.partial(h.SimpleHTTPRequestHandler, directory=sys.argv[2]), h.ThreadingHTTPServer, port=int(sys.argv[1]), bind=''127.0.0.1'')'
    # A server left on the port would answer instead of this site's and the run would test the wrong build.
    $probe = New-Object Net.Sockets.TcpClient
    try { $probe.Connect('127.0.0.1', $Port); throw "Port $Port already answers (a server left running?). Stop it by its PID or pass another -Port." } catch [Net.Sockets.SocketException] { } finally { $probe.Dispose() }
    $server = Start-Process python -ArgumentList "-c `"$py`" $Port `"$site`"" -PassThru -WindowStyle Hidden
    $profileDir = Join-Path $ProfileRoot ("oldjava-$name-$Port-" + [IO.Path]::GetRandomFileName().Substring(0, 8))
    $browser = Start-Process $Chrome -PassThru -ArgumentList '--headless=new', '--disable-gpu', '--hide-scrollbars', '--mute-audio',
        '--autoplay-policy=no-user-gesture-required', "--remote-debugging-port=$DebugPort", "--user-data-dir=$profileDir",
        "--window-size=$Width,$Height", 'about:blank'
    $session = @{ Server = $server; Browser = $browser; Profile = $profileDir }
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
    } catch { Stop-Headless $session; throw }
    $session
}

function Stop-Headless($session) {
    if ($script:ws) { $script:ws.Dispose(); $script:ws = $null }
    # The whole tree: Chrome's helper processes (crashpad, GPU, renderers) can outlive the browser and lock the profile.
    & taskkill.exe /PID $session.Browser.Id /T /F 2>&1 | Out-Null
    Stop-Process -Id $session.Server.Id -ErrorAction SilentlyContinue
    $session.Browser.WaitForExit(5000) | Out-Null
    for ($i = 0; $i -lt 10 -and (Test-Path $session.Profile); $i++) {
        Start-Sleep -Milliseconds 300
        Remove-Item -Recurse -Force $session.Profile -ErrorAction SilentlyContinue
    }
    if (Test-Path $session.Profile) { Write-Warning "Could not remove Chrome's profile $($session.Profile)" }
}

# Loads an app page and waits (up to 20 s) until `ready`, a page expression, is true. Returns whether it was.
function Open-App([string]$url, [string]$ready) {
    Invoke-Cdp 'Page.navigate' @{ url = $url } | Out-Null
    for ($i = 0; $i -lt 100; $i++) {
        Start-Sleep -Milliseconds 200
        try { if (Invoke-Js $ready) { return $true } } catch { }
    }
    $false
}
