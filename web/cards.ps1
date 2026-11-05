# Makes the link preview pictures: a 1200 x 630 share card per app (web/public/cards/<App>.jpg, plus index.jpg for
# the home page), rendered from web/card.html with headless Chrome. Names, hints, categories and origins come from
# web/src/shared/apps.json, so run this again after changing an app's entry there. The pages' share tags, which point
# at these cards, are written by the build (web/src/routes.tsx, web/src/pages/app/AppPage.tsx).
# Build first (npm run build in web/, for the thumbnails and the icon), then run from the repo root:
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
$out = Join-Path $PSScriptRoot 'public/cards'
if (-not (Test-Path (Join-Path $site 'index.html'))) { throw "No built site in $site. Run: npm run build (in web/)" }
if (-not (Test-Path $Chrome)) { throw "Chrome not found at $Chrome. Pass -Chrome <path>." }
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
