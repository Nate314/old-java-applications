# A browser smoke test of the built site in headless Chrome over the DevTools protocol (web/cdp.ps1): the pages
# visitors land on (home, story, play.html with and without ?app=, the lost page for a missing path, compare.html) and,
# for every app in the build's apps.json, its page, its ?embed and ?embed&full forms, its page again on a 400x800 phone
# screen with touch emulation (where the band must offer its Keyboard button and the touch controller must show on the
# apps that have one, without the buttons the app draws itself) and its Then page where it has one. On an app it presses Play and waits for the app to paint. It prints one PASS or FAIL line per page with its
# time. A page fails on an uncaught error, a console error, a failed or 4xx/5xx request, no window, an app whose
# biggest canvas is one flat color, or more controls without an accessible name than it is known to have. On each app's own page it then closes the app's windows (frontmost first) until the
# exit screen shows, checks it (Run again focused, other apps offered) and presses Run again, which must bring the app
# back without its title screen. On the story and the lost page it also opens an app from a link far down the page in
# the play sheet (PlayHere.tsx), checks it as above, closes the sheet each way a visitor can (Back, Esc, a click
# around it, its close button; Forward and the link open it again) and checks the link is where it was. Exits 1
# when any page fails.
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

# The frontmost shown window's own close box, pressed: 'exit' once the exit screen is up, 'none' when no window can be
# closed. Windows with a title bar go first; a borderless window's box is hidden but still closes it as the desktop's
# window manager would (NathaNOS, ScreenSaver, the Windows key; CircleSpawner's circles only after its Circle Creator).
# With no window left it closes the Start menu, since pretend Windows holds the exit while it is open.
$Close = "(function () { if (document.querySelector('.shim-exit')) return 'exit'; var rank = function (w) { return (w.querySelector(':scope > .titlebar').getClientRects().length ? 1e9 : 0) + (+getComputedStyle(w).zIndex || 0); }; var w = [].slice.call(document.querySelectorAll('#desktop > *')).filter(function (w) { return w.getClientRects().length && w.querySelector(':scope > .titlebar:not(.no-close) .close'); }).sort(function (x, y) { return rank(x) - rank(y); }).pop(); if (!w) { w = document.querySelector('.shim-start-power'); if (!w) return 'none'; w.click(); return 'closed'; } w.querySelector(':scope > .titlebar .close').click(); return 'closed'; })()"

# Controls a screen reader announces with no name (an empty accessible name in Chrome's accessibility tree), in the
# page and the frames on it, as "role <tag.class>". Shown (not hidden) ones only; the shim's widgets count too.
$Controls = '^(button|checkbox|combobox|link|listbox|menuitem|menuitemcheckbox|menuitemradio|radio|searchbox|slider|spinbutton|switch|tab|textbox|treeitem)$'
function Get-Unnamed {
    $frames = New-Object Collections.Generic.List[object]
    $frames.Add((Invoke-Cdp 'Page.getFrameTree').frameTree)
    for ($i = 0; $i -lt $frames.Count; $i++) {
        foreach ($c in @($frames[$i].childFrames)) { if ($c) { $frames.Add($c) } }
        foreach ($n in (Invoke-Cdp 'Accessibility.getFullAXTree' @{ frameId = $frames[$i].frame.id }).nodes) {
            if ($n.ignored -or $n.role.value -notmatch $Controls -or "$($n.name.value)".Trim()) { continue }
            $d = (Invoke-Cdp 'DOM.describeNode' @{ backendNodeId = $n.backendDOMNodeId }).node
            $k = [array]::IndexOf(@($d.attributes), 'class')
            "$($n.role.value) <$($d.localName)$(if ($k -ge 0) { '.' + ($d.attributes[$k + 1] -replace '\s+', '.') })>"
        }
    }
}
# Unnamed controls each page had when the check came in (round 68, 960x600); a page fails with more, so a new one
# must get a name (aria-label, a label, setLabelFor in the shim). Lower a number when one of them gets its name.
# Since round 68's shim names (NearLabel, grid keys) every page has 0, so none is listed.
$KnownUnnamed = @{}

