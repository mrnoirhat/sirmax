# SPDX-License-Identifier: AGPL-3.0-or-later
<#
.SYNOPSIS
    Creates the self-signed code-signing certificate used to sign SIRMAX on a developer machine.

.DESCRIPTION
    This is a development certificate. It puts a real publisher name on the binaries, which is
    worth doing — an unsigned installer shows "Publicador desconocido", a self-signed one shows
    the name — but Windows will not trust it on any machine where it has not been installed
    deliberately. See docs/SIGNING.md for what that means and what a CA-issued certificate changes.

    Idempotent: running it again reports the existing certificate instead of creating a second one.
#>
param(
    [string] $CommonName = 'Andriezer Galva Montero (Mrnoirhat)',
    [string] $Organization = 'SIRMAX',
    [string] $Country = 'DO',
    [int]    $Years = 5
)

$ErrorActionPreference = 'Stop'

$existing = @(Get-ChildItem Cert:\CurrentUser\My -CodeSigningCert |
              Where-Object { $_.Subject -like "*$CommonName*" })
if ($existing.Count -gt 0) {
    Write-Output ("Ya existe: " + $existing[0].Subject)
    Write-Output ("Huella:    " + $existing[0].Thumbprint)
    Write-Output ("Caduca:    " + $existing[0].NotAfter.ToString('yyyy-MM-dd'))
    exit 0
}

$params = @{
    Type              = 'CodeSigningCert'
    Subject           = "CN=$CommonName, O=$Organization, C=$Country"
    KeyUsage          = 'DigitalSignature'
    FriendlyName      = "SIRMAX code signing - $CommonName"
    CertStoreLocation = 'Cert:\CurrentUser\My'
    NotAfter          = (Get-Date).AddYears($Years)
    KeyAlgorithm      = 'RSA'
    KeyLength         = 3072
    HashAlgorithm     = 'SHA256'
}
$cert = New-SelfSignedCertificate @params

Write-Output ("Creado: " + $cert.Subject)
Write-Output ("Huella: " + $cert.Thumbprint)
Write-Output ("Caduca: " + $cert.NotAfter.ToString('yyyy-MM-dd'))
Write-Output ""
Write-Output "La clave privada queda en el almacén del usuario y NO se exporta ni se sube a git."
Write-Output "Para que Windows confíe en él en esta máquina, ver docs/SIGNING.md."
