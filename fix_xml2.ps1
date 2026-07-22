function Generate-Layout {
    param([string]$LayoutFile, [hashtable]$IdTypes)
    $shortName = [System.IO.Path]::GetFileNameWithoutExtension($LayoutFile) -replace '^fragment_', ''
    
    $lines = New-Object System.Collections.ArrayList
    [void]$lines.Add('<?xml version="1.0" encoding="utf-8"?>')
    [void]$lines.Add('<!-- Generated from Kotlin fragment references -->')
    [void]$lines.Add('<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"')
    [void]$lines.Add('    android:layout_width="match_parent" android:layout_height="match_parent"')
    [void]$lines.Add('    android:background="@color/bg" android:padding="16dp">')
    [void]$lines.Add('    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"')
    [void]$lines.Add('        android:orientation="vertical">')
    [void]$lines.Add('        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"')
    [void]$lines.Add('            android:text="' + $shortName + '" android:textSize="24sp" android:textColor="#00F0FF"')
    [void]$lines.Add('            android:textStyle="bold"/>')
    
    foreach ($id in ($IdTypes.Keys | Sort-Object)) {
        $type = $IdTypes[$id]
        switch ($type) {
            'Button' { $at = 'Button' }
            'EditText' { $at = 'EditText' }
            'TextView' { $at = 'TextView' }
            'Spinner' { $at = 'Spinner' }
            default { $at = 'View' }
        }
        $hint = ($id -replace '^[^_]+_', '') -replace '_', ' '
        
        [void]$lines.Add('        <' + $at + ' android:id="@+id/' + $id + '"')
        if ($type -eq 'EditText') {
            [void]$lines.Add('            android:layout_width="match_parent" android:layout_height="48dp"')
            [void]$lines.Add('            android:hint="' + $hint + '" android:inputType="text"')
            [void]$lines.Add('            android:background="#161B22" android:padding="12dp"')
            [void]$lines.Add('            android:textColor="#FFF" android:textColorHint="#8B949E"')
            [void]$lines.Add('            android:layout_marginTop="8dp"/>')
        } elseif ($type -eq 'Button') {
            [void]$lines.Add('            android:layout_width="match_parent" android:layout_height="48dp"')
            [void]$lines.Add('            android:text="' + $hint + '" android:textColor="#FFF"')
            [void]$lines.Add('            android:backgroundTint="#00F0FF" android:layout_marginTop="8dp"/>')
        } elseif ($type -eq 'Spinner') {
            [void]$lines.Add('            android:layout_width="match_parent" android:layout_height="48dp"')
            [void]$lines.Add('            android:layout_marginTop="8dp" android:background="#161B22"/>')
        } else {
            [void]$lines.Add('            android:layout_width="match_parent" android:layout_height="48dp"')
            [void]$lines.Add('            android:layout_marginTop="8dp"/>')
        }
    }
    
    [void]$lines.Add('    </LinearLayout>')
    [void]$lines.Add('</ScrollView>')
    
    $result = $lines -join "`n"
    [System.IO.File]::WriteAllText($LayoutFile, $result, [System.Text.Encoding]::UTF8)
    Write-Host '  Generated: ' (Split-Path $LayoutFile -Leaf)
}

$javaRoot = "C:\Users\musta\OneDrive\Belgeler\KimyaUygulamasi\app\src\main\java\com\kimya\uygulama"
$resRoot = "C:\Users\musta\OneDrive\Belgeler\KimyaUygulamasi\app\src\main\res"
$skipList = @('fragment_birim', 'fragment_seyreltme', 'fragment_stoykiyometri', 'fragment_reaksiyon')

foreach ($dir in @("$javaRoot\fragments", "$javaRoot\features")) {
    if (-not (Test-Path $dir)) { continue }
    $fragmentFiles = Get-ChildItem -Path $dir -Filter "*.kt"
    foreach ($f in $fragmentFiles) {
        $content = [System.IO.File]::ReadAllText($f.FullName)
        $layoutMatch = [regex]::Match($content, 'R\.layout\.(\w+)')
        if (-not $layoutMatch.Success) { continue }
        $layoutName = $layoutMatch.Groups[1].Value
        if ($layoutName -in $skipList) { continue }
        
        $layoutFile = "$resRoot\layout\$layoutName.xml"
        if (-not (Test-Path $layoutFile)) {
            Write-Host 'Layout not found: ' $layoutName
            continue
        }
        
        $idTypes = @{}
        $viewMatches = [regex]::Matches($content, 'findViewById\s*<\s*(\w+)\s*>\s*\(\s*R\.id\.(\w+)\s*\)')
        foreach ($m in $viewMatches) { $idTypes[$m.Groups[2].Value] = $m.Groups[1].Value }
        $viewMatches2 = [regex]::Matches($content, '\.findViewById\s*<\s*(\w+)\s*>\s*\(\s*R\.id\.(\w+)\s*\)')
        foreach ($m in $viewMatches2) { $idTypes[$m.Groups[2].Value] = $m.Groups[1].Value }
        
        if ($idTypes.Count -eq 0) { continue }
        
        $xmlText = [System.IO.File]::ReadAllText($layoutFile)
        $xmlIds = [regex]::Matches($xmlText, 'android:id="@\+id/(\w+)"') | ForEach-Object { $_.Groups[1].Value }
        $missing = $idTypes.Keys | Where-Object { $_ -notin $xmlIds }
        
        if ($missing.Count -gt 0) {
            Write-Host 'Regenerating ' $layoutName ' - ' $missing.Count ' missing IDs for ' $f.Name
            Generate-Layout $layoutFile $idTypes
        }
    }
}
Write-Host 'Done!'
