$source = "C:\Users\Nikhi\.gemini\antigravity\brain\99d53a61-fde2-4dce-a3fb-e6cf87aeb121\moodify_app_icon_v2_1776695928830.png"
$res = "C:\Users\Nikhi\Downloads\ITW Project\Moodify\app\src\main\res"

Add-Type -AssemblyName System.Drawing
$img = [System.Drawing.Image]::FromFile($source)

$sizes = @{
    "mdpi" = 48
    "hdpi" = 72
    "xhdpi" = 96
    "xxhdpi" = 144
    "xxxhdpi" = 192
}

foreach ($size in $sizes.GetEnumerator()) {
    $folder = "$res\mipmap-$($size.Key)"
    if (!(Test-Path $folder)) { New-Item -ItemType Directory -Path $folder | Out-Null }
    
    $bmp = New-Object System.Drawing.Bitmap($img, $size.Value, $size.Value)
    $bmp.Save("$folder\ic_launcher.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Save("$folder\ic_launcher_round.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    
    if (Test-Path "$folder\ic_launcher.webp") { Remove-Item "$folder\ic_launcher.webp" }
    if (Test-Path "$folder\ic_launcher_round.webp") { Remove-Item "$folder\ic_launcher_round.webp" }
    if (Test-Path "$folder\ic_launcher.xml") { Remove-Item "$folder\ic_launcher.xml" }
    if (Test-Path "$folder\ic_launcher_round.xml") { Remove-Item "$folder\ic_launcher_round.xml" }
}

$img.Dispose()

if (Test-Path "$res\mipmap-anydpi-v26\ic_launcher.xml") { Remove-Item "$res\mipmap-anydpi-v26\ic_launcher.xml" }
if (Test-Path "$res\mipmap-anydpi-v26\ic_launcher_round.xml") { Remove-Item "$res\mipmap-anydpi-v26\ic_launcher_round.xml" }

Write-Host "Icons successfully generated and replaced!"