# One page: loads it, waits until `ready` (a selector) is on it, or for an app (-App) presses Play when there is
# one and checks the window and its paint. -Click presses that button first and -Frame is the iframe the app is in
# (play.html's player); -Exit closes the app and runs it again (see the top); -Missing expects the address itself to
# answer 404 (the lost page); -Sheet is the page's app links: the last one opens the app in the play sheet (see the top);
# -Phone loads it in a 400x800 phone screen with a finger for a pointer, so apps that pick a narrow layout at start are
# checked in it too, and checks its touch extras (Test-Touch).
function Test-Page([string]$name, [string]$path, [string]$ready, [switch]$App, [string]$Click, [string]$Frame, [switch]$Missing, [switch]$Exit, [string]$Sheet, [switch]$Phone) {
    $t = [Diagnostics.Stopwatch]::StartNew()
    Invoke-Cdp 'Page.navigate' @{ url = 'about:blank' } | Out-Null
    Invoke-Js '1' | Out-Null   # reads the last page's late events, then forgets them
    if ($Phone) { Invoke-Cdp 'Emulation.setDeviceMetricsOverride' @{ width = 400; height = 800; deviceScaleFactor = 1; mobile = $true } | Out-Null }
    else { Invoke-Cdp 'Emulation.clearDeviceMetricsOverride' | Out-Null }
    # A phone's finger: (pointer: coarse) matches, so the page offers its Keyboard button and touch controller.
    Invoke-Cdp 'Emulation.setTouchEmulationEnabled' @{ enabled = [bool]$Phone; maxTouchPoints = 5 } | Out-Null
    $script:Errors.Clear(); $script:Urls.Clear()
    $why = $null
    $url = "http://127.0.0.1:$Port/$path"
    Invoke-Cdp 'Page.navigate' @{ url = $url } | Out-Null
    # An expression about the app's document: the page's own, or its player frame's.
    $in = { param($expr) if ($Frame) { "(function (document) { return $expr; })(document.querySelector('$Frame').contentDocument)" } else { $expr } }
    $wait = { param($expr, [int]$ms) for ($i = 0; $i -lt $ms / 200; $i++) { Start-Sleep -Milliseconds 200; try { if (Invoke-Js $expr) { return $true } } catch { } }; $false }
    # The sheet's link is scrolled to the middle of the window and clicked; its place in the window is kept for the close.
    if ($Sheet) { $Frame = '.play-here[open] iframe'; $pressFirst = "(function (l) { var b = l[l.length - 1]; if (b) { b.scrollIntoView({ block: 'center', behavior: 'instant' }); window.__smokeLink = b; window.__smokeTop = b.getBoundingClientRect().top; b.click(); } return !!b; })(document.querySelectorAll('$Sheet'))" }
    elseif ($Click) { $pressFirst = "(function (b) { if (b) b.click(); return !!b; })(document.querySelector('$Click'))" }
    if (-not $App) {
        if (-not (& $wait "document.readyState === 'complete' && !!document.querySelector('$ready')" 20000)) { $why = "page did not finish loading ($ready)" }
        else { Start-Sleep -Milliseconds $Settle }
    } elseif ($pressFirst -and -not (& $wait $pressFirst 20000)) { $why = "no $Click$Sheet to press" }
    elseif (-not (& $wait (& $in "!!(document.querySelector('.shim-title-play:not([disabled])') || document.querySelector('#desktop > *'))") 30000)) { $why = 'Play never became ready' }
    else {
        Invoke-Js (& $in "(function (b) { if (b) b.click(); })(document.querySelector('.shim-title-play'))") | Out-Null
        if (-not (& $wait (& $in $Up) 20000)) { $why = 'no window after Play' }
        else {
            Start-Sleep -Milliseconds $Settle
            # Up to 5 s more for an app that starts on a flat screen and draws a little at a time (ScreenSaver's Then
            # adds one circle to the black every 250 ms).
            if (-not (& $wait "$(& $in $Paint) !== 1" 5000)) { $why = 'blank canvas (one flat color)' }
            elseif ($Exit) { $why = Test-Exit $wait }
            elseif ($Sheet) { $why = Test-SheetClose $wait }
            elseif ($Phone) { $why = Test-Touch (($path -split '/')[0]) }
        }
    }
    $note = ''
    if (-not $why) {
        $unnamed = @(Get-Unnamed)
        $known = [int]$KnownUnnamed[$name]
        if ($unnamed.Count -gt $known) { $why = "$($unnamed.Count) unnamed controls (known: $known): " + (($unnamed | Group-Object | ForEach-Object { "$($_.Count) x $($_.Name)" }) -join ', ') }
        elseif ($unnamed.Count -lt $known) { $note = "  ($($unnamed.Count) unnamed controls, $known known: lower its `$KnownUnnamed)" }
    }
    Invoke-Js '1' | Out-Null   # reads the events that came during the waits
    # Chrome asks for /favicon.ico by itself on a page with no icon link (the lost page); a miss there is not the page's.
    $errors = $script:Errors | Select-Object -Unique | Where-Object { -not ($Missing -and $_ -eq "HTTP 404: $url") -and $_ -notmatch '^HTTP 404: \S+/favicon\.ico$' }
    if ($errors) { $why = (@($why) + $errors | Where-Object { $_ }) -join ' | ' }
    $line = '{0,-4} {1,-32} {2,5:0.0} s' -f $(if ($why) { 'FAIL' } else { 'PASS' }), $name, $t.Elapsed.TotalSeconds
    if ($why) { Write-Host "$line  $why" -ForegroundColor Red } else { Write-Host "$line$note" }
    -not $why
}

