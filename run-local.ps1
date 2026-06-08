# run-local.ps1 - Launch eBookShop MainApp with optional Google OAuth VM args
# Reads credentials from environment variables or from %USERPROFILE%\.ebookshop\oauth.properties

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Definition
$oauthPath = Join-Path $env:USERPROFILE '.ebookshop\oauth.properties'

$clientId = $env:GOOGLE_OAUTH_CLIENT_ID
$clientSecret = $env:GOOGLE_OAUTH_CLIENT_SECRET

if (-not $clientId -or -not $clientSecret) {
    if (Test-Path $oauthPath) {
        $lines = Get-Content $oauthPath | Where-Object { $_ -match '=' }
        foreach ($line in $lines) {
            $parts = $line -split '=', 2
            $k = $parts[0].Trim()
            $v = if ($parts.Length -ge 2) { $parts[1].Trim() } else { '' }
            switch ($k) {
                'GOOGLE_OAUTH_CLIENT_ID' { if (-not $clientId) { $clientId = $v } }
                'GOOGLE_OAUTH_CLIENT_SECRET' { if (-not $clientSecret) { $clientSecret = $v } }
            }
        }
    }
}

# Build JVM args
$args = @()
if ($clientId) { $args += "-Dgoogle.oauth.clientId=$clientId" }
if ($clientSecret) { $args += "-Dgoogle.oauth.clientSecret=$clientSecret" }

# Classpath (adjust if your build places classes elsewhere)
$cp = (Join-Path $projectRoot 'target\classes') + ';' + (Join-Path $projectRoot 'target\dependency\*')
$args += '-cp'
$args += $cp

# Main class
$main = 'com.mycompany.ebookshop.MainApp'

# Ensure JavaFX modules are added (for JDKs that bundle JavaFX)
## If the environment provides a JavaFX SDK lib path, add module-path so native JavaFX modules are found.
$javafxLib = $env:JAVA_FX_LIB
if (-not $javafxLib) { $javafxLib = $env:JAVAFX_LIB }
if ($javafxLib) {
    $args = @()
    if ($clientId) { $args += "-Dgoogle.oauth.clientId=$clientId" }
    if ($clientSecret) { $args += "-Dgoogle.oauth.clientSecret=$clientSecret" }
    $args += '--module-path'
    $args += $javafxLib
    $args += '--add-modules'
    $args += 'javafx.controls,javafx.fxml'
    $args += '-cp'
    $args += $cp
    $args += $main
} else {
    # No JavaFX SDK path provided; try to add modules only (works for JDKs that bundle JavaFX)
    $args += '--add-modules'
    $args += 'javafx.controls,javafx.fxml'
    $args += $main
}

$java = 'java'

Write-Host "Running: $java $($args -join ' ')"
& $java @args
