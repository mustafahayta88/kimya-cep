$root = "C:\Users\musta\OneDrive\Belgeler\KimyaUygulamasi\app\src\main\java\com\kimya\uygulama"
$files = Get-ChildItem -Path $root -Recurse -Filter "*.kt" | Where-Object { -not $_.FullName.Contains('\build\') }
$fixCount = 0
foreach ($f in $files) {
    $content = [System.IO.File]::ReadAllText($f.FullName)
    $orig = $content
    # Color.argb
    $content = $content -replace '\bargI\b', 'argb'
    # Array functions
    $content = $content -replace '\bIntArrayOI\b', 'intArrayOf'
    $content = $content -replace '\bIloatArrayOI\b', 'floatArrayOf'
    $content = $content -replace '\barrayOI\b', 'arrayOf'
    # Toast.makeText
    $content = $content -replace '\bmaIeText\b', 'makeText'
    # String functions
    $content = $content -replace '\bisBlanI\b', 'isBlank'
    $content = $content -replace '\bremovePreIix\b', 'removePrefix'
    $content = $content -replace '\bisNotBlanI\b', 'isNotBlank'
    # Drawable
    $content = $content -replace '\bdrawaIle\b', 'drawable'
    $content = $content -replace '\bGradientDrawaIle\b', 'GradientDrawable'
    # Background / Enabled
    $content = $content -replace '\bIacIground\b', 'background'
    $content = $content -replace '\bisEnaIled\b', 'isEnabled'
    # Iteration
    $content = $content -replace '\bforEachindexed\b', 'forEachIndexed'
    # TextWatcher
    $content = $content -replace '\bIeIoreTextChanged\b', 'beforeTextChanged'
    $content = $content -replace '\baIterTextChanged\b', 'afterTextChanged'
    $content = $content -replace '\bEditaIle\b', 'Editable'
    # Turkish words
    $content = $content -replace '\bozelliI\b', 'ozellik'
    $content = $content -replace '\bmoleIulKutlesiHesapla\b', 'molekulKutlesiHesapla'
    $content = $content -replace '\bguvenliI\b', 'guvenlik'
    $content = $content -replace '\bteoriI\b', 'teorik'
    # Comments/strings
    $content = $content -replace '\bIotlin\b', 'kotlin'
    $content = $content -replace '\bSpannaIleStringBuilder\b', 'SpannableStringBuilder'
    $content = $content -replace '\bMenuitem\b', 'MenuItem'
    # Misc
    $content = $content -replace '\bnot_Iaydet\b', 'not_kaydet'
    $content = $content -replace '\bnot_Iaydet_dosya\b', 'not_kaydet_dosya'
    $content = $content -replace '\bmaxOIOrNull\b', 'maxOfOrNull'
    $content = $content -replace '\bperiyodiIVeri\b', 'periyodikVeri'
    $content = $content -replace '\bsetOnitemClicIListener\b', 'setOnItemClickListener'
    $content = $content -replace '\bsetStroIe\b', 'setStroke'
    $content = $content -replace '\bcoercein\b', 'coerceIn'
    # filesDir, favFile, fileName
    $content = $content -replace '\bIilesDir\b', 'filesDir'
    $content = $content -replace '\bIavFile\b', 'favFile'
    $content = $content -replace '\bIile\b', 'file'
    $content = $content -replace '\bIilename\b', 'filename'
    # int - Int (type)
    $content = $content -replace '\bas\?\s+int\b', 'as? Int'
    $content = $content -replace '\b:\s*int\b', ': Int'
    # PdIExporter - PdfExporter
    $content = $content -replace '\bPdIExporter\b', 'PdfExporter'
    if ($content -ne $orig) {
        [System.IO.File]::WriteAllText($f.FullName, $content, [System.Text.Encoding]::UTF8)
        Write-Host "Fixed: $($f.Name)"
        $fixCount++
    }
}
Write-Host "Total files fixed: $fixCount"