# The pages visitors land on, then each app's page, its embedded forms (another site's widget, and filling it) and its
# Then page where it has one.
$pages = @(
    @{ name = 'home page'; path = 'index.html'; ready = '#player-frame' }
    @{ name = 'story'; path = 'story.html'; ready = 'main' }
    # No ?app=: the tile picker of every app; its last tile plays that app right there.
    @{ name = 'play.html (no app, picker)'; path = 'play.html'; App = $true; Click = '#oops:not([hidden]) .pick li:last-child button'; Frame = '#frame' }
    @{ name = "play.html?app=$($Apps[0])"; path = "play.html?app=$($Apps[0])"; App = $true; Click = '#poster-play'; Frame = '#frame' }
    @{ name = 'lost page (missing path)'; path = 'no-such-page'; ready = '#lost'; Missing = $true }
    @{ name = 'compare.html'; path = 'compare.html'; ready = 'main.compare .panes' }
    @{ name = 'story play sheet'; path = 'story.html'; App = $true; Sheet = '#apps20 a' }
    @{ name = 'lost page play sheet'; path = 'no-such-page'; App = $true; Sheet = '#cards a.card'; Missing = $true }
)
foreach ($app in $Apps) {
    $pages += @{ name = $app; path = "$app/index.html"; App = $true; Exit = $true }
    $pages += @{ name = "$app ?embed"; path = "$app/index.html?embed"; App = $true }
    $pages += @{ name = "$app ?embed&full"; path = "$app/index.html?embed&full"; App = $true }
    $pages += @{ name = "$app at 400 px"; path = "$app/index.html"; App = $true; Phone = $true }
    if (Test-Path (Join-Path $Site "$app/then.html")) { $pages += @{ name = "$app Then"; path = "$app/then.html"; App = $true } }
}

# The app's exit and Run again, on its page with the app running; returns why it failed, or nothing.
function Test-Exit($wait) {
    for ($i = 0; $i -lt 8; $i++) {   # an app may close in steps (a game window, then its menu)
        $r = Invoke-Js $Close
        if ($r -ne 'closed') { break }
        if (& $wait "!!document.querySelector('.shim-exit')" 1500) { $r = 'exit'; break }
    }
    if ($r -ne 'exit') { return "exit: no exit screen after closing the app's windows ($r)" }
    if (-not (& $wait "document.querySelectorAll('.shim-exit .shim-picks a').length > 0" 5000)) { return 'exit: no other apps offered' }
    if (-not (Invoke-Js "document.activeElement === document.querySelector('.shim-exit > button')")) { return 'exit: Run again is not focused' }
    Invoke-Js "document.querySelector('.shim-exit > button').click()" | Out-Null
    if (-not (& $wait "!document.querySelector('.shim-exit, .shim-title-play') && $Up" 30000)) { return 'exit: Run again did not bring the app back without its title screen' }
}

