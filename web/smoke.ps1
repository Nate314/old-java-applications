# A browser smoke test of the built site in headless Chrome over the DevTools protocol (web/cdp.ps1): the pages
# visitors land on (home, story, play.html with and without ?app=, the lost page for a missing path, compare.html) and,
# for every app in the build's apps.json, its page, its ?embed and ?embed&full forms and its Then page where it has
# one. On an app it presses Play and waits for the app to paint. It prints one PASS or FAIL line per page with its
# time. A page fails on an uncaught error, a console error, a failed or 4xx/5xx request, no window, or an app whose
# biggest canvas is one flat color. Exits 1 when any page fails.
# Sound is off (--mute-audio, __shimSynth.noteOn stubbed, new AudioContexts suspended), the clipboard and
# alert/confirm/prompt are stubbed, Chrome's profile is a throwaway folder under -ProfileRoot, and the server and
# Chrome are stopped by PID. Build first (mvn -f web/pom.xml package), then run from the repo root:
#   ./web/smoke.ps1
#   ./web/smoke.ps1 -Apps Minesweeper,Clock -Port 8107 -DebugPort 9307 -ProfileRoot C:\some\scratch
param(
    [string[]]$Apps,
    [int]$Port = 8101,
    [int]$DebugPort = 9302,
    [string]$Chrome = "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
    [string]$Site = (Join-Path $PSScriptRoot 'target/site'),
    [string]$ProfileRoot = [IO.Path]::GetTempPath(),   # a folder (made if missing) for Chrome's throwaway profile
    [int]$Settle = 2000   # ms an app runs after its first window before the paint check
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'cdp.ps1')
# Its 8.3 short form: with a long folder, Chrome's cache files pass Windows' 260 character limit and CacheStorage fails.
New-Item -ItemType Directory -Force $ProfileRoot | Out-Null   # a scratch folder that is not there yet
$ProfileRoot = (New-Object -ComObject Scripting.FileSystemObject).GetFolder($ProfileRoot).ShortPath
if (-not $Apps) { $Apps = @(Get-Content -Raw (Join-Path $Site 'apps.json') | ConvertFrom-Json | ForEach-Object { $_.id }) }

# Runs in every page before its own scripts.
$Guard = @'
(function () {
  var s; Object.defineProperty(window, '__shimSynth', { configurable: true, get: function () { return s; },
    set: function (v) { if (v) v.noteOn = function () {}; s = v; } });
  ['AudioContext', 'webkitAudioContext'].forEach(function (n) {
    var A = window[n]; if (!A) return;
    window[n] = function (o) { var c = new A(o); c.suspend(); c.resume = function () { return Promise.resolve(); }; return c; };
    window[n].prototype = A.prototype;
  });
  var ok = function () { return Promise.resolve(); };
  try { Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText: ok, write: ok, readText: function () { return Promise.resolve(''); }, read: function () { return Promise.resolve([]); } } }); } catch (e) {}
  document.execCommand = function () { return false; };
  ['alert', 'confirm', 'prompt'].forEach(function (n) { window[n] = function () { console.error('smoke: page called ' + n + '()'); return null; }; });
})();
'@

$script:Errors = New-Object Collections.Generic.List[string]
$script:OnEvent = {
    param($e)
    $p = $e.params
    $line = switch ($e.method) {
        'Runtime.exceptionThrown' { 'uncaught: ' + $(if ($p.exceptionDetails.exception.description) { $p.exceptionDetails.exception.description } else { $p.exceptionDetails.text }) }
        'Runtime.consoleAPICalled' { if ($p.type -in 'error', 'assert') { 'console.error: ' + (($p.args | ForEach-Object { if ($null -ne $_.value) { $_.value } else { $_.description } }) -join ' ') } }
        'Log.entryAdded' { if ($p.entry.level -eq 'error' -and $p.entry.source -ne 'network') { 'log: ' + $p.entry.text } }
        'Network.loadingFailed' { if (-not $p.canceled) { "request failed: $($p.errorText) ($($p.type)) $($script:Urls[$p.requestId])" } }
        'Network.requestWillBeSent' { $script:Urls[$p.requestId] = $p.request.url }
        'Network.responseReceived' { if ($p.response.status -ge 400) { "HTTP $($p.response.status): $($p.response.url)" } }
    }
    if ($line) { $script:Errors.Add(($line -split "`n")[0]) }
}
$script:Urls = @{}

# The app's first window is up (Windows: its pretend desktop shell; a console program, SimpleAlgorithms' Then: its
# first printed line).
$Up = "!!document.querySelector('#desktop > *, .shim-shell > *, .shim-console:not(:empty)')"
# What the app painted: its biggest canvas's distinct colors (sampled), or -1 when it has no canvas.
$Paint = "(function (c) { if (!c) return -1; var d = c.getContext('2d').getImageData(0, 0, c.width, c.height).data, seen = {}, n = 0, i; for (i = 0; i < d.length && n < 2; i += 4 * 7) { var k = d[i] + ',' + d[i + 1] + ',' + d[i + 2] + ',' + d[i + 3]; if (!seen[k]) { seen[k] = 1; n++; } } return n; })($Biggest)"

