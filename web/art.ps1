# Renders the apps' own art from its SVG sources (each app's art/ folder) to the PNG and JPEG files the Java code
# loads, with headless Chrome, so a changed drawing can be rendered again the same way. The SVGs are the source;
# the rendered files are committed next to the code that reads them. Run from the repo root:
#   ./web/art.ps1
#   ./web/art.ps1 -Only bird,bible   (by output file name, without the extension)
param(
    [string[]]$Only = @(),
    [string]$Chrome = "$env:ProgramFiles\Google\Chrome\Application\chrome.exe"
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
if (-not (Test-Path $Chrome)) { throw "Chrome not found at $Chrome. Pass -Chrome <path>." }
Add-Type -AssemblyName System.Drawing

# Source SVG -> output file, at the SVG's own size (its width and height attributes).
$jobs = @(
    @{ Svg = 'FlappyBird/art/bg.svg';                    Out = 'FlappyBird/src/bg.jpg' }
    @{ Svg = 'FlappyBird/art/ground.svg';                Out = 'FlappyBird/src/ground.jpg' }
    @{ Svg = 'FlappyBird/art/topPipe.svg';               Out = 'FlappyBird/src/topPipe.png' }
    @{ Svg = 'FlappyBird/art/bottomPipe.svg';            Out = 'FlappyBird/src/bottomPipe.png' }
    @{ Svg = 'FlappyBird/art/bird.svg';                  Out = 'FlappyBird/src/bird.png' }
    @{ Svg = 'BibleDownloader/art/bible.svg';            Out = 'BibleDownloader/src/assets/bible.png' }
    @{ Svg = 'BibleDownloader/art/bible.svg';            Out = 'NathaNOS/assets/BibleDownloader.png' }
    @{ Svg = 'NathaNOS/art/DinoGame.svg';                Out = 'NathaNOS/assets/DinoGame.png' }
    @{ Svg = 'NathaNOS/art/Minesweeper.svg';             Out = 'NathaNOS/assets/Minesweeper.png' }
    @{ Svg = 'NathaNOS/art/clock.svg';                   Out = 'NathaNOS/assets/clock.png' }
    @{ Svg = 'NathaNOS/art/NumberGuess.svg';             Out = 'NathaNOS/assets/NumberGuess.png' }
    @{ Svg = 'NathaNOS/art/StockChecker.svg';            Out = 'NathaNOS/assets/StockChecker.png' }
    @{ Svg = 'NathaNOS/art/SynthesizerKeyboard.svg';     Out = 'NathaNOS/assets/SynthesizerKeyboard.png' }
    @{ Svg = 'NathaNOS/art/UltimateTicTacToe.svg';       Out = 'NathaNOS/assets/UltimateTicTacToe.png' }
    @{ Svg = 'NathaNOS/art/cursor.svg';                  Out = 'NathaNOS/assets/cursor.png' }
)

$profileDir = Join-Path ([IO.Path]::GetTempPath()) "oldjava-art-chrome-$PID"
try {
    foreach ($job in $jobs) {
        $name = [IO.Path]::GetFileNameWithoutExtension($job.Out)
        if ($Only.Count -and $Only -notcontains $name) { continue }
        $svg = Join-Path $root $job.Svg
        $out = Join-Path $root $job.Out
        $text = [IO.File]::ReadAllText($svg)
        if ($text -notmatch '<svg[^>]*\swidth="(\d+)"[^>]*\sheight="(\d+)"') { throw "$($job.Svg) needs width and height attributes" }
        $w = [int]$Matches[1]; $h = [int]$Matches[2]
        $png = Join-Path ([IO.Path]::GetTempPath()) "oldjava-art-$PID-$name.png"
        $uri = ([Uri]$svg).AbsoluteUri
        & $Chrome --headless --disable-gpu --hide-scrollbars "--user-data-dir=$profileDir" `
            --default-background-color=00000000 --force-device-scale-factor=1 "--window-size=$w,$h" "--screenshot=$png" $uri 2>$null | Out-Null
        if (-not (Test-Path $png)) { throw "Chrome did not render $($job.Svg)" }
        if ($job.Out -like '*.jpg') {
            # JPEG: flattened onto white (the art has no transparency), quality 90.
            $src = [Drawing.Image]::FromFile($png)
            try {
                $bmp = New-Object Drawing.Bitmap $w, $h
                $g = [Drawing.Graphics]::FromImage($bmp)
                $g.Clear([Drawing.Color]::White)
                $g.DrawImage($src, 0, 0, $w, $h)
                $g.Dispose()
                $codec = [Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object MimeType -eq 'image/jpeg'
                $params = New-Object Drawing.Imaging.EncoderParameters 1
                $params.Param[0] = New-Object Drawing.Imaging.EncoderParameter ([Drawing.Imaging.Encoder]::Quality), 90L
                $bmp.Save($out, $codec, $params)
                $bmp.Dispose()
            } finally { $src.Dispose() }
            Remove-Item $png
        } else {
            Move-Item -Force $png $out
        }
        Write-Host "$($job.Svg) -> $($job.Out) ($w x $h)"
    }
} finally {
    if (Test-Path $profileDir) { Remove-Item -Recurse -Force $profileDir -ErrorAction SilentlyContinue }
}
