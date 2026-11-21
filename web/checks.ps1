# Runs the apps' self-checks (the *Check classes under <App>/test/) the way each app's README says to: its javac
# line compiles the app and its checks, then each check runs with its README java line's flags (-ea, headless...).
# Prints one PASS or FAIL line per check with its time, and exits 1 when anything fails. A check not in its
# README runs with -ea and is marked so. Nothing is written outside -Out (default out/checks, gitignored): the
# classes go to <Out>/<App>/classes in place of the README's out/check/<App>, each check's output to
# <Out>/<App>/<Check>.log, and user.home points at -UserHome (default <Out>/home) so no real save file is touched.
# A check also fails if it loaded the real sound output or microphone (DirectAudioDevice), the MIDI synthesizer,
# the system clipboard or a key-pressing Robot: the checks use the apps' silent paths and key lists, and this
# makes sure they still do. A check still running after -Timeout seconds is stopped by PID and fails.
# From the repo root:
#   ./web/checks.ps1
#   ./web/checks.ps1 -Apps Clock,Minesweeper -Out C:\some\scratch\checks
param(
    [string[]]$Apps,
    [string]$Out = (Join-Path (Split-Path $PSScriptRoot) 'out/checks'),
    [string]$UserHome,
    [int]$Timeout = 300
)
$ErrorActionPreference = 'Stop'
$Repo = Split-Path $PSScriptRoot
$Out = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($Out)
if (-not $UserHome) { $UserHome = Join-Path $Out 'home' }
New-Item -ItemType Directory -Force $UserHome | Out-Null
$Apps = @($Apps -split ',' | Where-Object { $_ })   # also when called with -File, which passes the list as one string
if (-not $Apps) { $Apps = @(Get-ChildItem $Repo -Directory | Where-Object { Test-Path (Join-Path $_.FullName 'test') } | ForEach-Object Name) }
# Classes that would reach the machine: speakers or microphone, the MIDI synthesizer, the clipboard, real key presses.
$Forbidden = 'com\.sun\.media\.sound\.(DirectAudioDevice|SoftSynthesizer) |sun\.awt\.windows\.(WClipboard|WRobotPeer) '

# Runs a program from $dir with its own output in $log; returns its exit code, or $null when stopped after $Timeout.
function Invoke-Logged([string]$exe, [string[]]$argv, [string]$dir, [string]$log) {
    $si = [Diagnostics.ProcessStartInfo]::new($exe)
    foreach ($a in $argv) { $si.ArgumentList.Add($a) }
    $si.WorkingDirectory = $dir; $si.UseShellExecute = $false
    $si.RedirectStandardOutput = $true; $si.RedirectStandardError = $true
    $p = [Diagnostics.Process]::Start($si)
    $o = $p.StandardOutput.ReadToEndAsync(); $e = $p.StandardError.ReadToEndAsync()
    $done = $p.WaitForExit($Timeout * 1000)
    if (-not $done) { $p.Kill($true); $p.WaitForExit() }
    Set-Content -Encoding UTF8 $log ($o.Result + $e.Result)
    if ($done) { $p.ExitCode } else { $null }
}

$results = @()
$all = [Diagnostics.Stopwatch]::StartNew()
foreach ($app in $Apps) {
    $appDir = Join-Path $Repo $app
    $readme = Join-Path $appDir 'README.md'
    $lines = if (Test-Path $readme) { @(Get-Content $readme | ForEach-Object Trim) } else { @() }
    $javac = $lines | Where-Object { $_ -like 'javac *' } | Select-Object -First 1
    if (-not $javac) { $results += [pscustomobject]@{ App = $app; Check = '(compile)'; Ok = $false; Ms = 0; Note = 'no javac line in README' }; Write-Host "FAIL $app (compile) no javac line in README"; continue }
    $classes = Join-Path $Out "$app/classes"
    New-Item -ItemType Directory -Force $classes | Out-Null
    # README commands run from the repo root when they name <App>/src, otherwise from the app's folder.
    $cwd = if ($javac -match " $app/") { $Repo } else { $appDir }
    $outPath = "(\.\./)?out/check/$app(?=\s|$)"
    $argv = foreach ($t in ($javac -replace $outPath, $classes) -split '\s+' | Select-Object -Skip 1) {
        if ($t -match '\*') { (Get-ChildItem (Join-Path $cwd $t)).FullName } else { $t }
    }
    $t0 = [Diagnostics.Stopwatch]::StartNew()
    $code = Invoke-Logged 'javac' $argv $cwd (Join-Path $Out "$app/javac.log")
    if ($code -ne 0) {
        $results += [pscustomobject]@{ App = $app; Check = '(compile)'; Ok = $false; Ms = $t0.ElapsedMilliseconds; Note = "javac failed, see $app/javac.log" }
        Write-Host "FAIL $app (compile)"; continue
    }
    $testDir = Join-Path $appDir 'test'
    foreach ($f in Get-ChildItem $testDir -Recurse -Filter '*Check.java' | Sort-Object Name) {
        $class = ([IO.Path]::GetRelativePath($testDir, $f.FullName) -replace '\.java$', '') -replace '[\\/]', '.'
        $line = $lines | Where-Object { $_ -match "^java .*\s$([regex]::Escape($class))$" } | Select-Object -First 1
        $note = ''
        $flags = if ($line) {
            $tok = @($line -split '\s+' | Select-Object -Skip 1)
            for ($i = 0; $i -lt $tok.Count - 1; $i++) { if ($tok[$i] -in '-cp', '-classpath') { $i++ } else { $tok[$i] } }
        } else { $note = 'not in README, ran with -ea'; '-ea' }
        $log = Join-Path $Out "$app/$($f.BaseName).log"
        $t0 = [Diagnostics.Stopwatch]::StartNew()
        $code = Invoke-Logged 'java' (@($flags) + "-Duser.home=$UserHome", '-verbose:class', '-cp', $classes, $class) $cwd $log
        $ms = $t0.ElapsedMilliseconds
        # Keep the check's own output in the log; the class-load lines only feed the guard.
        $text = @(Get-Content $log)
        $hit = $text | Where-Object { $_ -match '\[class,load\]' -and $_ -match $Forbidden } | Select-Object -First 1
        Set-Content -Encoding UTF8 $log ($text | Where-Object { $_ -notmatch '^\[[\d.]+s\]\[info\]\[class,load\]' })
        $ok = $code -eq 0 -and -not $hit
        if ($null -eq $code) { $note = "stopped after $Timeout s" }
        elseif ($hit) { $note = 'loaded ' + ($hit -replace '.*\] (\S+) .*', '$1') }
        elseif ($code -ne 0) { $note = "exit $code, see $app/$($f.BaseName).log" }
        $results += [pscustomobject]@{ App = $app; Check = $f.BaseName; Ok = $ok; Ms = $ms; Note = $note }
        Write-Host ('{0} {1}.{2} ({3:N1} s){4}' -f $(if ($ok) { 'PASS' } else { 'FAIL' }), $app, $f.BaseName, ($ms / 1000), $(if ($note) { " $note" }))
    }
}
$failed = @($results | Where-Object { -not $_.Ok })
Write-Host ('{0} of {1} checks passed in {2:N0} s; logs in {3}' -f ($results.Count - $failed.Count), $results.Count, $all.Elapsed.TotalSeconds, $Out)
if ($failed) { exit 1 }
