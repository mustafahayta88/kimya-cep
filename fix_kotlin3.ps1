$root = "C:\Users\musta\OneDrive\Belgeler\KimyaUygulamasi\app\src\main\java"
$files = Get-ChildItem -Path $root -Recurse -Filter "*.kt" | Where-Object { -not $_.FullName.Contains('\build\') }

$total = 0
foreach ($f in $files) {
    $content = [System.IO.File]::ReadAllText($f.FullName)
    $orig = $content
    $changes = 0

    # 1. Numeric suffix I -> f (digits followed by I, word boundary)
    $content = [regex]::Replace($content, '(?<!\w)(\d+)I(?!\w)', {
        $changes++
        return $args[0].Groups[1].Value + 'f'
    })

    # 2. Format specifier %.Nd -> %.Nf (like %.6I, %.4I, %.0I)
    $content = [regex]::Replace($content, '%(\d*)\.(\d+)I', {
        $changes++
        $w = if ($args[0].Groups[1].Value) { $args[0].Groups[1].Value } else { "" }
        return "%${w}.$($args[0].Groups[2].Value)f"
    })

    # 3. isFaIeIoldText -> isFakeBoldText
    $content = $content -replace 'isFaIeIoldText', 'isFakeBoldText'

    # 4. Calculator.kt specific: RegResult field name I -> b
    $content = $content -replace 'data class RegResult\(val a: Double, val I: Double, val r2: Double\)', 'data class RegResult(val a: Double, val b: Double, val r2: Double)'
    $content = $content -replace 'return RegResult\(a, I, r2\)', 'return RegResult(a, b, r2)'

    # 5. Variable renames in specific files
    $content = $content -replace 'stoyIMol\b', 'stoikiMol'
    $content = $content -replace 'mI\b', 'mK'

    # 6. DrawBeaIer -> drawBeaker
    $content = $content -replace 'drawBeaIer\b', 'drawBeaker'
    $content = $content -replace 'laIel\b', 'label'
    $content = $content -replace 'cLaIel\b', 'cLabel'
    $content = $content -replace 'vLaIel\b', 'vLabel'

    # 7. DilutionView beaker variable renames (ILeIt -> bLeft etc.)
    $content = $content -replace 'ILeIt1\b', 'bLeft1'
    $content = $content -replace 'ILeIt2\b', 'bLeft2'
    $content = $content -replace 'IRight1\b', 'bRight1'
    $content = $content -replace 'IRight2\b', 'bRight2'
    $content = $content -replace '\bITop\b', 'bTop'
    $content = $content -replace 'IBottom\b', 'bBottom'
    $content = $content -replace '\bIH\b', 'bH'
    $content = $content -replace 'IillFrac\b', 'fillFrac'
    $content = $content -replace 'IillPaint\b', 'fillPaint'
    $content = $content -replace 'IorderPaint\b', 'borderPaint'
    $content = $content -replace 'IillTop\b', 'fillTop'
    $content = $content -replace 'IillPath\b', 'fillPath'

    # 8. Ig -> bg (background paint variable)
    $content = $content -replace '\bIg\b', 'bg'

    # 9. StoichiometryView box variable renames
    $content = $content -replace 'IoxW\b', 'boxW'
    $content = $content -replace 'IoxH\b', 'boxH'
    $content = $content -replace 'Iox1LeIt\b', 'box1Left'
    $content = $content -replace 'Iox2LeIt\b', 'box2Left'
    $content = $content -replace 'IoxTop\b', 'boxTop'
    $content = $content -replace 'IoxCenterY\b', 'boxCenterY'
    $content = $content -replace 'IoxPaint\b', 'boxPaint'
    $content = $content -replace 'IorderPaint\b', 'borderPaint'
    $content = $content -replace 'laIel1\b', 'label1'
    $content = $content -replace 'laIel2\b', 'label2'
    $content = $content -replace 'arrowLeIt\b', 'arrowLeft'

    # 10. coercein -> coerceIn (Kotlin stdlib)
    $content = $content -replace '\bcoercein\b', 'coerceIn'

    # 11. Iulunamadi -> bulunamadi (Turkish "not found")
    $content = $content -replace 'Iulunamadi', 'bulunamadi'

    # 12. stoyIiyometriHesapla -> stokiyometriHesapla
    $content = $content -replace 'stoyIiyometriHesapla\b', 'stokiyometriHesapla'

    # 13. StoyIiyometriFragment -> StokiyometriFragment (class name)
    $content = $content -replace 'class StoyIiyometriFragment', 'class StokiyometriFragment'

    if ($content -ne $orig) {
        [System.IO.File]::WriteAllText($f.FullName, $content, [System.Text.Encoding]::UTF8)
        Write-Host "Fixed: $($f.Name)"
        $total++
    }
}
Write-Host "`nTotal files fixed: $total"
