# Generates Android launcher assets and the in-app logo from branding/logo-source.png.
# The source has a transparent background, so no circular crop is needed: the
# script only trims the empty margin around the artwork and scales it down.
# Requires Windows PowerShell 5+ (System.Drawing). Run from the repository root:
#   powershell -ExecutionPolicy Bypass -File branding/make-icons.ps1
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$srcPath = Join-Path $PSScriptRoot 'logo-source.png'
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

# Bounding box of the visible artwork (alpha > 8), ignoring transparent margins.
$bmpSrc = New-Object System.Drawing.Bitmap($src)
$minX = $bmpSrc.Width; $minY = $bmpSrc.Height; $maxX = 0; $maxY = 0
for ($y = 0; $y -lt $bmpSrc.Height; $y++) {
    for ($x = 0; $x -lt $bmpSrc.Width; $x++) {
        if ($bmpSrc.GetPixel($x, $y).A -gt 8) {
            if ($x -lt $minX) { $minX = $x }
            if ($x -gt $maxX) { $maxX = $x }
            if ($y -lt $minY) { $minY = $y }
            if ($y -gt $maxY) { $maxY = $y }
        }
    }
}
$bmpSrc.Dispose()

$artW = $maxX - $minX + 1
$artH = $maxY - $minY + 1
# Square crop around the artwork with a small breathing room.
$cropSize = [int]([Math]::Max($artW, $artH) * 1.04)
$cropX = [int](($minX + $maxX) / 2 - $cropSize / 2)
$cropY = [int](($minY + $maxY) / 2 - $cropSize / 2)
# Keep the crop inside the source image.
if ($cropX -lt 0) { $cropX = 0 }
if ($cropY -lt 0) { $cropY = 0 }
if ($cropX + $cropSize -gt $src.Width) { $cropX = $src.Width - $cropSize }
if ($cropY + $cropSize -gt $src.Height) { $cropY = $src.Height - $cropSize }
$srcRect = New-Object System.Drawing.Rectangle($cropX, $cropY, $cropSize, $cropSize)
Write-Output "source $($src.Width)x$($src.Height), art ${artW}x${artH}, crop $cropSize at ($cropX,$cropY)"

# Draws the artwork scaled to `diameter` of the canvas. `background` fills the
# canvas with the brand navy (used only where a plate is required).
function Draw-Logo([System.Drawing.Graphics]$g, [int]$canvas, [double]$diameter, [bool]$background) {
    if ($background) {
        $g.Clear($navy)
    } else {
        $g.Clear([System.Drawing.Color]::Transparent)
    }
    $d = [int]($canvas * $diameter)
    $x = [int](($canvas - $d) / 2)
    $y = [int](($canvas - $d) / 2)
    $dest = New-Object System.Drawing.Rectangle($x, $y, $d, $d)
    $g.DrawImage($src, $dest, $srcRect, [System.Drawing.GraphicsUnit]::Pixel)
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

# Full-size brand art for README (transparent).
$big = New-Bitmap 1024
Draw-Logo $big[1] 1024 0.96 $false
$big[0].Save((Join-Path $PSScriptRoot 'logo.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$big[1].Dispose(); $big[0].Dispose()

# Home page logo (transparent).
$homeArt = New-Bitmap 512
Draw-Logo $homeArt[1] 512 0.98 $false
$homeArt[0].Save((Join-Path $root 'app\src\main\assets\logo.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$homeArt[1].Dispose(); $homeArt[0].Dispose()

# Legacy launcher icon (Android 7 and launchers that ignore adaptive icons):
# the artwork is already circular, so keep it transparent.
$legacy = New-Bitmap 512
Draw-Logo $legacy[1] 512 0.96 $false

# Adaptive foreground: art inside the 66% safe zone, transparent outside.
$fg = New-Bitmap 432
Draw-Logo $fg[1] 432 0.72 $false

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
