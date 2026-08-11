<#
.SYNOPSIS
  Generates DietScheduler's "plate as calendar" brand assets as PNGs, using PowerShell 5.1 +
  System.Drawing (GDI+). No PIL/ImageMagick/cairosvg was available on either the Windows host or
  WSL for this project, so this is deliberately dependency-free and reproducible by re-running it.

.DESCRIPTION
  Concept: a plate (the app is about meals) whose rim carries 12 date-dots like hours on a clock
  face (the app is about *scheduling* those meals) -- 11 in the brand's terracotta, one in coral
  marking "today". Two compositions, because Android's adaptive-icon safe zone (the center ~66%
  of the canvas that survives every mask shape) is too tight to fit the plate AND flanking
  cutlery without either shrinking to illegibility or clipping under a circular/squircle mask:

    - Launcher icon: plate only, cutlery *inside* it. Sized to fit the 625px safe zone within a
      1024px canvas (Android masks adaptive icons to ~66% of the canvas: 1024 * 0.66 ~= 675, and
      we stay a bit inside that at 625 for margin).
    - Master logo (splash / README): the full concept, cutlery flanking the plate, on a sand
      field, plate rim stroked in the brand's coral -- no mask constraint, so it can breathe.

  Outputs six PNGs into mobile/assets/branding/:
    icon_foreground.png  - adaptive icon foreground layer (transparent bg)
    icon_background.png  - adaptive icon background layer (opaque sand fill)
    icon_monochrome.png  - Android 13+ themed-icon layer (white silhouette, transparent bg)
    icon_legacy.png      - flattened icon for iOS and Android <26 (opaque)
    logo_master.png      - full concept for splash screen backdrop / README
    splash_icon.png      - icon-only mark for flutter_native_splash (transparent bg)

.NOTES
  Re-run this script any time the brand palette changes (see mobile/lib/theme/app_palette.dart --
  the hex values below must be kept in sync with that file by hand, since this script has no way
  to parse Dart).
#>

Add-Type -AssemblyName System.Drawing

$OutDir = Join-Path $PSScriptRoot "..\..\mobile\assets\branding"
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

# --- Palette (must match mobile/lib/theme/app_palette.dart) ---
function Get-Color([string]$Hex) { return [System.Drawing.ColorTranslator]::FromHtml("#$Hex") }
$Sand        = Get-Color "DBD3AD"
$Coral       = Get-Color "D36060"
$Terracotta  = Get-Color "C2714F"
$DarkNeutral = Get-Color "1E1B15"  # matches AppColorSchemes.light.onSurface -- best universal contrast for cutlery
$White       = [System.Drawing.Color]::White
$Transparent = [System.Drawing.Color]::Transparent

