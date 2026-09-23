$ErrorActionPreference = 'Stop'

# Credentials exist only in this process environment and its backend child.
$previousUsername = $env:MAIL_USERNAME
$previousPassword = $env:MAIL_PASSWORD
$previousHost = $env:MAIL_HOST
$previousPort = $env:MAIL_PORT
$previousFrom = $env:MAIL_FROM

Push-Location $PSScriptRoot
try {
    $env:MAIL_USERNAME = (Read-Host 'Gmail address used to SEND email').Trim()
    if ([string]::IsNullOrWhiteSpace($env:MAIL_USERNAME)) {
        throw 'A Gmail sender address is required.'
    }
    $gmailSecret = Read-Host 'Google App Password (input is hidden)' -AsSecureString
    $env:MAIL_PASSWORD = ([System.Net.NetworkCredential]::new('', $gmailSecret).Password) -replace '\s', ''
    if ([string]::IsNullOrWhiteSpace($env:MAIL_PASSWORD)) {
        throw 'An App Password is required.'
    }
    $env:MAIL_HOST = 'smtp.gmail.com'
    $env:MAIL_PORT = '587'
    $env:MAIL_FROM = $env:MAIL_USERNAME
    & .\gradlew.bat bootRun --no-configuration-cache
    if ($LASTEXITCODE -ne 0) {
        throw 'Backend failed. Check the error log above.'
    }
} finally {
    $env:MAIL_USERNAME = $previousUsername
    $env:MAIL_PASSWORD = $previousPassword
    $env:MAIL_HOST = $previousHost
    $env:MAIL_PORT = $previousPort
    $env:MAIL_FROM = $previousFrom
    if ($null -ne $gmailSecret) { $gmailSecret.Dispose() }
    Pop-Location
}
