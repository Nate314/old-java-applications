<#
.SYNOPSIS
    Compile and run one of the Java apps, or build it into a runnable jar.
.EXAMPLE
    ./run.ps1                  # prompts for an app, then runs it
    ./run.ps1 Minesweeper      # runs Minesweeper
    ./run.ps1 FlappyBird -Build  # builds out/FlappyBird.jar
#>
param(
    [string]$App,
    [switch]$Build
)
$ErrorActionPreference = 'Stop'

# WorkDir is where the app expects to be launched from, since some load files by relative path.
$apps = [ordered]@{
    Clock             = @{ Main = 'clock.Main';                WorkDir = 'Clock' }
    FlappyBird        = @{ Main = 'flappyBird.Main';           WorkDir = 'FlappyBird/src'; Args = @('120') }
    Minesweeper       = @{ Main = 'minesweeper.Main';          WorkDir = 'Minesweeper' }
    NathaNOS          = @{ Main = 'main.Main';                 WorkDir = 'NathaNOS' }
    RubiksCube2D      = @{ Main = 'rubikscube.Main';           WorkDir = 'RubiksCube2D' }
    ScreenSaver       = @{ Main = 'screensaver.Main';          WorkDir = 'ScreenSaver' }
    SpriteFloatyStars = @{ Main = 'animate2.Animate2Main';     WorkDir = 'SpriteFloatyStars' }
    UltimateTicTacToe = @{ Main = 'ultimateticktacktoe.Main';  WorkDir = 'UltimateTicTacToe' }
    CircleSpawner     = @{ Main = 'circlewindow.Main';         WorkDir = 'CircleSpawner' }
    DinoGame          = @{ Main = 'dinosaurgameplayer.DinosaurGamePlayer'; WorkDir = 'DinoGame' }
    StockChecker      = @{ Main = 'stockchecker.Main';         WorkDir = 'StockChecker' }
    SynthesizerKeyboard = @{ Main = 'synthesizer.Main';        WorkDir = 'SynthesizerKeyboard' }
    Windows           = @{ Main = 'test.Test';                 WorkDir = 'Windows' }
    WindowsE          = @{ Main = 'windowse.WindowsEMain';     WorkDir = 'WindowsE' }
    BibleDownloader   = @{ Main = 'audiobibledownloader.Main'; WorkDir = 'BibleDownloader' }
    GroupCreator      = @{ Main = 'groupcreator.Main';         WorkDir = 'GroupCreator' }
    MusicTheoryExpert = @{ Main = 'musictheoryexpert.Main';    WorkDir = 'MusicTheoryExpert' }
    NumberGuess       = @{ Main = 'numberguess.Main';          WorkDir = 'NumberGuess' }
    SimpleAlgorithms  = @{ Main = 'simplealgorithms.Main';     WorkDir = 'SimpleAlgorithms' }
    TankGame          = @{ Main = 'tankgame.Main';             WorkDir = 'TankGame' }
}
$names = @($apps.Keys)

if (-not $App) {
    for ($i = 0; $i -lt $names.Count; $i++) { Write-Host "  $($i + 1)) $($names[$i])" }
    $choice = Read-Host 'Pick an app (number or name)'
    $App = if ($choice -match '^\d+$' -and [int]$choice -ge 1 -and [int]$choice -le $names.Count) { $names[[int]$choice - 1] } else { $choice }
}
$match = $names | Where-Object { $_ -ieq $App }
if (-not $match) { throw "Unknown app '$App'. Choose one of: $($names -join ', ')" }
$App = $match
$cfg = $apps[$App]

$root = $PSScriptRoot
$src = Join-Path $root "$App/src"
$classes = Join-Path $root "out/$App/classes"

# Compile into a clean output folder.
if (Test-Path $classes) { Remove-Item -Recurse -Force $classes }
New-Item -ItemType Directory -Force $classes | Out-Null
$sources = @(Get-ChildItem -Recurse -Filter *.java $src | ForEach-Object FullName)
& javac -nowarn -encoding UTF-8 -d $classes @sources
if ($LASTEXITCODE -ne 0) { throw "javac failed for $App" }

# Put non-Java files from src (images, etc.) on the classpath next to the classes, like the original jars.
Get-ChildItem -Recurse -File $src | Where-Object { $_.Extension -ne '.java' -and $_.FullName -notmatch '[\\/]META-INF[\\/]' } | ForEach-Object {
    $dest = Join-Path $classes $_.FullName.Substring($src.Length + 1)
    New-Item -ItemType Directory -Force (Split-Path $dest) | Out-Null
    Copy-Item $_.FullName $dest
}

$workDir = Join-Path $root $cfg.WorkDir
$appArgs = @($cfg.Args | Where-Object { $_ })

if ($Build) {
    # Locate the JDK's jar tool from the running java, since only java/javac are usually on PATH.
    $jarTool = (Get-Command jar -ErrorAction SilentlyContinue).Source
    if (-not $jarTool) {
        $javaHome = (& java -XshowSettings:properties -version 2>&1 | Select-String 'java.home\s*=\s*(.+)$').Matches[0].Groups[1].Value.Trim()
        $jarTool = Join-Path $javaHome 'bin/jar.exe'
    }
    if (-not (Test-Path $jarTool)) { throw 'Could not find the jar tool. Install a JDK or put jar on PATH.' }

    $jarPath = Join-Path $root "out/$App.jar"
    & $jarTool --create --file $jarPath --main-class $cfg.Main -C $classes .
    if ($LASTEXITCODE -ne 0) { throw "jar failed for $App" }
    Write-Host "Built $jarPath"
    Write-Host "Run it from its working folder: cd '$workDir'; java -jar '$jarPath' $appArgs"
} else {
    Push-Location $workDir
    try { & java -cp $classes $cfg.Main @appArgs } finally { Pop-Location }
}
