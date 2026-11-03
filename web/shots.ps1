# Captures the home page thumbnails (web/site/shots/<App>.png) from a built site with headless Chrome.
# Each app page is opened in thumbnail mode (?embed&shot: square, shadowless windows on a magenta desktop) and
# driven over the DevTools protocol (web/cdp.ps1) through its recipe below, so every thumbnail shows its app in
# play (a game under way, a lesson lit, a race half run) rather than a start screen, and can be taken again the
# same way after a change. Then the image is cropped to its windows, the remaining magenta is made transparent,
# and wide shots are scaled down to -MaxWidth.
# Build first (mvn -f web/pom.xml package), then run from the repo root:
#   ./web/shots.ps1
#   ./web/shots.ps1 -Apps Minesweeper,Clock -Port 8101
#   ./web/shots.ps1 -Out somewhere/else   (to compare before replacing the committed ones)
param(
    [string[]]$Apps,
    [int]$Port = 8101,
    [int]$DebugPort = 9302,
    [string]$Chrome = "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
    [int]$Width = 960,
    [int]$Height = 600,
    [int]$MaxWidth = 480,
    [string]$Out = (Join-Path $PSScriptRoot 'site/shots')
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'cdp.ps1')

# Steps as in web/cdp.ps1, taken once the app's first window is up; the picture is taken after the last one.
# Query is added to the page address. Ready, a page expression, says the app is up (by default: its first window
# is on the desktop). The four games with a computer player get its key (W, C) instead of ?watch, which thumbnail
# mode leaves out.
$ShellUp = "!!document.querySelector('.shim-shell > *')"
# WindowsE's own window, lower right in front of the File Explorer windows it opened.
$WindowsEFront = "js:(function (d) { var w = d.firstElementChild; d.style.zIndex = 1000000; w.style.left = (innerWidth - w.offsetWidth - 24) + 'px'; w.style.top = (innerHeight - w.offsetHeight - 24) + 'px'; })(document.getElementById('desktop'))"
# SynthesizerKeyboard's looper: clear any old loop (Delete twice), record a melody layer after the count-in, then a
# bass layer an octave down at the next bar. Keys are held (keydown, then keyup after ms) so notes get their lengths.
function SynthHold([string]$key, [int]$code, [int]$ms) {
    "js:(function (t) { function f(type) { t.dispatchEvent(new KeyboardEvent(type, { key: '$key', keyCode: $code, which: $code, bubbles: true })); } f('keydown'); setTimeout(function () { f('keyup'); }, $ms); })(document.activeElement || document.body)"
}
$SynthLoop = @('wait:800', 'key:O', (SynthHold 'Delete' 46 20), (SynthHold 'Delete' 46 20), 'wait:200', 'key:Space', 'wait:2450') +
    (@(@('z', 90), @('c', 67), @('b', 66), @('c', 67), @('z', 90), @('c', 67), @('b', 66), @(',', 188)) | ForEach-Object { (SynthHold $_[0] $_[1] 450), 'wait:625' }) +
    @('wait:400', 'key:ArrowLeft', 'key:Space', 'wait:2300') +
    (@(@('z', 90), @('b', 66), @('v', 86), @('c', 67)) | ForEach-Object { (SynthHold $_[0] $_[1] 1100), 'wait:1250' })
