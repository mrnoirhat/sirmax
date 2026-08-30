# SPDX-License-Identifier: AGPL-3.0-or-later
<#
.SYNOPSIS
    Authenticode-signs the SIRMAX Windows artifacts.

.DESCRIPTION
    Signs only SIRMAX's own binaries: the launcher executable and the MSI. The DLLs under
    runtime\bin belong to the JDK and to Microsoft and arrive already signed by them; re-signing
    someone else's binary with our key replaces a trusted signature with a less trusted one, which
    is strictly worse than leaving it alone.

    The certificate is looked up in the current user's store by subject. Whether Windows trusts it
    is a separate question from whether the file is signed — see docs/SIGNING.md.

.PARAMETER AppImage
    The jpackage app-image directory (contains SIRMAX.exe).

.PARAMETER Msi
    Path to the built installer.

.PARAMETER Subject
    Substring matched against the signing certificate's subject.

.PARAMETER TimestampServer
    RFC-3161 timestamp authority. A timestamp is what keeps an already-downloaded copy verifying
    after the certificate expires; without one every signature dies on the expiry date.
#>
param(
    [string] $AppImage,
    [string] $Msi,
    [string] $Subject = 'Andriezer',
    [string] $TimestampServer = 'http://timestamp.digicert.com'
)

$ErrorActionPreference = 'Stop'

$cert = @(Get-ChildItem Cert:\CurrentUser\My -CodeSigningCert |
          Where-Object { $_.Subject -like "*$Subject*" })
if ($cert.Count -eq 0) {
    Write-Output "No hay certificado de firma que coincida con '$Subject'."
    Write-Output "Crea uno con tools/new-signing-cert.ps1, o importa el de una CA."
    exit 2
}
$cert = $cert[0]
Write-Output ("Certificado: " + $cert.Subject)
Write-Output ("Huella:      " + $cert.Thumbprint)

$targets = New-Object System.Collections.Generic.List[string]
if ($AppImage -and (Test-Path $AppImage)) {
    $exe = Join-Path $AppImage 'SIRMAX.exe'
    if (Test-Path $exe) { $targets.Add($exe) }
}
if ($Msi -and (Test-Path $Msi)) { $targets.Add($Msi) }

if ($targets.Count -eq 0) {
    Write-Output "No se encontró ningún artefacto que firmar."
    exit 3
}

$failed = 0
foreach ($t in $targets) {
    # jpackage leaves its output read-only, and the signer cannot write to it.
    Set-ItemProperty -Path $t -Name IsReadOnly -Value $false

    $name = Split-Path $t -Leaf
    try {
        Set-AuthenticodeSignature -FilePath $t -Certificate $cert `
            -HashAlgorithm SHA256 -TimestampServer $TimestampServer -ErrorAction Stop | Out-Null
    } catch {
        Write-Output ("{0,-24} ERROR {1}" -f $name, $_.Exception.Message)
        $failed++
        continue
    }

    # Report what is actually on the file rather than the cmdlet's return value. On a self-signed
    # certificate the status is UnknownError — "chain terminates in an untrusted root" — even
    # though the signature applied correctly, and reporting that as a failure would be wrong.
    $sig = Get-AuthenticodeSignature -FilePath $t
    $signer = if ($sig.SignerCertificate) { $sig.SignerCertificate.Subject } else { '(sin firma)' }
    $stamped = if ($sig.TimeStamperCertificate) { 'con sello de tiempo' } else { 'SIN sello de tiempo' }
    Write-Output ("{0,-24} {1,-14} {2}" -f $name, $sig.Status, $stamped)
    Write-Output ("{0,-24} editor: {1}" -f '', $signer)

    if (-not $sig.SignerCertificate) { $failed++ }
}

if ($failed -gt 0) { exit 1 }
Write-Output "Listo. Si el estado es UnknownError, el binario está firmado pero el certificado"
Write-Output "todavía no es de confianza en esta máquina: ver docs/SIGNING.md."