function New-Canvas([int]$Size) {
    $bmp = New-Object System.Drawing.Bitmap($Size, $Size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear($Transparent)
    return [PSCustomObject]@{ Bitmap = $bmp; Graphics = $g }
}

function Save-Canvas($Canvas, [string]$Path) {
    $Canvas.Bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $Canvas.Graphics.Dispose()
    $Canvas.Bitmap.Dispose()
}

# A simple geometric fork: a handle plus four tine rectangles joined by a base bar.
function Draw-Fork($g, [double]$cx, [double]$cy, [double]$length, [System.Drawing.Color]$color) {
    $handleWidth = $length * 0.12
    $tineHeight = $length * 0.38
    $handleHeight = $length - $tineHeight
    $tineWidth = $handleWidth * 0.22
    $tineGap = $handleWidth * 0.14
    $brush = New-Object System.Drawing.SolidBrush($color)

    $g.FillRectangle($brush, [float]($cx - $handleWidth / 2), [float]($cy + $tineHeight), [float]$handleWidth, [float]$handleHeight)

    $totalTineWidth = 4 * $tineWidth + 3 * $tineGap
    $startX = $cx - $totalTineWidth / 2
    for ($i = 0; $i -lt 4; $i++) {
        $tx = $startX + $i * ($tineWidth + $tineGap)
        $g.FillRectangle($brush, [float]$tx, [float]$cy, [float]$tineWidth, [float]($tineHeight * 0.78))
    }
    $g.FillRectangle($brush, [float]($cx - $totalTineWidth / 2), [float]($cy + $tineHeight * 0.78), [float]$totalTineWidth, [float]($tineHeight * 0.22))
    $brush.Dispose()
}

# A simple geometric knife: a handle plus a tapering blade polygon.
function Draw-Knife($g, [double]$cx, [double]$cy, [double]$length, [System.Drawing.Color]$color) {
    $handleWidth = $length * 0.14
    $bladeHeight = $length * 0.42
    $handleHeight = $length - $bladeHeight
    $bladeWidth = $length * 0.13
    $brush = New-Object System.Drawing.SolidBrush($color)

    $g.FillRectangle($brush, [float]($cx - $handleWidth / 2), [float]($cy + $bladeHeight), [float]$handleWidth, [float]$handleHeight)

    $points = @(
        [System.Drawing.PointF]::new([float]($cx - $bladeWidth / 2), [float]($cy + $bladeHeight)),
        [System.Drawing.PointF]::new([float]($cx - $bladeWidth / 2), [float]($cy + $bladeHeight * 0.18)),
        [System.Drawing.PointF]::new([float]$cx, [float]$cy),
        [System.Drawing.PointF]::new([float]($cx + $bladeWidth / 2), [float]($cy + $bladeHeight * 0.18)),
        [System.Drawing.PointF]::new([float]($cx + $bladeWidth / 2), [float]($cy + $bladeHeight))
    )
    $g.FillPolygon($brush, $points)
    $brush.Dispose()
}

# The plate + 12-dot dial.
#   plateColor: fill for the plate disc itself. Must contrast with whatever it's composited over
#     (a sand plate on a sand background is invisible -- the real bug this script's first draft
#     had) -- so the plate face is white against the app's sand field/background everywhere.
#   rimColor $null: no stroked outline. rimFilled $false + rimColor set: plate drawn as a hollow
#     OUTLINE ring instead of a filled disc, for the monochrome layer -- Android tints that layer
#     a single flat color at runtime, so dots/plate/cutlery all need to be the SAME tone (no
#     terracotta/coral distinction survives that tinting anyway) and a bold ring reads as "plate"
#     more clearly than a solid blob once everything is one color.
function Draw-PlateDial($g, [double]$cx, [double]$cy, [double]$plateRadius, [double]$ringRadius, [System.Drawing.Color]$plateColor, $rimColor, [double]$rimWidth, [bool]$monochrome = $false, [System.Drawing.Color]$monoColor = [System.Drawing.Color]::White) {
    if ($monochrome) {
        $pen = New-Object System.Drawing.Pen($monoColor, [float]($plateRadius * 0.06))
        $g.DrawEllipse($pen, [float]($cx - $plateRadius), [float]($cy - $plateRadius), [float]($plateRadius * 2), [float]($plateRadius * 2))
        $pen.Dispose()
    } else {
        $plateBrush = New-Object System.Drawing.SolidBrush($plateColor)
        $g.FillEllipse($plateBrush, [float]($cx - $plateRadius), [float]($cy - $plateRadius), [float]($plateRadius * 2), [float]($plateRadius * 2))
        $plateBrush.Dispose()

        if ($null -ne $rimColor) {
            $pen = New-Object System.Drawing.Pen($rimColor, [float]$rimWidth)
            $g.DrawEllipse($pen, [float]($cx - $plateRadius), [float]($cy - $plateRadius), [float]($plateRadius * 2), [float]($plateRadius * 2))
            $pen.Dispose()
        }
    }

    $dotRadius = $plateRadius * 0.045
    for ($i = 0; $i -lt 12; $i++) {
        $angle = (-90 + $i * 30) * [Math]::PI / 180  # start at 12 o'clock ("today"), clockwise
        $dx = $cx + $ringRadius * [Math]::Cos($angle)
        $dy = $cy + $ringRadius * [Math]::Sin($angle)
        $dotColor = if ($monochrome) { $monoColor } elseif ($i -eq 0) { $Coral } else { $Terracotta }
        $dotBrush = New-Object System.Drawing.SolidBrush($dotColor)
        $g.FillEllipse($dotBrush, [float]($dx - $dotRadius), [float]($dy - $dotRadius), [float]($dotRadius * 2), [float]($dotRadius * 2))
        $dotBrush.Dispose()
    }
}

$Size = 1024
$Cx = $Size / 2
$Cy = $Size / 2

# --- 1. icon_foreground.png: plate Ø600 (radius 300), dot ring Ø520 (radius 260), cutlery inside,
#        no rim stroke. Plate face is WHITE, not sand -- this layer gets composited BY ANDROID on
#        top of icon_background.png (flat sand), and a sand plate on a sand background is
#        invisible; white against sand is what actually reads as "a plate". ---
$fg = New-Canvas $Size
Draw-PlateDial $fg.Graphics $Cx $Cy 300 260 $White $null 0
Draw-Fork $fg.Graphics ($Cx - 90) $Cy 190 $DarkNeutral
Draw-Knife $fg.Graphics ($Cx + 90) $Cy 190 $DarkNeutral
Save-Canvas $fg (Join-Path $OutDir "icon_foreground.png")

# --- 2. icon_background.png: flat sand fill, the adaptive icon's background layer. ---
$bg = New-Canvas $Size
$bgBrush = New-Object System.Drawing.SolidBrush($Sand)
$bg.Graphics.FillRectangle($bgBrush, 0, 0, $Size, $Size)
$bgBrush.Dispose()
Save-Canvas $bg (Join-Path $OutDir "icon_background.png")

# --- 3. icon_monochrome.png: single-tone silhouette, transparent bg -- Android 13+ applies its
#        own flat tint over this for themed (Material You) icons, so every element here must
#        already be one color (the plate is drawn as a hollow ring, not a filled disc, so its
#        shape stays legible once tinted -- see Draw-PlateDial's monochrome mode). ---
$mono = New-Canvas $Size
Draw-PlateDial $mono.Graphics $Cx $Cy 300 260 $White $null 0 $true $White
Draw-Fork $mono.Graphics ($Cx - 90) $Cy 190 $White
Draw-Knife $mono.Graphics ($Cx + 90) $Cy 190 $White
Save-Canvas $mono (Join-Path $OutDir "icon_monochrome.png")

# --- 4. icon_legacy.png: background + foreground flattened, opaque -- iOS and Android <26. ---
$legacy = New-Canvas $Size
$legacyBrush = New-Object System.Drawing.SolidBrush($Sand)
$legacy.Graphics.FillRectangle($legacyBrush, 0, 0, $Size, $Size)
$legacyBrush.Dispose()
Draw-PlateDial $legacy.Graphics $Cx $Cy 300 260 $White $null 0
Draw-Fork $legacy.Graphics ($Cx - 90) $Cy 190 $DarkNeutral
Draw-Knife $legacy.Graphics ($Cx + 90) $Cy 190 $DarkNeutral
Save-Canvas $legacy (Join-Path $OutDir "icon_legacy.png")

# --- 5. logo_master.png: full concept, no mask constraint -- sand field, plate rim stroked in
#        coral, cutlery flanking OUTSIDE the plate (this is the composition that doesn't survive
#        adaptive-icon masking, which is exactly why the launcher icon above is a separate,
#        simplified composition). ---
$MasterSize = 1200
$master = New-Canvas $MasterSize
$fieldBrush = New-Object System.Drawing.SolidBrush($Sand)
$master.Graphics.FillRectangle($fieldBrush, 0, 0, $MasterSize, $MasterSize)
$fieldBrush.Dispose()
$mCx = $MasterSize / 2
$mCy = $MasterSize / 2
Draw-PlateDial $master.Graphics $mCx $mCy 340 290 $White $Coral 10
Draw-Fork $master.Graphics ($mCx - 420) $mCy 260 $DarkNeutral
Draw-Knife $master.Graphics ($mCx + 420) $mCy 260 $DarkNeutral
Save-Canvas $master (Join-Path $OutDir "logo_master.png")

# --- 6. splash_icon.png: icon-only mark, transparent bg, slightly more breathing room than the
#        launcher icon (splash screens read better with visible margin around the mark). White
#        plate reads against either the light (sand) or dark (near-black) splash background
#        configured separately in pubspec.yaml -- flutter_native_splash composites this over that
#        configured color. ---
$splash = New-Canvas $Size
Draw-PlateDial $splash.Graphics $Cx $Cy 250 216 $White $null 0
Draw-Fork $splash.Graphics ($Cx - 75) $Cy 158 $DarkNeutral
Draw-Knife $splash.Graphics ($Cx + 75) $Cy 158 $DarkNeutral
Save-Canvas $splash (Join-Path $OutDir "splash_icon.png")

Write-Host "Wrote 6 brand assets to $OutDir"