# Controller buttons an app draws itself on a 400 px screen, so its touch controller must leave them out (ownBelow in
# runtime/touch-pad.tsx).
$OwnOnPhone = @{ DinoGame = 'Jump', 'Duck' }

# The phone page's touch extras, with the app running: the band's Keyboard button, and the touch controller exactly on
# the apps that have one (window.oldjavaPad.has), without the buttons the app draws itself. Returns why it failed, or nothing.
function Test-Touch([string]$app) {
    if (-not (Invoke-Js "matchMedia('(pointer: coarse)').matches")) { return 'touch: (pointer: coarse) does not match under touch emulation' }
    if (-not (Invoke-Js "(function (b) { return !!b && b.getClientRects().length > 0; })(document.querySelector('.shim-keys-open'))")) { return 'touch: no Keyboard button in the band' }
    $has = Invoke-Js "window.oldjavaPad.has('$app')"
    $shown = Invoke-Js "(function (p) { return !!p && !p.hidden && p.getClientRects().length > 0; })(document.querySelector('.shim-pad'))"
    if ($has -ne $shown) { return "touch: the controller is $(if ($shown) { 'shown' } else { 'not shown' }), expected $(if ($has) { 'shown' } else { 'none' })" }
    $labels = @(Invoke-Js "[].map.call(document.querySelectorAll('.shim-pad .shim-pad-btn > span'), function (s) { return s.textContent; })")
    $twice = @($OwnOnPhone[$app] | Where-Object { $labels -contains $_ })
    if ($twice) { return "touch: the controller repeats the app's own $($twice -join ', ')" }
}

# Every way out of the play sheet, on a page with an app playing in it: Back (then Forward opens it again), Esc from
# its bar, a click on the page around it, and its close button, each reopened from the link (or Forward) in between.
# Each must close the sheet and its app, take #play= off the address and leave the link where it was. Returns why it
# failed, or nothing.
function Test-SheetClose($wait) {
    if (-not (Invoke-Js 'scrollY > 0')) { return 'sheet: the link was not below the fold, so the scroll was not tested' }
    $open = "!!document.querySelector('.play-here[open] iframe') && /^#play=/.test(location.hash)"
    $ways = [ordered]@{
        'Back' = { Invoke-Js 'history.back()' | Out-Null }
        'Esc' = { Invoke-Js "document.querySelector('.play-here-bar .btn.primary').focus()" | Out-Null; Send-Key Escape }
        'a click around it' = { Send-Click 4 300 }   # the window is 960 wide, the sheet at most 100vw - 32px
        'its close button' = { Invoke-Js "document.querySelector('.play-here-bar .btn.primary').click()" | Out-Null }
    }
    $reopen = @{ 'Back' = 'history.forward()'; 'Esc' = 'window.__smokeLink.click()'; 'a click around it' = 'window.__smokeLink.click()' }
    foreach ($way in $ways.Keys) {
        if (-not (& $wait $open 5000)) { return "sheet: not open with #play= in the address before $way" }
        & $ways[$way]
        if (-not (& $wait "!document.querySelector('.play-here[open], .play-here iframe') && !location.hash" 5000)) { return "sheet: $way did not close it, left its app running or left #play= in the address" }
        $moved = Invoke-Js 'Math.round(window.__smokeLink.getBoundingClientRect().top - window.__smokeTop)'
        if ($moved) { return "sheet: the page moved $moved px under it ($way)" }
        if ($reopen[$way]) { Invoke-Js $reopen[$way] | Out-Null }
    }
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
