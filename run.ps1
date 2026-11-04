<#
.SYNOPSIS
    Starts the web site in Docker through compose-launcher, as the other compose projects do
    (https://github.com/Nate314/compose-launcher, the git submodule in ./compose-launcher; its ports and printed
    URLs are in run.conf). With `old` first, it works natively instead: compile and run one of the Java apps,
    build it into a runnable jar, or serve the web site.
.EXAMPLE
    ./run.ps1                    # docker compose up --build -d on a free port, then prints the site's address
    ./run.ps1 down               # any other arguments go to docker compose (logs -f, ps, up --build -d, ...)
    ./run.ps1 share              # starts it, then gives it a public address through ngrok
    ./run.ps1 old                # asks: the launcher (every app as a card, Enter plays one) or the site
    ./run.ps1 old Site           # serves the built site (built first if missing), asking for the port
    ./run.ps1 old Site -Build    # builds the site again, then serves it
    ./run.ps1 old Minesweeper    # runs Minesweeper
    ./run.ps1 old FlappyBird -Build  # builds out/FlappyBird.jar
    ./run.ps1 old DinoGame screen    # anything after the app name is passed to it
#>
# No param() block on purpose: $args keeps docker compose flags such as -d intact (a param() block would read
# -d as PowerShell's own -Debug).
$ErrorActionPreference = 'Stop'

# WorkDir is where the app expects to be launched from, since some load files by relative path.
$apps = [ordered]@{
    # The launcher lists the others and runs each with this script, in the PowerShell running it now.
    Launcher          = @{ Main = 'launcher.Main';             WorkDir = '.'; Args = @((Get-Process -Id $PID).Path) }
    Clock             = @{ Main = 'clock.Main';                WorkDir = 'Clock' }
    FlappyBird        = @{ Main = 'flappyBird.Main';           WorkDir = 'FlappyBird/src'; Args = @('120') }
    Minesweeper       = @{ Main = 'minesweeper.Main';          WorkDir = 'Minesweeper' }
    # Apps: built into NathaNOS's taskbar (assets/Clock.jar already ships; Windows and WindowsE would drive the real desktop).
    NathaNOS          = @{ Main = 'main.Main';                 WorkDir = 'NathaNOS'
                           Apps = @('Minesweeper', 'UltimateTicTacToe', 'RubiksCube2D', 'FlappyBird', 'SpriteFloatyStars',
                                    'ScreenSaver', 'CircleSpawner', 'StockChecker', 'SynthesizerKeyboard', 'BibleDownloader',
                                    'GroupCreator', 'MusicTheoryExpert', 'NumberGuess', 'SimpleAlgorithms', 'TankGame', 'DinoGame') }
    RubiksCube2D      = @{ Main = 'rubikscube.Main';           WorkDir = 'RubiksCube2D' }
    ScreenSaver       = @{ Main = 'screensaver.Main';          WorkDir = 'ScreenSaver' }
    SpriteFloatyStars = @{ Main = 'animate2.Animate2Main';     WorkDir = 'SpriteFloatyStars' }
    UltimateTicTacToe = @{ Main = 'ultimateticktacktoe.Main';  WorkDir = 'UltimateTicTacToe' }
    CircleSpawner     = @{ Main = 'circlewindow.Main';         WorkDir = 'CircleSpawner' }
    DinoGame          = @{ Main = 'dinosaurgameplayer.Main';   WorkDir = 'DinoGame' }
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

# Docker: anything without `old` first goes to the shared launcher, which picks free ports into .env and runs
# docker compose with those arguments (up --build -d, then the address, when there are none).
function Invoke-Compose([object[]]$ComposeArgs) {
    Set-Location $PSScriptRoot
    if (-not (Test-Path compose-launcher/run.ps1)) {
        git submodule update --init compose-launcher
        if ($LASTEXITCODE -ne 0) {
            [Console]::Error.WriteLine('run.ps1: could not fetch the compose-launcher submodule. This needs a git clone of the project (not a zip download) and network access.')
            exit 1
        }
    }
    & ./compose-launcher/run.ps1 @ComposeArgs
    exit $LASTEXITCODE
}

if (-not ($args.Count -and "$($args[0])" -ieq 'old')) { Invoke-Compose $args }

# old: the app (or Site), -Build anywhere, and the rest for the app, as the param() block this script had read them.
$App = $null
$Build = $false
$AppArgs = @()
for ($i = 1; $i -lt $args.Count; $i++) {
    $a = "$($args[$i])"
    if ($a -ieq '-Build') { $Build = $true }
    elseif ($a -ieq '-App' -and $i + 1 -lt $args.Count) { $App = "$($args[++$i])" }
    elseif ($null -eq $App) { $App = $a }
    else { $AppArgs += $a }
}

if (-not $App) {
    $choice = Read-Host 'Open the [L]auncher or the [S]ite? (Enter for Launcher)'
    $App = if ($choice -match '^\s*s') { 'Site' } else { 'Launcher' }
}

# The web site: built with Maven (when missing, or with -Build) and served by web/serve.ps1.
if ($App -ieq 'Site') {
    $siteIndex = Join-Path $PSScriptRoot 'web/target/site/index.html'
    if ($Build -or -not (Test-Path $siteIndex)) {
        if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { throw 'Building the site needs Maven (mvn) on the PATH.' }
        if (-not (Get-Command npm -ErrorAction SilentlyContinue)) { throw 'Building the site needs Node.js (npm) on the PATH, for its React pages.' }
        Write-Host 'Building the site (web/target/site); this takes a few minutes...'
        & mvn -q -f (Join-Path $PSScriptRoot 'web/pom.xml') package
        if ($LASTEXITCODE -ne 0) { throw 'The site build failed.' }
    }
    do {
        $answer = (Read-Host 'Port (Enter for 8000)').Trim()
        if (-not $answer) { $answer = '8000' }
        $valid = $answer -match '^\d{1,5}$' -and [int]$answer -ge 1 -and [int]$answer -le 65535
        if (-not $valid) { Write-Host 'A port is a number from 1 to 65535.' }
    } until ($valid)
    & (Join-Path $PSScriptRoot 'web/serve.ps1') -Port ([int]$answer) -Open
    return
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
$appArgs = @(@($cfg.Args) + @($AppArgs) | Where-Object { $_ })

# NathaNOS shows one taskbar button per jar in assets/. Stage a copy of its folder with a jar of each listed app,
# plus the files those apps read from their own working folder (images, data), and run it from there.
if ($cfg.Apps) {
    $stage = Join-Path $root "out/$App/desktop"
    if (Test-Path $stage) { Remove-Item -Recurse -Force $stage }
    New-Item -ItemType Directory -Force $stage | Out-Null
    Copy-Item -Recurse (Join-Path $workDir 'assets') $stage
    foreach ($name in $cfg.Apps) {
        & $PSCommandPath old $name -Build 6>$null
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
