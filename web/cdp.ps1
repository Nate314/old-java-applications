# Shared by web/shots.ps1 and web/clips.ps1 (dot-sourced): serves the built site with Python, starts headless Chrome
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
    $named = @{ Space = @(' ', 'Space', 32); Enter = @("`r", 'Enter', 13); Escape = @('', 'Escape', 27)
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
function Repeat([int]$n, [string[]]$steps) { $out = @(); for ($i = 0; $i -lt $n; $i++) { $out += $steps }; $out }

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
function Start-Headless([string]$site, [int]$Port, [int]$DebugPort, [string]$Chrome, [string]$name, [int]$Width = 960, [int]$Height = 600) {
    if (-not (Test-Path (Join-Path $site 'index.html'))) { throw "No built site in $site. Run: mvn -f web/pom.xml package" }
    if (-not (Test-Path $Chrome)) { throw "Chrome not found at $Chrome. Pass -Chrome <path>." }
    $server = Start-Process python -ArgumentList '-m', 'http.server', $Port, '--bind', '127.0.0.1', '--directory', $site -PassThru -WindowStyle Hidden
    $profileDir = Join-Path ([IO.Path]::GetTempPath()) "oldjava-$name-$Port"
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
    Stop-Process -Id $session.Browser.Id -ErrorAction SilentlyContinue
    Stop-Process -Id $session.Server.Id -ErrorAction SilentlyContinue
    Start-Sleep -Milliseconds 500
    Remove-Item -Recurse -Force $session.Profile -ErrorAction SilentlyContinue
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
