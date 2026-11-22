# Makes the link preview pictures: a 1200 x 630 share card per app (web/public/cards/<App>.jpg, plus index.jpg for
# the home page and story.jpg for the story), rendered from web/card.html with headless Chrome. Names, hints, categories and origins come from
# web/src/shared/apps.json, so run this again after changing an app's entry there. The pages' share tags, which point
# at these cards, are written by the build (web/src/routes.tsx, web/src/pages/app/AppPage.tsx).
# Build first (npm run build in web/, for the thumbnails and the icon), then run from the repo root:
# (-Port, -DebugPort and -ProfileRoot have no defaults: pass your own on every run)
#   ./web/cards.ps1 -Port 8107 -DebugPort 9307 -ProfileRoot C:\some\scratch
#   ./web/cards.ps1 -Apps Minesweeper,Clock -Port 8107 -DebugPort 9307 -ProfileRoot C:\some\scratch
#   ./web/cards.ps1 -Apps story ...              # only the story page's card (story.jpg)
#   ./web/cards.ps1 -Out somewhere/else ...      (to compare before replacing the committed ones)
# The server and headless Chrome are started as in shots.ps1 and clips.ps1 (web/cdp.ps1's Start-Headless).
param(
    [string[]]$Apps = @(),
    [int]$Port = $(throw '-Port is required (no default: two runs must never share a port)'),
    [int]$DebugPort = $(throw '-DebugPort is required (no default: two runs must never share a port)'),
    [string]$Chrome = "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
    [string]$Out = (Join-Path $PSScriptRoot 'public/cards'),
    [string]$ProfileRoot = $(throw '-ProfileRoot is required (no default: keep Chrome profiles in your own scratch folder)')   # where Chrome's throwaway profile folder goes
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'cdp.ps1')
$site = Join-Path $PSScriptRoot 'target/site'
$out = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($Out)   # .NET's Save needs a full path
New-Item -ItemType Directory -Force $out | Out-Null

# The apps, from the one list the home page's gallery is rendered from.
$catalogFile = Join-Path $PSScriptRoot 'src/shared/apps.json'
$entries = foreach ($a in ([IO.File]::ReadAllText($catalogFile) | ConvertFrom-Json)) {
    [pscustomobject]@{
        Id     = $a.id
        Cat    = $a.cat
        Origin = if ($a.origin) { $a.origin } else { 'decompiled' }
        Name   = $a.name
        Hint   = $a.hint
    }
}
if (-not $entries) { throw "No apps found in $catalogFile" }

$selected = if ($Apps.Count) { $entries | Where-Object { $Apps -contains $_.Id } } else { $entries }
$template = Join-Path $site '_card.html'
# Chrome's screenshot is a PNG; the card is kept as a JPEG, a fifth of the size.
Add-Type -AssemblyName System.Drawing
$jpeg = [Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq 'image/jpeg' }
$quality = New-Object Drawing.Imaging.EncoderParameters(1)
$quality.Param[0] = New-Object Drawing.Imaging.EncoderParameter([Drawing.Imaging.Encoder]::Quality, [long]88)
function Shoot([string]$query, [string]$jpg) {
    $url = "http://127.0.0.1:$Port/_card.html$query"
    # Ready once this card (not the one before it), its pictures and its fonts have loaded; the card does nothing after that.
    $loaded = "location.href === '$url' && document.readyState === 'complete' && [].every.call(document.images, function (i) { return i.complete; }) && document.fonts.status === 'loaded'"
    if (-not (Open-App $url $loaded)) { throw "The card $url did not load." }
    $png = [Convert]::FromBase64String((Invoke-Cdp 'Page.captureScreenshot' @{ format = 'png' }).data)
    $img = [Drawing.Image]::FromStream((New-Object IO.MemoryStream(, $png)))
    try { $img.Save($jpg, $jpeg, $quality) } finally { $img.Dispose() }
    Write-Host "card -> $jpg"
}
$session = Start-Headless $site $Port $DebugPort $Chrome 'cards' 1200 630 -ProfileRoot $ProfileRoot
try {
    Copy-Item (Join-Path $PSScriptRoot 'card.html') $template
    Invoke-Cdp 'Emulation.setDeviceMetricsOverride' @{ width = 1200; height = 630; deviceScaleFactor = 1; mobile = $false } | Out-Null
    if (-not $Apps.Count) { Shoot '' (Join-Path $out 'index.jpg') }
    if (-not $Apps.Count -or $Apps -contains 'story') { Shoot '?story' (Join-Path $out 'story.jpg') }
    foreach ($e in $selected) {
        $q = '?app=' + $e.Id + '&name=' + [uri]::EscapeDataString($e.Name) + '&hint=' + [uri]::EscapeDataString($e.Hint) +
            '&cat=' + $e.Cat + '&origin=' + $e.Origin
        Shoot $q (Join-Path $out "$($e.Id).jpg")
    }
} finally {
    Stop-Headless $session
    Remove-Item -Force $template -ErrorAction SilentlyContinue
}
