<#
.SYNOPSIS
    Compile and run one of the Java apps, or build it into a runnable jar.
.EXAMPLE
    ./run.ps1                  # opens the launcher: every app as a card, Enter plays one
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
    # The launcher lists the others and runs each with this script, in the PowerShell running it now.
    Launcher          = @{ Main = 'launcher.Main';             WorkDir = '.'; Args = @((Get-Process -Id $PID).Path) }
    Clock             = @{ Main = 'clock.Main';                WorkDir = 'Clock' }
    FlappyBird        = @{ Main = 'flappyBird.Main';           WorkDir = 'FlappyBird/src'; Args = @('120') }
    Minesweeper       = @{ Main = 'minesweeper.Main';          WorkDir = 'Minesweeper' }
    # Apps: built into NathaNOS's taskbar (assets/Clock.jar already ships; the Robot apps would drive the real desktop).
    NathaNOS          = @{ Main = 'main.Main';                 WorkDir = 'NathaNOS'
                           Apps = @('Minesweeper', 'UltimateTicTacToe', 'RubiksCube2D', 'FlappyBird', 'SpriteFloatyStars',
                                    'ScreenSaver', 'CircleSpawner', 'StockChecker', 'SynthesizerKeyboard', 'BibleDownloader',
                                    'GroupCreator', 'MusicTheoryExpert', 'NumberGuess', 'SimpleAlgorithms', 'TankGame') }
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

if (-not $App) { $App = 'Launcher' }
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

# NathaNOS shows one taskbar button per jar in assets/. Stage a copy of its folder with a jar of each listed app,
# plus the files those apps read from their own working folder (images, data), and run it from there.
if ($cfg.Apps) {
    $stage = Join-Path $root "out/$App/desktop"
    if (Test-Path $stage) { Remove-Item -Recurse -Force $stage }
    New-Item -ItemType Directory -Force $stage | Out-Null
    Copy-Item -Recurse (Join-Path $workDir 'assets') $stage
    foreach ($name in $cfg.Apps) {
        & $PSCommandPath $name -Build 6>$null
        Copy-Item (Join-Path $root "out/$name.jar") (Join-Path $stage 'assets')
        Get-ChildItem -File (Join-Path $root $apps[$name].WorkDir) | Where-Object Extension -ne '.md' | Copy-Item -Destination $stage
    }
    Write-Host "Staged $App with $($cfg.Apps.Count) more apps in $stage"
    $workDir = $stage
}

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
