# Captures the home page thumbnails (web/public/shots/<App>.png) from a built site with headless Chrome.
# Each app page is opened in thumbnail mode (?embed&shot: square, shadowless windows on a magenta desktop) and
# driven over the DevTools protocol (web/cdp.ps1) through its recipe below, so every thumbnail shows its app in
# play (a game under way, a lesson lit, a race half run) rather than a start screen, and can be taken again the
# same way after a change. Then the image is cropped to its windows, the remaining magenta is made transparent,
# and wide shots are scaled down to -MaxWidth. Last, Chrome encodes the home page's lighter copies of it (Save-Webp).
# Build first (mvn -f web/pom.xml package), then run from the repo root:
#   ./web/shots.ps1
#   ./web/shots.ps1 -Apps Minesweeper,Clock -Port 8101
#   ./web/shots.ps1 -Out somewhere/else   (to compare before replacing the committed ones)
#   ./web/shots.ps1 -WebpOnly   (only re-encode the .webp copies from the PNGs already in -Out, e.g. after a merge)
param(
    [string[]]$Apps,
    [int]$Port = 8101,
    [int]$DebugPort = 9302,
    [string]$Chrome = "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
    [int]$Width = 960,
    [int]$Height = 600,
    [int]$MaxWidth = 480,
    [string]$Out = (Join-Path $PSScriptRoot 'public/shots'),
    [switch]$WebpOnly,
    [string]$ProfileRoot = [IO.Path]::GetTempPath()   # where Chrome's throwaway profile folder goes
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'cdp.ps1')

