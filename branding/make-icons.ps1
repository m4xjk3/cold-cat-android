# Generates Android launcher assets and the in-app logo from branding/logo-source.jpg.
# Requires Windows PowerShell 5+ (System.Drawing). Run from the repository root:
#   powershell -ExecutionPolicy Bypass -File branding/make-icons.ps1
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$srcPath = Join-Path $PSScriptRoot 'logo-source.jpg'
$src = [System.Drawing.Image]::FromFile($srcPath)
$navy = [System.Drawing.Color]::FromArgb(255, 6, 27, 51)

function New-Bitmap([int]$size) {
    $bmp = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    return @($bmp, $g)
}

# Circular crop of the artwork (the black corners of the source are not part of the logo).
function Draw-Logo([System.Drawing.Graphics]$g, [int]$canvas, [double]$diameter, [bool]$background) {
    if ($background) {
        $g.Clear($navy)
    } else {
        $g.Clear([System.Drawing.Color]::Transparent)
    }
    $d = [int]($canvas * $diameter)
    $x = [int](($canvas - $d) / 2)
    $y = [int](($canvas - $d) / 2)
    $clip = New-Object System.Drawing.Drawing2D.GraphicsPath
    $clip.AddEllipse($x, $y, $d, $d)
    $old = $g.Clip
    $g.SetClip($clip)
    $g.DrawImage($src, (New-Object System.Drawing.Rectangle($x, $y, $d, $d)))
    $g.Clip = $old
    $clip.Dispose()
}

function Save-Resized([System.Drawing.Bitmap]$source, [string]$path, [int]$size) {
    $dst = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($dst)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.DrawImage($source, 0, 0, $size, $size)
    $g.Dispose()
    New-Item -ItemType Directory -Force (Split-Path -Parent $path) | Out-Null
    $dst.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $dst.Dispose()
}

# Full-size brand art for README / home page.
$big = New-Bitmap 1024
Draw-Logo $big[1] 1024 0.94 $true
$big[0].Save((Join-Path $PSScriptRoot 'logo.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$big[1].Dispose(); $big[0].Dispose()

$homeArt = New-Bitmap 512
Draw-Logo $homeArt[1] 512 1.0 $false
$homeArt[0].Save((Join-Path $root 'app\src\main\assets\logo.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$homeArt[1].Dispose(); $homeArt[0].Dispose()

# Legacy launcher icon: circular art on navy.
$legacy = New-Bitmap 512
Draw-Logo $legacy[1] 512 0.94 $true

# Adaptive foreground: transparent, art inside the 66% safe zone.
$fg = New-Bitmap 432
Draw-Logo $fg[1] 432 0.66 $false

$densities = @{ 'mdpi' = 1; 'hdpi' = 1.5; 'xhdpi' = 2; 'xxhdpi' = 3; 'xxxhdpi' = 4 }
foreach ($d in $densities.GetEnumerator()) {
    $dir = Join-Path $root "app\src\main\res\mipmap-$($d.Key)"
    $s = [int](48 * $d.Value)
    Save-Resized $legacy[0] (Join-Path $dir 'ic_launcher.png') $s
    Save-Resized $legacy[0] (Join-Path $dir 'ic_launcher_round.png') $s
    Save-Resized $fg[0] (Join-Path $dir 'ic_launcher_foreground.png') ([int](108 * $d.Value))
}

$legacy[1].Dispose(); $legacy[0].Dispose()
$fg[1].Dispose(); $fg[0].Dispose()
$src.Dispose()
Write-Output 'Icons written.'
