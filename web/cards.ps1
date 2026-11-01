# Makes the link previews: a 1200 x 630 share card per app (web/site/cards/<App>.jpg, plus index.jpg for the
# home page), rendered from web/card.html with headless Chrome, and the description, Open Graph and Twitter
# tags between <!-- share --> and <!-- /share --> in the head of every page. Names, hints, categories and
# origins come from the gallery in web/site/index.html, so run this again after changing an app's entry there.
# The tags point at the card and the page with relative addresses; build with -Dsite.url=https://... to make
# them absolute, which most link preview services need (see web/README.md, Share).
# Build first (mvn -f web/pom.xml package, for the thumbnails and the icon), then run from the repo root:
#   ./web/cards.ps1
#   ./web/cards.ps1 -Apps Minesweeper,Clock -Port 8102
param(
    [string[]]$Apps = @(),
    [int]$Port = 8102,
    [string]$Chrome = "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
    [int]$WaitMs = 1500
)
$ErrorActionPreference = 'Stop'
$site = Join-Path $PSScriptRoot 'target/site'
$src = Join-Path $PSScriptRoot 'site'
$out = Join-Path $src 'cards'
if (-not (Test-Path (Join-Path $site 'index.html'))) { throw "No built site in $site. Run: mvn -f web/pom.xml package" }
if (-not (Test-Path $Chrome)) { throw "Chrome not found at $Chrome. Pass -Chrome <path>." }
New-Item -ItemType Directory -Force $out | Out-Null

function Attr([string]$s) { [Net.WebUtility]::HtmlEncode($s) }
$utf8 = New-Object Text.UTF8Encoding($false)

# The gallery: <li data-app="Id" data-origin="original" data-cat="game">...<strong>Name</strong><span>Hint</span>
$homePage = Join-Path $src 'index.html'
$gallery = [regex]::Matches([IO.File]::ReadAllText($homePage), '<li data-app="(\w+)"([^>]*)>.*?<strong>(.*?)</strong><span>(.*?)</span>')
$entries = foreach ($m in $gallery) {
    $attrs = $m.Groups[2].Value
    [pscustomobject]@{
        Id     = $m.Groups[1].Value
        Cat    = if ($attrs -match 'data-cat="(\w+)"') { $Matches[1] } else { '' }
        Origin = if ($attrs -match 'data-origin="(\w+)"') { $Matches[1] } else { 'decompiled' }
        Name   = [Net.WebUtility]::HtmlDecode($m.Groups[3].Value)
        Hint   = [Net.WebUtility]::HtmlDecode($m.Groups[4].Value)
    }
}
if (-not $entries) { throw "No apps found in the gallery of $homePage" }
$count = @($entries).Count

# The block of tags for one page. $up is the way from the page to the site root ('' or '../').
function Share-Tags([string]$title, [string]$description, [string]$page, [string]$card, [string]$alt, [string]$up) {
    $nl = "`r`n"
    $lines = @(
        '<!-- share: written by web/cards.ps1 from the gallery in index.html -->',
        "<meta name=`"description`" content=`"$(Attr $description)`">",
        '<meta property="og:type" content="website">',
        '<meta property="og:site_name" content="Old Java Applications">',
        "<meta property=`"og:title`" content=`"$(Attr $title)`">",
        "<meta property=`"og:description`" content=`"$(Attr $description)`">",
        "<meta property=`"og:url`" content=`"$up$page`">",
        "<meta property=`"og:image`" content=`"${up}cards/$card`">",
        '<meta property="og:image:width" content="1200">',
        '<meta property="og:image:height" content="630">',
        "<meta property=`"og:image:alt`" content=`"$(Attr $alt)`">",
        '<meta name="twitter:card" content="summary_large_image">',
        '<!-- /share -->'
    )
    ($lines | ForEach-Object { "  $_" }) -join $nl
}

function Write-Tags([string]$file, [string]$block) {
    $html = [IO.File]::ReadAllText($file)
    if ($html -match '(?s)  <!-- share.*?<!-- /share -->') {
        $html = [regex]::Replace($html, '(?s)  <!-- share.*?<!-- /share -->', { param($m) $block })
    } else {
        $html = $html -replace '(\r?\n)(  <link rel="stylesheet")', "`$1$block`$1`$2"
    }
    [IO.File]::WriteAllText($file, $html, $utf8)
}

$selected = if ($Apps.Count) { $entries | Where-Object { $Apps -contains $_.Id } } else { $entries }
$template = Join-Path $site '_card.html'
Copy-Item (Join-Path $PSScriptRoot 'card.html') $template
$server = Start-Process python -ArgumentList '-m', 'http.server', $Port, '--bind', '127.0.0.1', '--directory', $site -PassThru -WindowStyle Hidden
$profileDir = Join-Path ([IO.Path]::GetTempPath()) "oldjava-cards-$Port"
# Chrome saves a PNG; the card is kept as a JPEG, a fifth of the size.
Add-Type -AssemblyName System.Drawing
$jpeg = [Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq 'image/jpeg' }
$quality = New-Object Drawing.Imaging.EncoderParameters(1)
$quality.Param[0] = New-Object Drawing.Imaging.EncoderParameter([Drawing.Imaging.Encoder]::Quality, [long]88)
function Shoot([string]$query, [string]$jpg) {
    $png = Join-Path ([IO.Path]::GetTempPath()) "oldjava-card-$Port.png"
    & $Chrome --headless=new --disable-gpu --hide-scrollbars --mute-audio "--user-data-dir=$profileDir" `
        '--window-size=1200,630' "--virtual-time-budget=$WaitMs" "--screenshot=$png" `
        "http://127.0.0.1:$Port/_card.html$query" 2>$null | Out-Null
    $img = [Drawing.Image]::FromFile($png)
    try { $img.Save($jpg, $jpeg, $quality) } finally { $img.Dispose(); Remove-Item -Force $png }
    Write-Host "card -> $jpg"
}
try {
    Start-Sleep -Seconds 1
    if (-not $Apps.Count) { Shoot '' (Join-Path $out 'index.jpg') }
    foreach ($e in $selected) {
        $q = '?app=' + $e.Id + '&name=' + [uri]::EscapeDataString($e.Name) + '&hint=' + [uri]::EscapeDataString($e.Hint) +
            '&cat=' + $e.Cat + '&origin=' + $e.Origin
        Shoot $q (Join-Path $out "$($e.Id).jpg")
    }
} finally {
    Stop-Process -Id $server.Id -ErrorAction SilentlyContinue
    Remove-Item -Force $template -ErrorAction SilentlyContinue
    Remove-Item -Recurse -Force $profileDir -ErrorAction SilentlyContinue
}

Write-Tags $homePage (Share-Tags 'Old Java Applications' `
    "Java Swing and AWT programs recovered from old jars, compiled to JavaScript with TeaVM: play $count of them in the browser, installed as an app or offline." `
    'index.html' 'index.jpg' "Thumbnails of six of the $count apps" '')
foreach ($e in $entries) {
    $from = if ($e.Origin -eq 'original') { 'Built from the original Java source' } else { 'Recovered from an old Java jar' }
    Write-Tags (Join-Path $src "$($e.Id)/index.html") (Share-Tags "$($e.Name) | Old Java Applications" `
        "$($e.Hint) $from and playable in the browser." "$($e.Id)/index.html" "$($e.Id).jpg" "$($e.Name), running in the browser" '../')
    Write-Host "tags -> $($e.Id)/index.html"
}