$Recipes = [ordered]@{
    Clock               = @{ Steps = @('wait:800', 'key:F', 'wait:300', 'key:Space', 'wait:2500') }
    FlappyBird          = @{ Steps = @('wait:800', 'key:Space', 'wait:500', 'key:Space', 'wait:450', 'key:Space', 'wait:350') }
    Minesweeper         = @{ Steps = @('wait:800', 'key:W', 'wait:2200') }
    NathaNOS            = @{ Steps = @('wait:2500') }
    RubiksCube2D        = @{ Steps = @('wait:1500', (Click '^Mix$'), 'wait:1500', 'key:T', 'wait:600') + (Repeat 19 @($CoachTurn, 'wait:120')) + @('wait:1500') }
    ScreenSaver         = @{ Steps = @('wait:1500', 'key:9', 'wait:7000', 'key:F', 'wait:1800') }
    SpriteFloatyStars   = @{ Steps = @('wait:800', (Click 'Star Catcher'), 'wait:800', 'key:1', 'wait:2500') }
    UltimateTicTacToe   = @{ Steps = @('wait:800', 'key:W', 'wait:7000') }
    CircleSpawner       = @{ Steps = @('wait:1500', (Pick 'Heart'), 'wait:3000') }
    StockChecker        = @{ Steps = @('wait:800', 'key:F', 'wait:4000', 'key:B', 'wait:3000', 'key:R', 'wait:300') }
    SynthesizerKeyboard = @{ Steps = $SynthLoop + @('wait:1300') }
    GroupCreator        = @{ Steps = @('wait:800', (Click 'Make groups'), 'wait:1500') }
    MusicTheoryExpert   = @{ Steps = @('wait:800', (Click 'Songwriter'), 'wait:800') }
    NumberGuess         = @{ Steps = @('wait:800', 'key:5', 'key:Enter', 'wait:900', 'key:8', 'key:Enter', 'wait:900') }
    SimpleAlgorithms    = @{ Steps = @('wait:800', 'key:5', 'wait:3500') }
    TankGame            = @{ Steps = @('wait:800', 'key:C', 'wait:4500') }
    BibleDownloader     = @{ Steps = @('wait:5000') }
    DinoGame            = @{ HandMade = $true; Steps = @('wait:800', 'key:E', 'key:F', 'wait:25000', 'key:F', 'wait:1800') }
    Windows             = @{ Ready = $ShellUp; Steps = @('wait:1500') }
    WindowsE            = @{ Steps = @('wait:800', "js:(function (f) { f.value = '5'; f.dispatchEvent(new Event('input')); })(document.querySelector('#desktop input'))", (Click 'DO IT'), 'wait:2500', $WindowsEFront) }
}
# HandMade: the committed thumbnail was composited by hand (web/README.md), so only -Apps naming it retakes it.
if (-not $Apps) { $Apps = @($Recipes.Keys | Where-Object { -not $Recipes[$_].HandMade }) }

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

$site = Join-Path $PSScriptRoot 'target/site'
$out = $Out
New-Item -ItemType Directory -Force $out | Out-Null
$session = Start-Headless $site $Port $DebugPort $Chrome 'shots' $Width $Height
try {
    Invoke-Cdp 'Emulation.setDeviceMetricsOverride' @{ width = $Width; height = $Height; deviceScaleFactor = 1; mobile = $false } | Out-Null
    foreach ($app in $Apps) {
        $recipe = $Recipes[$app]
        if (-not $recipe) { Write-Warning "$app has no recipe; skipped."; continue }
        $query = 'embed&shot' + $(if ($recipe.Query) { '&' + $recipe.Query } else { '' })
        $up = if ($recipe.Ready) { $recipe.Ready } else { "(function (d) { return !!(d && d.firstElementChild && d.firstElementChild.offsetWidth); })(document.getElementById('desktop'))" }
        if (-not (Open-App "http://127.0.0.1:$Port/$app/index.html?$query" $up)) { Write-Warning "$app did not open a window; skipped."; continue }
        Invoke-Steps $recipe.Steps
        # Only the app: no controller, toast or pretend taskbar tray over the desktop.
        Invoke-Js "[].forEach.call(document.querySelectorAll('.shim-watch, .shim-pad, .shim-toast, .shim-gp-pointer, .shim-shell-tray'), function (e) { e.style.visibility = 'hidden'; })" | Out-Null
        Start-Sleep -Milliseconds 100
        $png = Join-Path $out "$app.png"
        [IO.File]::WriteAllBytes($png, [Convert]::FromBase64String((Invoke-Cdp 'Page.captureScreenshot' @{ format = 'png' }).data))
        Crop-Shot $png
        Write-Host "$app -> $png"
    }
} finally {
    Stop-Headless $session
}