# Steps as in web/cdp.ps1, taken once the app's first window is up; the picture is taken after the last one.
# Query is added to the page address. Ready, a page expression, says the app is up (by default: its first window
# is on the desktop). The four games with a computer player get its key (W, C) instead of ?watch, which thumbnail
# mode leaves out.
$ShellUp = "!!document.querySelector('.shim-shell > *')"
# WindowsE's own window (its shortcut keyboard), lower right in front of the File Explorer windows it opened.
$WindowsEFront = "js:(function (d) { var w = d.firstElementChild; d.style.zIndex = 1000000; document.querySelector('.shim-shell').style.zIndex = 1; w.style.left = (innerWidth - w.offsetWidth - 24) + 'px'; w.style.top = (innerHeight - w.offsetHeight - 24) + 'px'; })(document.getElementById('desktop'))"
# NathaNOS: a sunset picture saved as if painted in its Paint and made the wallpaper (written into the page's saves,
# then the page reloads), then opened from its desktop icon (the 19th icon: column 4, row 4 at 960 x 600).
$NathaNOSPicture = "js:(function () { var W = 200, H = 125, p = [], x, y, c, s = '', i = 0, j; for (y = 0; y < H; y++) for (x = 0; x < W; x++) { c = y < 20 ? 14 : y < 38 ? 15 : y < 55 ? 11 : y < 70 ? 4 : 12; if (y < 34 && (x * 7 + y * 13) % 89 == 0) c = 8; if ((x - 100) * (x - 100) + (y - 78) * (y - 78) < 500 && !(y > 74 && y % 5 == 0)) c = 5; if (y > 84 + 7 * Math.sin(x / 17)) c = 2; if (y > 100 + 6 * Math.sin(x / 11 + 1)) c = 0; p.push(c); } while (i < p.length) { j = i; while (j < p.length && p[j] == p[i]) j++; s += String.fromCharCode(65 + p[i]) + (j - i > 1 ? (j - i).toString(36) : ''); i = j; } localStorage.setItem('oldjava.saves.NathaNOS', JSON.stringify({ '/.nathanos-pictures.txt': 'picture=Sunset:' + s + '\n', '/.nathanos.txt': 'wallpaper=picture:Sunset\n' })); location.reload(); })()"
# A double click there.
$NathaNOSOpen = "js:(function () { $Tap tap(348, 318, 2); })()"
# NumberGuess: Page Down opens the Duel tab (from 1-10, where a fresh visitor starts; the locked ranges are passed over).
$NumberGuessDuel = "js:(function (t) { ['keydown', 'keyup'].forEach(function (type) { t.dispatchEvent(new KeyboardEvent(type, { key: 'PageDown', keyCode: 34, which: 34, bubbles: true })); }); })(document.activeElement || document.body)"
# Then a click on the middle of the lower line of its lane, which guesses the middle of what is left.
$NumberGuessMiddle = "js:(function (c) { $Tap var r = c.getBoundingClientRect(); tap(r.left + r.width / 2, r.top + r.height * 180 / 350); })(document.querySelector('#desktop canvas.paint'))"
$Recipes = [ordered]@{
    Clock               = @{ Steps = @('wait:800', 'key:W', 'wait:300', (ClockAt 10.25), 'wait:800') }
    FlappyBird          = @{ Steps = @('wait:800', 'key:Space', 'wait:500', 'key:Space', 'wait:450', 'key:Space', 'wait:350') }
    Minesweeper         = @{ Steps = @('wait:800', 'key:O', 'key:W', 'wait:2200') }
    NathaNOS            = @{ Steps = @('wait:1500', $NathaNOSPicture, 'wait:5000', $NathaNOSOpen, 'wait:1500') }
    RubiksCube2D        = @{ Steps = @('wait:1500', (Click '^Mix$'), 'wait:1500', 'key:T', 'wait:600') + (Repeat 19 @($CoachTurn, 'wait:120')) + @('wait:1500') }
    ScreenSaver         = @{ Steps = @('wait:1500', 'key:9', 'wait:7000', 'key:F', 'wait:1800') }
    SpriteFloatyStars   = @{ Steps = @('wait:800', (Pick 'Mode 6'), (Click '^OK$'), 'wait:3000', $StarsSuns, 'wait:9000') }
    UltimateTicTacToe   = @{ Steps = @('wait:800', 'key:W', 'wait:7000') }
    CircleSpawner       = @{ Steps = @('wait:1500', "js:document.querySelector('#desktop button[title=Clock]').click()", 'wait:3000') }
    StockChecker        = @{ Steps = @('wait:800', 'key:F', 'wait:4000', 'key:B', 'wait:3000', 'key:-', 'key:=', 'wait:300') }
    SynthesizerKeyboard = @{ Steps = $SynthLoop + @('wait:1300') }
    GroupCreator        = @{ Steps = @('wait:800', (Click 'Make groups'), 'wait:1500', 'key:W', 'wait:2200') }
    MusicTheoryExpert   = @{ Steps = @('wait:800', (Click 'Songwriter'), 'wait:800') }
    NumberGuess         = @{ Steps = @('wait:800', $NumberGuessDuel, 'wait:300', 'key:3', 'key:7', 'key:Enter', 'wait:300') + (Repeat 3 @($NumberGuessMiddle, 'wait:1300')) }
    SimpleAlgorithms    = @{ Steps = @('wait:800', 'key:5', 'wait:3500') }
    TankGame            = @{ Steps = @('wait:800', 'key:Q', 'wait:4300') }
    BibleDownloader     = @{ Steps = @('wait:5000') }
    DinoGame            = @{ HandMade = $true; Steps = @('wait:800', 'key:E', 'key:F', 'wait:25000', 'key:F', 'wait:1800') }
    Windows             = @{ Ready = $ShellUp; Steps = @('wait:1500') }
    WindowsE            = @{ Steps = @('wait:800', (Click 'Side by side'), 'wait:300', (Click 'DO IT'), 'wait:2500', $WindowsEFront) }
}
# HandMade: the committed thumbnail was composited by hand (web/README.md), so only -Apps naming it retakes it.
if (-not $Apps) { $Apps = @($Recipes.Keys | Where-Object { $WebpOnly -or -not $Recipes[$_].HandMade }) }

