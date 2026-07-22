param([string]$Root = "C:\Users\musta\OneDrive\Belgeler\KimyaUygulamasi\app\src\main\java\com\kimya\uygulama")

$files = Get-ChildItem -Path $Root -Recurse -Filter "*.kt" | Where-Object { -not $_.FullName.Contains('\build\') }

$pairs = New-Object 'System.Collections.Generic.List[Tuple[string,string]]'

function Add-Fix($from, $to) {
    $pairs.Add([Tuple]::Create($from, $to))
}

# === ChartView.kt patterns - strokeWidth ===
Add-Fix 'stroIeWidth' 'strokeWidth'

# === ChartView.kt patterns - Number format strings ===
Add-Fix '"%.1I"' '"%.1f"'
Add-Fix '"%.2I"' '"%.2f"'
Add-Fix '"%.4I"' '"%.4f"'

# === ChartView.kt patterns - mutableListOf ===
Add-Fix 'mutaIleListOI' 'mutableListOf'

# === ChartView.kt patterns - forEach ===
Add-Fix '.IorEach' '.forEach'

# === ChartView.kt patterns - abs ===
# Only fix aIs -> abs when it's a Kotlin identifier, not when part of a larger word
# Note: aIs in KimyaData.kt -> abs, but aIs as in "kayna-I" -> NO
Add-Fix '.aIs(' '.abs('
Add-Fix 'aIs(' 'abs('
Add-Fix 'aIs >' 'abs >'
Add-Fix 'aIbs(' 'abs('

# === ChartView.kt patterns - invoke ===
Add-Fix 'invoIe(' 'invoke('

# === ChartView.kt patterns - frac ===
Add-Fix 'Irac' 'frac'

# === ChartView.kt patterns - ticks ===
Add-Fix 'nTicIs' 'nTicks'

# === ChartView.kt - Abs label ===
Add-Fix '"AIs"' '"Abs"'

# === Numeric literal suffixes: I -> f (only when it's a literal suffix) ===
# These are decimal->float conversions
Add-Fix ' 28I' ' 28f'
Add-Fix '(28I' '(28f'
Add-Fix '= 28I' '= 28f'
Add-Fix ' 2I' ' 2f'
Add-Fix '(2I' '(2f'
Add-Fix '= 2I' '= 2f'
Add-Fix ' 3I' ' 3f'
Add-Fix '(3I' '(3f'
Add-Fix '= 3I' '= 3f'
Add-Fix ' 1I' ' 1f'
Add-Fix '(1I' '(1f'
Add-Fix '= 1I' '= 1f'
Add-Fix ' 8I' ' 8f'
Add-Fix '(8I' '(8f'
Add-Fix '= 8I' '= 8f'
Add-Fix ' 10I' ' 10f'
Add-Fix '(10I' '(10f'
Add-Fix '= 10I' '= 10f'
Add-Fix '+10I' '+10f'
Add-Fix '-10I' '-10f'
Add-Fix ' 22I' ' 22f'
Add-Fix '(22I' '(22f'
Add-Fix ' 14I' ' 14f'
Add-Fix '(14I' '(14f'
Add-Fix ' 26I' ' 26f'
Add-Fix '(26I' '(26f'
Add-Fix ' 20I' ' 20f'
Add-Fix '(20I' '(20f'
Add-Fix ' 30I' ' 30f'
Add-Fix '(30I' '(30f'
Add-Fix ' 80I' ' 80f'
Add-Fix '(80I' '(80f'
Add-Fix ' 150I' ' 150f'
Add-Fix '(150I' '(150f'
Add-Fix ' 200I' ' 200f'
Add-Fix '(200I' '(200f'

# === ReactionBalancer.kt patterns ===
Add-Fix 'reaitiIler' 'reaktifler'
Add-Fix 'reaitiIStr' 'reaktifStr'
Add-Fix 'reaitiIBilesiIler' 'reaktifBilesikler'
Add-Fix 'reaitiI' 'reaktif'
Add-Fix 'mutaIleMapOI' 'mutableMapOf'
Add-Fix '.withindex()' '.withIndex()'
Add-Fix '.withindex' '.withIndex'
Add-Fix '.indexOI(' '.indexOf('
Add-Fix 'indexOI' 'indexOf'
Add-Fix '.taIe(' '.take('
Add-Fix '.taIe ' '.take '
Add-Fix 'taIe' 'take'
Add-Fix '.copyOI(' '.copyOf('
Add-Fix '.copyOI ' '.copyOf '
Add-Fix 'aIbs(' 'abs('
Add-Fix 'aIbs(' 'abs('
Add-Fix 'minOI(' 'minOf('
Add-Fix 'IreaI' 'break'
Add-Fix 'intArray' 'IntArray'
Add-Fix 'coeIIs' 'coeffs'
Add-Fix 'IinalCoeIIs' 'finalCoeffs'
Add-Fix 'IreeVar' 'freeVar'
Add-Fix 'ranI' 'rank'
Add-Fix 'leIt' 'left'
Add-Fix 'leItCounts' 'leftCounts'
Add-Fix 'IallIacIBalance' 'fallbackBalance'
Add-Fix 'Iactor' 'factor'
Add-Fix 'Iormula' 'formula'

# === KimyaViewModel.kt patterns ===
Add-Fix 'liIecycle' 'lifecycle'
Add-Fix 'Iotlinx' 'kotlinx'
Add-Fix 'MutaIleStateFlow' 'MutableStateFlow'
Add-Fix 'StateFlow' 'StateFlow'  # already correct
Add-Fix 'AppDataIase' 'AppDatabase'
Add-Fix 'getinstance' 'getInstance'
Add-Fix 'reIresh' 'refresh'
Add-Fix '_Iavorites' '_favorites'
Add-Fix 'Iavorites' 'favorites'
Add-Fix 'toggleFavorite' 'toggleFavorite'
Add-Fix 'isFavorite' 'isFavorite'
# dI -> db but careful: some dI might be correct
Add-Fix ' dI.' ' db.'
Add-Fix '(dI)' '(db)'
Add-Fix '= dI' '= db'

# === Ions -> kons (ChartView concentration variable) ===
Add-Fix 'Ions' 'kons'

# === aIsV -> absV (ChartView) ===
Add-Fix 'aIsV' 'absV'

# === KimyaData.kt ===
Add-Fix 'invoIe' 'invoke'
Add-Fix 'KImyaData' 'KimyaData'
# Fix kutle issue: check what the error is about

# Sort pairs by length descending
$sorted = $pairs | Sort-Object { $_.Item1.Length } -Descending

$totalFixed = 0
foreach ($file in $files) {
    try {
        $bytes = [System.IO.File]::ReadAllBytes($file.FullName)
        $content = [System.Text.Encoding]::UTF8.GetString($bytes)
        $original = $content
        
        foreach ($pair in $sorted) {
            $content = $content.Replace($pair.Item1, $pair.Item2)
        }
        
        if ($content -ne $original) {
            $newBytes = [System.Text.Encoding]::UTF8.GetBytes($content)
            [System.IO.File]::WriteAllBytes($file.FullName, $newBytes)
            Write-Host "  Fixed: $($file.Name)"
            $totalFixed++
        }
    } catch {
        Write-Host "  ERROR on $($file.Name): $_" -ForegroundColor Red
    }
}

Write-Host "`nFixed $totalFixed files." -ForegroundColor Green