# One page: loads it, waits until `ready` (a selector) is on it, or for an app (-App) presses Play when there is
# one and checks the window and its paint. -Click presses that button first and -Frame is the iframe the app is in
# (play.html's player); -Missing expects the address itself to answer 404 (the lost page).
function Test-Page([string]$name, [string]$path, [string]$ready, [switch]$App, [string]$Click, [string]$Frame, [switch]$Missing) {
    $t = [Diagnostics.Stopwatch]::StartNew()
    Invoke-Cdp 'Page.navigate' @{ url = 'about:blank' } | Out-Null
    Invoke-Js '1' | Out-Null   # reads the last page's late events, then forgets them
    $script:Errors.Clear(); $script:Urls.Clear()
    $why = $null
    $url = "http://127.0.0.1:$Port/$path"
    Invoke-Cdp 'Page.navigate' @{ url = $url } | Out-Null
    # An expression about the app's document: the page's own, or its player frame's.
    $in = { param($expr) if ($Frame) { "(function (document) { return $expr; })(document.querySelector('$Frame').contentDocument)" } else { $expr } }
    $wait = { param($expr, [int]$ms) for ($i = 0; $i -lt $ms / 200; $i++) { Start-Sleep -Milliseconds 200; try { if (Invoke-Js $expr) { return $true } } catch { } }; $false }
    if (-not $App) {
        if (-not (& $wait "document.readyState === 'complete' && !!document.querySelector('$ready')" 20000)) { $why = "page did not finish loading ($ready)" }
        else { Start-Sleep -Milliseconds $Settle }
    } elseif ($Click -and -not (& $wait "(function (b) { if (b) b.click(); return !!b; })(document.querySelector('$Click'))" 20000)) { $why = "no $Click to press" }
    elseif (-not (& $wait (& $in "!!(document.querySelector('.shim-title-play:not([disabled])') || document.querySelector('#desktop > *'))") 30000)) { $why = 'Play never became ready' }
    else {
        Invoke-Js (& $in "(function (b) { if (b) b.click(); })(document.querySelector('.shim-title-play'))") | Out-Null
        if (-not (& $wait (& $in $Up) 20000)) { $why = 'no window after Play' }
        else {
            Start-Sleep -Milliseconds $Settle
            # Up to 5 s more for an app that starts on a flat screen and draws a little at a time (ScreenSaver's Then
            # adds one circle to the black every 250 ms).
            if (-not (& $wait "$(& $in $Paint) !== 1" 5000)) { $why = 'blank canvas (one flat color)' }
        }
    }
    Invoke-Js '1' | Out-Null   # reads the events that came during the waits
    # Chrome asks for /favicon.ico by itself on a page with no icon link (the lost page); a miss there is not the page's.
    $errors = $script:Errors | Select-Object -Unique | Where-Object { -not ($Missing -and $_ -eq "HTTP 404: $url") -and $_ -notmatch '^HTTP 404: \S+/favicon\.ico$' }
    if ($errors) { $why = (@($why) + $errors | Where-Object { $_ }) -join ' | ' }
    $line = '{0,-4} {1,-32} {2,5:0.0} s' -f $(if ($why) { 'FAIL' } else { 'PASS' }), $name, $t.Elapsed.TotalSeconds
    if ($why) { Write-Host "$line  $why" -ForegroundColor Red } else { Write-Host $line }
    -not $why
}

# The pages visitors land on, then each app's page, its embedded forms (another site's widget, and filling it) and its
# Then page where it has one.
$pages = @(
    @{ name = 'home page'; path = 'index.html'; ready = '#player-frame' }
    @{ name = 'story'; path = 'story.html'; ready = 'main' }
    @{ name = 'play.html (no app)'; path = 'play.html'; ready = '#oops:not([hidden])' }
    @{ name = "play.html?app=$($Apps[0])"; path = "play.html?app=$($Apps[0])"; App = $true; Click = '#poster-play'; Frame = '#frame' }
    @{ name = 'lost page (missing path)'; path = 'no-such-page'; ready = '#lost'; Missing = $true }
    @{ name = 'compare.html'; path = 'compare.html'; ready = 'main.compare' }
)
foreach ($app in $Apps) {
    $pages += @{ name = $app; path = "$app/index.html"; App = $true }
    $pages += @{ name = "$app ?embed"; path = "$app/index.html?embed"; App = $true }
    $pages += @{ name = "$app ?embed&full"; path = "$app/index.html?embed&full"; App = $true }
    if (Test-Path (Join-Path $Site "$app/then.html")) { $pages += @{ name = "$app Then"; path = "$app/then.html"; App = $true } }
}

$total = [Diagnostics.Stopwatch]::StartNew()
$session = Start-Headless $Site $Port $DebugPort $Chrome 'smoke' -ProfileRoot $ProfileRoot
$failed = 0
try {
    foreach ($m in 'Page.enable', 'Runtime.enable', 'Log.enable', 'Network.enable') { Invoke-Cdp $m | Out-Null }
    Invoke-Cdp 'Page.addScriptToEvaluateOnNewDocument' @{ source = $Guard } | Out-Null
    # Python's server can still be starting; a first page against it would fail on that alone.
    for ($i = 0; $i -lt 50; $i++) { try { Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:$Port/index.html" | Out-Null; break } catch { Start-Sleep -Milliseconds 200 } }
    foreach ($p in $pages) { if (-not (Test-Page @p)) { $failed++ } }
} finally {
    $script:OnEvent = $null
    Stop-Headless $session
}
Write-Host ('{0} of {1} pages passed in {2:0} s' -f ($pages.Count - $failed), $pages.Count, $total.Elapsed.TotalSeconds)
if ($failed) { exit 1 }
