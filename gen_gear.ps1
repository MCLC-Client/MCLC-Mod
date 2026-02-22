Add-Type -AssemblyName System.Drawing

$size = 128
$bmp = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.Clear([System.Drawing.Color]::Transparent)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

$cx = 64
$cy = 64
$white = [System.Drawing.Color]::White
$brush = New-Object System.Drawing.SolidBrush($white)

$teeth = 8
$outer = 54
$inner = 34
$hole  = 16
$pi    = [Math]::PI
$step  = 2 * $pi / $teeth
$half  = $step * 0.38

$pts = New-Object System.Collections.Generic.List[System.Drawing.PointF]

for ($i = 0; $i -lt $teeth; $i++) {
    $a1 = $i * $step - $half
    $a2 = $i * $step + $half
    $a3 = $i * $step + $step * 0.5
    $a4 = ($i + 1) * $step - $half

    $pts.Add([System.Drawing.PointF]::new([float]($cx + $outer * [Math]::Cos($a1)), [float]($cy + $outer * [Math]::Sin($a1))))
    $pts.Add([System.Drawing.PointF]::new([float]($cx + $outer * [Math]::Cos($a2)), [float]($cy + $outer * [Math]::Sin($a2))))
    $pts.Add([System.Drawing.PointF]::new([float]($cx + $inner * [Math]::Cos($a3)), [float]($cy + $inner * [Math]::Sin($a3))))
    $pts.Add([System.Drawing.PointF]::new([float]($cx + $inner * [Math]::Cos($a4)), [float]($cy + $inner * [Math]::Sin($a4))))
}

$g.FillPolygon($brush, $pts.ToArray())

# Punch a transparent hole in the center
$clearBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(0, 0, 0, 0))
$g.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
$g.FillEllipse($clearBrush, ($cx - $hole), ($cy - $hole), ($hole * 2), ($hole * 2))

$g.Dispose()
$bmp.Save("e:\coden\clientmod\src\main\resources\assets\modid\gear.png", [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Host "Gear icon generated successfully"