Add-Type -AssemblyName System.Drawing
function Crop-Shot([string]$path) {
    $file = [Drawing.Bitmap]::FromFile($path)
    $bmp = New-Object Drawing.Bitmap($file.Width, $file.Height, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [Drawing.Graphics]::FromImage($bmp); $g.DrawImage($file, 0, 0, $file.Width, $file.Height); $g.Dispose(); $file.Dispose()
    $rect = New-Object Drawing.Rectangle(0, 0, $bmp.Width, $bmp.Height)
    $data = $bmp.LockBits($rect, [Drawing.Imaging.ImageLockMode]::ReadWrite, $bmp.PixelFormat)
    $px = New-Object byte[] ($data.Stride * $bmp.Height)
    [Runtime.InteropServices.Marshal]::Copy($data.Scan0, $px, 0, $px.Length)
    $x0 = $bmp.Width; $y0 = $bmp.Height; $x1 = -1; $y1 = -1
    for ($y = 0; $y -lt $bmp.Height; $y++) {
        $row = $y * $data.Stride
        for ($x = 0; $x -lt $bmp.Width; $x++) {
            $i = $row + 4 * $x  # B G R A
            if ($px[$i] -gt 235 -and $px[$i + 1] -lt 20 -and $px[$i + 2] -gt 235) { $px[$i + 3] = 0; continue }
            if ($x -lt $x0) { $x0 = $x }; if ($x -gt $x1) { $x1 = $x }
            if ($y -lt $y0) { $y0 = $y }; if ($y -gt $y1) { $y1 = $y }
        }
    }
    [Runtime.InteropServices.Marshal]::Copy($px, 0, $data.Scan0, $px.Length)
    $bmp.UnlockBits($data)
    if ($x1 -ge 0) {
        $w = $x1 - $x0 + 1; $h = $y1 - $y0 + 1
        $scale = [Math]::Min(1.0, $MaxWidth / $w)
        $crop = New-Object Drawing.Bitmap([int]($w * $scale), [int]($h * $scale), [Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $g = [Drawing.Graphics]::FromImage($crop)
        $g.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $g.DrawImage($bmp, (New-Object Drawing.Rectangle(0, 0, $crop.Width, $crop.Height)), $x0, $y0, $w, $h, [Drawing.GraphicsUnit]::Pixel)
        $g.Dispose()
        $crop.Save($path, [Drawing.Imaging.ImageFormat]::Png); $crop.Dispose()
    }
    $bmp.Dispose()
}

# The PNG stays the master (share cards, photo booth, takeaway); the home page shows WebP copies, about a quarter of
# the bytes: <App>.webp at full size for the gallery and <App>-240.webp, 240 px wide, for the rail, passport and news.
# Lossy color at 0.9 (no visible change at thumbnail size), alpha kept exact by Chrome's encoder.
function Save-Webp([string]$png) {
    $b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($png))
    foreach ($w in 0, 240) {
        $webp = Invoke-Js "(async function () { var i = new Image(); i.src = 'data:image/png;base64,$b64'; await i.decode(); var c = document.createElement('canvas'), W = $w ? Math.min($w, i.width) : i.width; c.width = W; c.height = Math.round(i.height * W / i.width); var g = c.getContext('2d'); g.imageSmoothingQuality = 'high'; g.drawImage(i, 0, 0, c.width, c.height); var b = await new Promise(function (r) { c.toBlob(r, 'image/webp', 0.9); }); return await new Promise(function (r) { var f = new FileReader(); f.onload = function () { r(f.result.split(',')[1]); }; f.readAsDataURL(b); }); })()" -Await
        [IO.File]::WriteAllBytes(($png -replace '\.png$', $(if ($w) { "-$w.webp" } else { '.webp' })), [Convert]::FromBase64String($webp))
    }
}

$site = Join-Path $PSScriptRoot 'target/site'
$out = $Out
New-Item -ItemType Directory -Force $out | Out-Null
$session = Start-Headless $site $Port $DebugPort $Chrome 'shots' $Width $Height -ProfileRoot $ProfileRoot
try {
    Invoke-Cdp 'Emulation.setDeviceMetricsOverride' @{ width = $Width; height = $Height; deviceScaleFactor = 1; mobile = $false } | Out-Null
    foreach ($app in $Apps) {
        $png = Join-Path $out "$app.png"
        if ($WebpOnly) { if (Test-Path $png) { Save-Webp $png; Write-Host "$app -> webp" }; continue }
        $recipe = $Recipes[$app]
        if (-not $recipe) { Write-Warning "$app has no recipe; skipped."; continue }
        $query = 'embed&shot' + $(if ($recipe.Query) { '&' + $recipe.Query } else { '' })
        $up = if ($recipe.Ready) { $recipe.Ready } else { "(function (d) { return !!(d && d.firstElementChild && d.firstElementChild.offsetWidth); })(document.getElementById('desktop'))" }
        if (-not (Open-App "http://127.0.0.1:$Port/$app/index.html?$query" $up)) { Write-Warning "$app did not open a window; skipped."; continue }
        Invoke-Steps $recipe.Steps
        # Only the app: no controller, toast or pretend taskbar tray over the desktop.
        Invoke-Js "[].forEach.call(document.querySelectorAll('.shim-watch, .shim-pad, .shim-toast, .shim-gp-pointer, .shim-shell-tray'), function (e) { e.style.visibility = 'hidden'; })" | Out-Null
        Start-Sleep -Milliseconds 100
        [IO.File]::WriteAllBytes($png, [Convert]::FromBase64String((Invoke-Cdp 'Page.captureScreenshot' @{ format = 'png' }).data))
        Crop-Shot $png
        Save-Webp $png
        Write-Host "$app -> $png"
    }
} finally {
    Stop-Headless $session
}
