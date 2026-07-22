$javaRoot = "C:\Users\musta\OneDrive\Belgeler\KimyaUygulamasi\app\src\main\java\com\kimya\uygulama"
$resRoot = "C:\Users\musta\OneDrive\Belgeler\KimyaUygulamasi\app\src\main\res"
$ktFiles = Get-ChildItem -Path $javaRoot -Recurse -Filter "*.kt" | Where-Object { -not $_.FullName.Contains('\build\') }

Write-Host "=== PASS 1: Fix standard corruption patterns across ALL .kt files ==="
$fixCount = 0
foreach ($f in $ktFiles) {
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
    
    # Icons/actionbar
    $content = $content -replace '\bsetOnitemClicIListener\b', 'setOnItemClickListener'
    $content = $content -replace '\bsetStroIe\b', 'setStroke'
    
    # coerceIn (Kotlin stdlib)
    $content = $content -replace '\bcoercein\b', 'coerceIn'
    
    # filesDir, favFile, fileName corruptions
    $content = $content -replace '\bIilesDir\b', 'filesDir'
    $content = $content -replace '\bIavFile\b', 'favFile'
    $content = $content -replace '\bIile\b', 'file'
    $content = $content -replace '\bIilename\b', 'filename'
    
    # int -> Int (type)
    $content = $content -replace '\bas\?\s+int\b', 'as? Int'
    $content = $content -replace '\b:\s*int\b', ': Int'
    
    # Kotlin stdlib / Android
    $content = $content -replace '\bintent\b', 'intent'
    $content = $content -replace '\bPdIExporter\b', 'PdfExporter'
    
    if ($content -ne $orig) {
        [System.IO.File]::WriteAllText($f.FullName, $content, [System.Text.Encoding]::UTF8)
        Write-Host "  Fixed: $($f.Name)"
        $fixCount++
    }
}
Write-Host "  $fixCount files updated"

Write-Host "`n=== PASS 2: Find R.id references and identify missing XML layouts ==="

# Build a map of R.id references per fragment file
$fragmentDir = "$javaRoot\fragments"
$featuresDir = "$javaRoot\features"
$allDirs = @($fragmentDir, $featuresDir)

$fragmentLayouts = @{} # layout_name -> list of R.id references with types

foreach ($dir in $allDirs) {
    if (-not (Test-Path $dir)) { continue }
    $fragmentFiles = Get-ChildItem -Path $dir -Filter "*.kt"
    foreach ($f in $fragmentFiles) {
        $content = [System.IO.File]::ReadAllText($f.FullName)
        
        # Find layout reference
        $layoutMatch = [regex]::Match($content, 'R\.layout\.(\w+)')
        if (-not $layoutMatch.Success) { continue }
        $layoutName = "fragment_$($layoutMatch.Groups[1].Value -replace '^fragment_', '')"
        if ($layoutMatch.Groups[1].Value -match '^fragment_') { $layoutName = $layoutMatch.Groups[1].Value }
        else { $layoutName = $layoutMatch.Groups[1].Value }
        $layoutFile = "$resRoot\layout\$layoutName.xml"
        
        # Find all findViewById<Type>(R.id.xxx) patterns
        $idRefs = @()
        $viewMatches = [regex]::Matches($content, 'findViewById\s*<\s*(\w+)\s*>\s*\(\s*R\.id\.(\w+)\s*\)')
        foreach ($m in $viewMatches) {
            $type = $m.Groups[1].Value
            $id = $m.Groups[2].Value
            $idRefs += [PSCustomObject]@{ Id = $id; Type = $type }
        }
        
        # Also find patterns like v.findViewById<Button>(R.id.xxx) 
        $viewMatches2 = [regex]::Matches($content, '(\w+)\.findViewById\s*<\s*(\w+)\s*>\s*\(\s*R\.id\.(\w+)\s*\)')
        foreach ($m in $viewMatches2) {
            $type = $m.Groups[2].Value
            $id = $m.Groups[3].Value
            # Check if we already have this ID
            $existing = $idRefs | Where-Object { $_.Id -eq $id }
            if (-not $existing) {
                $idRefs += [PSCustomObject]@{ Id = $id; Type = $type }
            }
        }
        
        $idRefs = $idRefs | Sort-Object Id -Unique
        
        if ($idRefs.Count -gt 0) {
            $fragmentLayouts[$f.FullName] = @{
                LayoutFile = $layoutFile
                LayoutName = $layoutName
                IdRefs = $idRefs
            }
        }
    }
}

Write-Host "`n=== PASS 3: Generate missing XML layouts ==="
$generatedCount = 0
foreach ($fpath in $fragmentLayouts.Keys) {
    $info = $fragmentLayouts[$fpath]
    $layoutFile = $info.LayoutFile
    $layoutName = $info.LayoutName
    $idRefs = $info.IdRefs
    
    # Check if layout file exists
    if (-not (Test-Path $layoutFile)) {
        Write-Host "  LAYOUT MISSING: $layoutName.xml (referenced by $(Split-Path $fpath -Leaf))"
        # Generate minimal layout
        $xml = '<?xml version="1.0" encoding="utf-8"?>'
        $xml += "`n<ScrollView xmlns:android=""http://schemas.android.com/apk/res/android"""
        $xml += "`n    android:layout_width=""match_parent"" android:layout_height=""match_parent"""
        $xml += "`n    android:background=""@color/bg"" android:padding=""16dp"">"
        $xml += "`n    <LinearLayout android:layout_width=""match_parent"" android:layout_height=""wrap_content"""
        $xml += "`n        android:orientation=""vertical"">"
        $xml += "`n        <TextView android:layout_width=""wrap_content"" android:layout_height=""wrap_content"""
        $xml += "`n            android:text="""${layoutName}"" android:textSize=""24sp"" android:textColor=""#00F0FF"""
        $xml += "`n            android:textStyle=""bold""/>"
        
        foreach ($ref in $idRefs) {
            $type = $ref.Type
            $id = $ref.Id
            $androidType = switch ($type) {
                'Button' { 'Button' }
                'EditText' { 'EditText' }
                'TextView' { 'TextView' }
                'Spinner' { 'Spinner' }
                'View' { 'View' }
                'ListView' { 'ListView' }
                'ImageView' { 'ImageView' }
                'CheckBox' { 'CheckBox' }
                'RadioButton' { 'RadioButton' }
                default { 'View' }
            }
            $hint = $id -replace '^[^_]+_', '' -replace '_', ' '
            if ($type -eq 'EditText') {
                $xml += "`n        <$androidType android:id=""@+id/$id"" android:layout_width=""match_parent"""
                $xml += "`n            android:layout_height=""48dp"" android:hint=""$hint"" android:inputType=""text"""
                $xml += "`n            android:background=""#161B22"" android:padding=""12dp"" android:textColor=""#FFF"""
                $xml += "`n            android:textColorHint=""#8B949E"" android:layout_marginTop=""8dp""/>"
            } elseif ($type -eq 'Button') {
                $xml += "`n        <$androidType android:id=""@+id/$id"" android:layout_width=""match_parent"""
                $xml += "`n            android:layout_height=""48dp"" android:text=""$hint"" android:textColor=""#FFF"""
                $xml += "`n            android:backgroundTint=""#00F0FF"" android:layout_marginTop=""8dp""/>"
            } elseif ($type -eq 'Spinner') {
                $xml += "`n        <$androidType android:id=""@+id/$id"" android:layout_width=""match_parent"""
                $xml += "`n            android:layout_height=""48dp"" android:layout_marginTop=""8dp"""
                $xml += "`n            android:background=""#161B22""/>"
            } else {
                $xml += "`n        <$androidType android:id=""@+id/$id"" android:layout_width=""match_parent"""
                $xml += "`n            android:layout_height=""48dp"" android:layout_marginTop=""8dp""/>"
            }
        }
        
        $xml += "`n    </LinearLayout>"
        $xml += "`n</ScrollView>"
        
        [System.IO.File]::WriteAllText($layoutFile, $xml, [System.Text.Encoding]::UTF8)
        Write-Host "  -> Generated: $layoutName.xml with $($idRefs.Count) views"
        $generatedCount++
    }
}
if ($generatedCount -eq 0) {
    Write-Host "  All layout files already exist (they may still have wrong IDs though)"
}

Write-Host "`n=== PASS 4: Regenerate ALL fragment XML layouts with correct IDs ==="
$updatedCount = 0
$skipFragments = @('fragment_birim', 'fragment_seyreltme', 'fragment_stoykiyometri', 'fragment_reaksiyon')
foreach ($fpath in $fragmentLayouts.Keys) {
    $info = $fragmentLayouts[$fpath]
    $layoutFile = $info.LayoutFile
    $idRefs = $info.IdRefs
    
    if (-not (Test-Path $layoutFile)) { continue }
    if ($info.LayoutName -in $skipFragments) { continue }
    
    $xmlContent = [System.IO.File]::ReadAllText($layoutFile)
    $xmlIds = [regex]::Matches($xmlContent, 'android:id="@\+id/(\w+)"') | ForEach-Object { $_.Groups[1].Value }
    $missingIds = $idRefs | Where-Object { $_.Id -notin $xmlIds }
    
    if ($missingIds.Count -gt 0) {
        Write-Host "  $(Split-Path $layoutFile -Leaf): $($missingIds.Count) IDs missing, regenerating..."
        
        $shortName = $info.LayoutName -replace '^fragment_', ''
        if ($shortName -match '^fragment_') { $shortName = $shortName -replace '^fragment_', '' }
        
        $xml = '<?xml version="1.0" encoding="utf-8"?>'
        $xml += "`n<ScrollView xmlns:android=""http://schemas.android.com/apk/res/android"""
        $xml += "`n    android:layout_width=""match_parent"" android:layout_height=""match_parent"""
        $xml += "`n    android:background=""@color/bg"" android:padding=""16dp"">"
        $xml += "`n    <LinearLayout android:layout_width=""match_parent"" android:layout_height=""wrap_content"""
        $xml += "`n        android:orientation=""vertical"">"
        $xml += "`n        <TextView android:layout_width=""wrap_content"" android:layout_height=""wrap_content"""
        $xml += "`n            android:text="""${shortName}"" android:textSize=""24sp"" android:textColor=""#00F0FF"""
        $xml += "`n            android:textStyle=""bold""/>"
        
        foreach ($ref in $idRefs) {
            $type = $ref.Type
            $id = $ref.Id
            $androidType = switch ($type) {
                'Button' { 'Button' }
                'EditText' { 'EditText' }
                'TextView' { 'TextView' }
                'Spinner' { 'Spinner' }
                'View' { 'View' }
                'ListView' { 'ListView' }
                'ImageView' { 'ImageView' }
                'CheckBox' { 'CheckBox' }
                'RadioButton' { 'RadioButton' }
                default { 'View' }
            }
            $hint = $id -replace '^[^_]+_', '' -replace '_', ' '
            if ($type -eq 'EditText') {
                $xml += "`n        <$androidType android:id=""@+id/$id"" android:layout_width=""match_parent"""
                $xml += "`n            android:layout_height=""48dp"" android:hint=""$hint"" android:inputType=""text"""
                $xml += "`n            android:background=""#161B22"" android:padding=""12dp"" android:textColor=""#FFF"""
                $xml += "`n            android:textColorHint=""#8B949E"" android:layout_marginTop=""8dp""/>"
            } elseif ($type -eq 'Button') {
                $xml += "`n        <$androidType android:id=""@+id/$id"" android:layout_width=""match_parent"""
                $xml += "`n            android:layout_height=""48dp"" android:text=""$hint"" android:textColor=""#FFF"""
                $xml += "`n            android:backgroundTint=""#00F0FF"" android:layout_marginTop=""8dp""/>"
            } elseif ($type -eq 'Spinner') {
                $xml += "`n        <$androidType android:id=""@+id/$id"" android:layout_width=""match_parent"""
                $xml += "`n            android:layout_height=""48dp"" android:layout_marginTop=""8dp"""
                $xml += "`n            android:background=""#161B22""/>"
            } else {
                $xml += "`n        <$androidType android:id=""@+id/$id"" android:layout_width=""match_parent"""
                $xml += "`n            android:layout_height=""48dp"" android:layout_marginTop=""8dp""/>"
            }
        }
        
        $xml += "`n    </LinearLayout>"
        $xml += "`n</ScrollView>"
        
        [System.IO.File]::WriteAllText($layoutFile, $xml, [System.Text.Encoding]::UTF8)
        Write-Host "    -> Regenerated $($info.LayoutName).xml"
        $updatedCount++
    }
}

Write-Host "`n=== Summary ==="
Write-Host "Standard corruption fixes applied to $fixCount files"
Write-Host "XML layouts generated: $generatedCount"
Write-Host "XML layouts updated: $updatedCount"
Write-Host "Done!"
