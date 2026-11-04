<#
.SYNOPSIS
    Serves the built site (web/target/site) on http://localhost:<Port>/ with PowerShell alone. Ctrl+C stops it.
.EXAMPLE
    ./web/serve.ps1              # port 8000
    ./web/serve.ps1 -Port 8080 -Open
#>
param(
    [int]$Port = 8000,
    [string]$Root = (Join-Path $PSScriptRoot 'target/site'),
    [switch]$Open
)
$ErrorActionPreference = 'Stop'
if (-not (Test-Path (Join-Path $Root 'index.html'))) { throw "No built site in $Root. Build it with: mvn -f web/pom.xml package" }
$Root = (Resolve-Path $Root).Path

$types = @{
    '.html' = 'text/html; charset=utf-8'; '.js' = 'text/javascript; charset=utf-8'; '.mjs' = 'text/javascript; charset=utf-8'
    '.css' = 'text/css; charset=utf-8'; '.json' = 'application/json'; '.webmanifest' = 'application/manifest+json'
    '.txt' = 'text/plain; charset=utf-8'; '.md' = 'text/plain; charset=utf-8'; '.java' = 'text/plain; charset=utf-8'
    '.csv' = 'text/csv; charset=utf-8'; '.xml' = 'application/xml'; '.svg' = 'image/svg+xml'
    '.png' = 'image/png'; '.jpg' = 'image/jpeg'; '.jpeg' = 'image/jpeg'; '.gif' = 'image/gif'; '.ico' = 'image/x-icon'
    '.webp' = 'image/webp'; '.mp4' = 'video/mp4'; '.webm' = 'video/webm'; '.wav' = 'audio/wav'; '.mp3' = 'audio/mpeg'
    '.wasm' = 'application/wasm'; '.woff2' = 'font/woff2'; '.jar' = 'application/java-archive'
}

# The file a URL path names: a folder means its index.html; null when there is none or it is outside the root.
function Resolve-SitePath([string]$urlPath) {
    $relative = [Uri]::UnescapeDataString($urlPath).TrimStart('/').Replace('/', [IO.Path]::DirectorySeparatorChar)
    $full = [IO.Path]::GetFullPath((Join-Path $Root $relative))
    if (-not $full.StartsWith($Root, [StringComparison]::OrdinalIgnoreCase)) { return $null }
    if (Test-Path $full -PathType Container) { $full = Join-Path $full 'index.html' }
    if (Test-Path $full -PathType Leaf) { return $full }
    return $null
}

$listener = [Net.HttpListener]::new()
$listener.Prefixes.Add("http://localhost:$Port/")
try { $listener.Start() } catch { throw "Could not listen on port ${Port}: $($_.Exception.Message)" }
$url = "http://localhost:$Port/"
Write-Host "Serving $Root at $url (Ctrl+C stops it)"
if ($Open) { Start-Process $url }

try {
    while ($listener.IsListening) {
        # Wait in short steps so Ctrl+C is noticed between requests.
        $next = $listener.GetContextAsync()
        while (-not $next.Wait(250)) { }
        $context = $next.Result
        $request = $context.Request
        $response = $context.Response
        try {
            $path = $request.Url.AbsolutePath
            # A folder without its trailing slash: redirect, so the page's relative addresses resolve inside it.
            $folder = [IO.Path]::GetFullPath((Join-Path $Root ([Uri]::UnescapeDataString($path).TrimStart('/'))))
            if (-not $path.EndsWith('/') -and $folder.StartsWith($Root, [StringComparison]::OrdinalIgnoreCase) -and (Test-Path $folder -PathType Container)) {
                $response.Redirect($path + '/' + $request.Url.Query)
                $status = 302
            } else {
                $file = Resolve-SitePath $path
                $status = 200
                if (-not $file) { $file = Resolve-SitePath '/404.html'; $status = 404 }
                $response.StatusCode = $status
                if ($file) {
                    $type = $types[[IO.Path]::GetExtension($file).ToLowerInvariant()]
                    $response.ContentType = if ($type) { $type } else { 'application/octet-stream' }
                    $response.Headers['Cache-Control'] = 'no-cache'
                    $bytes = [IO.File]::ReadAllBytes($file)
                    $response.ContentLength64 = $bytes.Length
                    if ($request.HttpMethod -ne 'HEAD') { $response.OutputStream.Write($bytes, 0, $bytes.Length) }
                }
            }
            if ($status -ne 200) { Write-Host "$status $($request.HttpMethod) $path" }
        } catch {
            # The browser closed the connection mid-answer (a cancelled load); nothing to do.
        } finally {
            try { $response.Close() } catch { }
        }
    }
} finally {
    $listener.Stop()
    $listener.Close()
    Write-Host 'Stopped.'
}
