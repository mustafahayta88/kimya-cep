$javaRoot = "C:\Users\musta\OneDrive\Belgeler\KimyaUygulamasi\app\src\main\java\com\kimya\uygulama"
$resRoot = "C:\Users\musta\OneDrive\Belgeler\KimyaUygulamasi\app\src\main\res"

$fragmentDir = "$javaRoot\fragments"
$featuresDir = "$javaRoot\features"

$fragmentLayouts = @{}

foreach ($dir in @($fragmentDir, $featuresDir)) {
    if (-not (Test-Path $dir)) { continue }
    $fragmentFiles = Get-ChildItem -Path $dir -Filter "*.kt"
    foreach ($f in $fragmentFiles) {
        $content = [System.IO.File]::ReadAllText($f.FullName)
        $layoutMatch = [regex]::Match($content, 'R\.layout\.(\w+)')
        if (-not $layoutMatch.Success) { continue }
        $layoutName = $layoutMatch.Groups[1].Value
        $layoutFile = "$resRoot\layout\$layoutName.xml"
        
        # Find all findViewById(Type) patterns
        $idRefs = @()
        $viewMatches = [regex]::Matches($content, 'findViewById\s*<\s*(\w+)\s*>\s*\(\s*R\.id\.(\w+)\s*\)')
        foreach ($m in $viewMatches) {
            $type = $m.Groups[1].Value
            $id = $m.Groups[2].Value
            if ($id -notin ($idRefs | ForEach-Object { $_[0] })) {
                $idRefs += @($id, $type)
            }
        }
        $viewMatches2 = [regex]::Matches($content, '(\w+)\.findViewById\s*<\s*(\w+)\s*>\s*\(\s*R\.id\.(\w+)\s*\)')
        foreach ($m in $viewMatches2) {
            $type = $m.Groups[2].Value
            $id = $m.Groups[3].Value
            if ($id -notin ($idRefs | ForEach-Object { $_[0] })) {
                $idRefs += @($id, $type)
            }
        }
        
        $unique = @{}
        for ($i = 0; $i -lt $idRefs.Length; $i += 2) {
            $unique[$idRefs[$i]] = $idRefs[$i + 1]
        }
        
        if ($unique.Count -gt 0) {
            $fragmentLayouts[$f.FullName] = @{
                LayoutFile = $layoutFile
                LayoutName = $layoutName
                IdTypes = $unique
            }
        }
    }
}

$skipList = @('fragment_birim', 'fragment_seyreltme', 'fragment_stoykiyometri', 'fragment_reaksiyon')
$count = 0

foreach ($fpath in $fragmentLayouts.Keys) {
    $info = $fragmentLayouts[$fpath]
    $layoutFile = $info.LayoutFile
    $layoutName = $info.LayoutName
    $idTypes = $info.IdTypes
    
    if ($layoutName -in $skipList) { continue }
    if (-not (Test-Path $layoutFile)) {
        Write-Host "MISSING: $layoutName.xml - generating"
        $needsGen = $true
    } else {
        $xmlText = [System.IO.File]::ReadAllText($layoutFile)
        $xmlIds = [regex]::Matches($xmlText, 'android:id="@\+id/(\w+)"') | ForEach-Object { $_.Groups[1].Value }
        $missing = $idTypes.Keys | Where-Object { $_ -notin $xmlIds }
        if ($missing.Count -gt 0) {
            Write-Host "NEEDS UPDATE: $layoutName.xml for $(Split-Path $fpath -Leaf) - $($missing.Count) missing IDs"
            $needsGen = $true
        } else {
            $needsGen = $false
        }
    }
    
    if ($needsGen) {
        $shortName = $layoutName -replace '^fragment_', ''
        $lines = @()
        $lines += '<?xml version="1.0" encoding="utf-8"?>'
        $lines += '<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"'
        $lines += '    android:layout_width="match_parent" android:layout_height="match_parent"'
        $lines += '    android:background="@color/bg" android:padding="16dp">'
        $lines += '    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"'
        $lines += '        android:orientation="vertical">'
        $lines += '        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"'
        $lines += "            android:text=""$shortName"" android:textSize=""24sp"" android:textColor=""#00F0FF"""
        $lines += '            android:textStyle="bold"/>'
        
        foreach ($id in ($idTypes.Keys | Sort-Object)) {
            $type = $idTypes[$id]
            switch ($type) {
                'Button' { $androidType = 'Button' }
                'EditText' { $androidType = 'EditText' }
                'TextView' { $androidType = 'TextView' }
                'Spinner' { $androidType = 'Spinner' }
                default { $androidType = 'View' }
            }
            $hint = ($id -replace '^[^_]+_', '') -replace '_', ' '
            if ($type -eq 'EditText') {
                $lines += "        <$androidType android:id=""@+id/$id"" android:layout_width=""match_parent"""
                $lines += "            android:layout_height=""48dp"" android:hint=""$hint"" android:inputType=""text"""
                $lines += "            android:background=""#161B22"" android:padding=""12dp"" android:textColor=""#FFF"""
                $lines += "            android:textColorHint=""#8B949E"" android:layout_marginTop=""8dp""/>"
            } elseif ($type -eq 'Button') {
                $lines += "        <$androidType android:id=""@+id/$id"" android:layout_width=""match_parent"""
                $lines += "            android:layout_height=""48dp"" android:text=""$hint"" android:textColor=""#FFF"""
                $lines += "            android:backgroundTint=""#00F0FF"" android:layout_marginTop=""8dp""/>"
            } elseif ($type -eq 'Spinner') {
                $lines += "        <$androidType android:id=""@+id/$id"" android:layout_width=""match_parent"""
                $lines += "            android:layout_height=""48dp"" android:layout_marginTop=""8dp"""
                $lines += "            android:background=""#161B22""/>"
            } else {
                $lines += "        <$androidType android:id=""@+id/$id"" android:layout_width=""match_parent"""
                $lines += "            android:layout_height=""48dp"" android:layout_marginTop=""8dp""/>"
            }
        }
        
        $lines += '    </LinearLayout>'
        $lines += '</ScrollView>'
        
        [System.IO.File]::WriteAllText($layoutFile, $lines -join "`n", [System.Text.Encoding]::UTF8)
        Write-Host "  -> Generated: $layoutName.xml"
        $count++
    }
}
Write-Host "XML layouts generated/updated: $count"
