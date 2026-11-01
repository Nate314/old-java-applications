# Captures the home page thumbnails (web/site/shots/<App>.png) from a built site with headless Chrome.
# Each app page is opened in thumbnail mode (?embed&shot: square, shadowless windows on a magenta desktop),
# then the image is cropped to its windows, the remaining magenta is made transparent, and wide shots are
# scaled down to -MaxWidth.
# Build first (mvn -f web/pom.xml package), then run from the repo root:
#   ./web/shots.ps1
#   ./web/shots.ps1 -Apps Minesweeper,Clock -Port 8101
param(
    [string[]]$Apps = @('Clock', 'FlappyBird', 'Minesweeper', 'NathaNOS', 'RubiksCube2D', 'ScreenSaver',
        'SpriteFloatyStars', 'UltimateTicTacToe', 'CircleSpawner', 'StockChecker', 'SynthesizerKeyboard',
        'GroupCreator', 'MusicTheoryExpert', 'NumberGuess', 'SimpleAlgorithms', 'TankGame'),
    [int]$Port = 8101,
    [string]$Chrome = "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
    [int]$Width = 960,
    [int]$Height = 600,
    [int]$WaitMs = 4000,
    [int]$MaxWidth = 480
)
$ErrorActionPreference = 'Stop'
$site = Join-Path $PSScriptRoot 'target/site'
$out = Join-Path $PSScriptRoot 'site/shots'
if (-not (Test-Path (Join-Path $site 'index.html'))) { throw "No built site in $site. Run: mvn -f web/pom.xml package" }
if (-not (Test-Path $Chrome)) { throw "Chrome not found at $Chrome. Pass -Chrome <path>." }
New-Item -ItemType Directory -Force $out | Out-Null

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
$server = Start-Process python -ArgumentList '-m', 'http.server', $Port, '--bind', '127.0.0.1', '--directory', $site -PassThru -WindowStyle Hidden
$profileDir = Join-Path ([IO.Path]::GetTempPath()) "oldjava-shots-$Port"
try {
    Start-Sleep -Seconds 1
    foreach ($app in $Apps) {
        $png = Join-Path $out "$app.png"
        & $Chrome --headless=new --disable-gpu --hide-scrollbars --mute-audio "--user-data-dir=$profileDir" `
            "--window-size=$Width,$Height" "--virtual-time-budget=$WaitMs" "--screenshot=$png" `
            "http://127.0.0.1:$Port/$app/index.html?embed&shot" 2>$null | Out-Null
        Crop-Shot $png
        Write-Host "$app -> $png"
    }
} finally {
    Stop-Process -Id $server.Id -ErrorAction SilentlyContinue
    Remove-Item -Recurse -Force $profileDir -ErrorAction SilentlyContinue
}
