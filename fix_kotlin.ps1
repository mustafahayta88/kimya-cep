param([string]$Root = "C:\Users\musta\OneDrive\Belgeler\KimyaUygulamasi\app\src\main\java\com\kimya\uygulama")

$files = Get-ChildItem -Path $Root -Recurse -Filter "*.kt" | Where-Object { -not $_.FullName.Contains('\build\') }

# Build replacement map: longer strings first to avoid partial matches
$pairs = New-Object 'System.Collections.Generic.List[Tuple[string,string]]'

function Add-Fix($from, $to) {
    $pairs.Add([Tuple]::Create($from, $to))
}

# === PACKAGE / IMPORT lines ===
Add-Fix 'com.Iimya.uygulama.Iragments' 'com.kimya.uygulama.fragments'
Add-Fix 'com.Iimya.uygulama.Ieatures'  'com.kimya.uygulama.features'
Add-Fix 'com.Iimya.uygulama.utIl'     'com.kimya.uygulama.util'
Add-Fix 'com.Iimya.uygulama.utils'    'com.kimya.uygulama.utils'
Add-Fix 'com.Iimya.uygulama'          'com.kimya.uygulama'

# Fragment import
Add-Fix 'import androidx.Iragment.app.Fragment' 'import androidx.fragment.app.Fragment'
Add-Fix 'import androidx.Iragment'              'import androidx.fragment'

# LayoutInflater import
Add-Fix 'import android.view.LayoutinIlater' 'import android.view.LayoutInflater'

# Kotlin math
Add-Fix 'import Iotlin.math' 'import kotlin.math'

# === PACKAGE keyword ===
Add-Fix 'pacIage ' 'package '

# === COMMON mis-corrupted type: int -> Int (from our earlier In->in fix) ===
# These are Kotlin type usages, not the lowercase 'int' keyword
# Only fix where Int is used as a TYPE (after :, as type param, etc.)
Add-Fix ': int' ': Int'
Add-Fix '<int>' '<Int>'
Add-Fix '(int)' '(Int)'
Add-Fix ', int' ', Int'
Add-Fix 'int?' 'Int?'
Add-Fix 'int,' 'Int,'

# === mapOf / listOf / setOf ===
Add-Fix 'mapOI' 'mapOf'
Add-Fix 'listOI' 'listOf'
Add-Fix 'setOI' 'setOf'

# === Boolean ===
Add-Fix 'Ialse' 'false'
Add-Fix 'Irue' 'true'
Add-Fix 'DouIle' 'Double'
Add-Fix 'DouIle?' 'Double?'

# === Kotlin keywords ===
Add-Fix ' Iun ' ' fun '
Add-Fix '(Iun ' '(fun '
Add-Fix 'Iun: ' 'fun: '
Add-Fix 'Iun<' 'fun<'
Add-Fix 'Iun (' 'fun ('
Add-Fix 'Iun<' 'fun<'
Add-Fix 'override Iun' 'override fun'
Add-Fix 'private Iun' 'private fun'
Add-Fix 'prIvate Iun' 'private fun'
Add-Fix ' Iun(' ' fun('
Add-Fix ' Iun<' ' fun<'
Add-Fix ' Iun {' ' fun {'
Add-Fix '(\nIun ' '(\nfun '

# if keyword - handle both 'If' and 'iI' corruptions
Add-Fix 'iI (' 'if ('
Add-Fix 'iI(' 'if('
Add-Fix 'iI ' 'if '
Add-Fix 'iI.' 'if.'
Add-Fix 'iI!' 'if!'
Add-Fix ' iI (' ' if ('
Add-Fix ' iI(' ' if('
Add-Fix 'iI {' 'if {'
Add-Fix ' iI {' ' if {'
Add-Fix 'iI\n' 'if\n'
Add-Fix ' iI ' ' if '
Add-Fix 'else iI' 'else if'
Add-Fix ' else iI' ' else if'

# 'If' -> 'if'
Add-Fix ' If (' ' if ('
Add-Fix ' If(' ' if('
Add-Fix '(If (' '(if ('
Add-Fix '(If(' '(if('
Add-Fix '\nIf (' '\nif ('
Add-Fix '\nIf(' '\nif('

# for keyword
Add-Fix ' Ior ' ' for '
Add-Fix '(Ior ' '(for '
Add-Fix ' Ior(' '(for('
Add-Fix 'Ior (' 'for ('
Add-Fix 'Ior(' 'for('

# in keyword
Add-Fix ' In ' ' in '
Add-Fix '(In ' '(in '
Add-Fix ' In:' ' in:'
Add-Fix ' In(' ' in('
Add-Fix ' In {' ' in {'
Add-Fix '\nIn ' '\nin '

# is keyword
Add-Fix ' Is ' ' is '
Add-Fix '(Is ' '(is '
Add-Fix ' Is:' ' is:'
Add-Fix '\nIs ' '\nis '

# object keyword
Add-Fix ' oIject ' ' object '
Add-Fix '(oIject ' '(object '
Add-Fix 'oIject :' 'object :'
Add-Fix ' oIject:' ' object:'
Add-Fix 'oIject ' 'object '

# by keyword (only for 'by lazy' pattern)
Add-Fix ' Iy lazy' ' by lazy'
Add-Fix '(Iy lazy' '(by lazy'

# as keyword
Add-Fix ' as? ' ' as? '  # already correct

# === findViewById ===
Add-Fix 'IindViewById' 'findViewById'

# === inflater / inflate ===
Add-Fix 'inIlater' 'inflater'
Add-Fix 'LayoutinIlater' 'LayoutInflater'
Add-Fix 'inIlate' 'inflate'

# === setOnClickListener ===
Add-Fix 'setOnClicIListener' 'setOnClickListener'
Add-Fix 'setOnClicI' 'setOnClick'

# === performClick ===
Add-Fix 'perIormClicI' 'performClick'
Add-Fix 'perIorm' 'perform'

# === Fragment / fragment (context-dependent!) ===
# R.layout.fragment_xxx
Add-Fix 'R.layout.Iragment_' 'R.layout.fragment_'
# R.id.fragment_xxx
Add-Fix 'R.id.Iragment_' 'R.id.fragment_'
# import ...fragment
Add-Fix '.Iragment.' '.fragment.'
# word 'fragment' at start/after space
Add-Fix ' Iragment ' ' fragment '
Add-Fix ' Iragment_' ' fragment_'
# Class name Fragment stays capitalized, but import fragment stays lowercase
# The package/folder part
Add-Fix '.Iragments' '.fragments'

# === View methods ===
Add-Fix 'IindViewById' 'findViewById'
Add-Fix 'setOnClicIListener' 'setOnClickListener'

# === String / List / Pair types ===
# These are Kotlin types, not element symbols
Add-Fix 'StrIng' 'String'
Add-Fix 'strIng' 'string'
Add-Fix 'LIst' 'List'
Add-Fix 'lIst' 'list'
Add-Fix 'PaIr' 'Pair'
Add-Fix 'paIr' 'pair'
# NOTE: Element symbols like "LI"(Li), "TI"(Ti), "NI"(Ni), "SI"(Si) in periodic table data
# are display strings and should NOT be changed - they work fine with .lowercase() lookup

# === Turkish chemistry terms (i/I corruption) ===
# Common pattern: Turkish words where 'i' became 'I'
Add-Fix 'bIle' 'bile'
Add-Fix 'kImya' 'kimya'
Add-Fix 'Iimya' 'kimya'
Add-Fix 'Iategoriler' 'kategoriler'
Add-Fix 'Iategori' 'kategori'
Add-Fix 'IategorI' 'kategori'
Add-Fix 'IaynaI' 'kaynak'
Add-Fix 'Iayna' 'kayna'
Add-Fix 'hedeI' 'hedef'
Add-Fix 'Iirimler' 'birimler'
Add-Fix 'Iirim' 'birim'
Add-Fix 'IilesI' 'bilesi'
Add-Fix 'bIlesI' 'bilesi'
Add-Fix 'bIlesIk' 'bilesik'
Add-Fix 'bIlesen' 'bilesen'
Add-Fix 'IIlesen' 'bilesen'
Add-Fix 'IIlesenler' 'bilesenler'
Add-Fix 'IilesiI' 'bilesik'
Add-Fix 'Iormul' 'formul'
Add-Fix 'Iormulu' 'formulu'
Add-Fix 'Iormul' 'formul'
Add-Fix 'ozellII' 'ozellik'
Add-Fix 'ozellI' 'ozell'
Add-Fix 'kulIan' 'kullan'
Add-Fix 'kullanIl' 'kullanil'
Add-Fix 'IullanIl' 'kullanil'
Add-Fix 'Iullan' 'kullan'
Add-Fix 'Iullanim' 'kullanim'
Add-Fix 'IullanIm' 'kullanim'
Add-Fix 'Iullani' 'kullani'
Add-Fix 'Iullanan' 'kullanan'
Add-Fix 'eleitron' 'elektron'
Add-Fix 'eleitrI' 'elektri'
Add-Fix 'eleitronegatiI' 'elektronegatif'
Add-Fix 'IyOn' 'IyOn'  # already correct? or Iyon...
Add-Fix 'IyOn' 'iyon'  # hmm, in Turkish "iyon" is lowercase
# Actually, in Kotlin variables, it could be camelCase
Add-Fix 'iyonlasma' 'iyonlasma'  # already correct?

# === Element properties ===
Add-Fix 'Iutle' 'kutle'
Add-Fix 'yogunluI' 'yogunluk'
Add-Fix 'sicaIliI' 'sicaklik'
Add-Fix 'SicaIliI' 'Sicaklik'
Add-Fix 'UzunluI' 'Uzunluk'
Add-Fix 'YogunluI' 'Yogunluk'
Add-Fix 'Basinc' 'Basinc'  # already correct
Add-Fix 'Konsantrasyon' 'Konsantrasyon'  # already correct

# === Turkish chem in map keys ===
Add-Fix '"UzunluI"' '"Uzunluk"'
Add-Fix '"SicaIliI"' '"Sicaklik"'
Add-Fix '"YogunluI"' '"Yogunluk"'

# Unit symbols
Add-Fix '"Iar"' '"bar"'
Add-Fix '"IPa"' '"kPa"'
Add-Fix '"IWh"' '"kWh"'
Add-Fix '"IJ"' '"kJ"'
Add-Fix '"Ical"' '"kcal"'
Add-Fix '"Im\u00B2"' '"km\u00B2"'
Add-Fix '"It\u00B2"' '"ft\u00B2"'
Add-Fix '"Ig/m\u00B3"' '"kg/m\u00B3"'
Add-Fix '"lI/It\u00B3"' '"lb/ft\u00B3"'

# === Collection methods ===
Add-Fix '.Ieys' '.keys'
Add-Fix 'Iey' 'key'
Add-Fix '.Iind' '.find'
Add-Fix 'assocIateBy' 'associateBy'
Add-Fix 'sumOI' 'sumOf'
Add-Fix 'IindAll' 'findAll'
Add-Fix 'Iind' 'find'
Add-Fix '.Iirst()' '.first()'
Add-Fix '.Iirst ' '.first '
Add-Fix 'Iirst' 'first'
Add-Fix '.Iilter' '.filter'
Add-Fix '.IlatMap' '.flatMap'
Add-Fix '.grupIy' '.grupBy'
Add-Fix '.mapOI' '.mapOf'
Add-Fix '.listOI' '.listOf'

# === Layout IDs - BirimFragment ===
# These need to match the XML layout
# First, fix the corrupted characters in R.id references
# Then we'll update the Kotlin to use the ACTUAL XML IDs
# For now, just fix character corruption
Add-Fix 'R.id.Iirim_Iategori'  'R.id.birim_kategori'
Add-Fix 'R.id.Iirim_IaynaI'    'R.id.birim_kaynak'
Add-Fix 'R.id.Iirim_hedeI'     'R.id.birim_hedef'
Add-Fix 'R.id.Iirim_deger'     'R.id.birim_deger'
Add-Fix 'R.id.Iirim_donustur'  'R.id.birim_donustur'
Add-Fix 'R.id.Iirim_sonuc'     'R.id.birim_sonuc'
Add-Fix 'R.id.Iirim_preset'    'R.id.birim_preset'

# === AsitBazFragment ===
Add-Fix 'Iragment_asitIaz' 'fragment_asitbaz'

# === BilesikFragment ===
Add-Fix 'Iragment_IilesiI' 'fragment_bilesik'

# === EnstrumantalFragment ===
# Already handled by general rules

# === ReaksiyonFragment ===
Add-Fix 'Iragment_reaIsiyon' 'fragment_reaksiyon'

# === StoykiyometriFragment ===
Add-Fix 'Iragment_stoyIiyometri' 'fragment_stoykiyometri'

# === MolKutlesiFragment ===
Add-Fix 'Iragment_molIutlesi' 'fragment_molkutlesi'

# === Others ===
Add-Fix 'Iragment_dashIoard' 'fragment_dashboard'

# === getView / getDropDownView ===
Add-Fix 'getDropDownView' 'getDropDownView'  # already correct? Let me check
# Actually in the file it's 'getDropDownView' which is already correct

# === backgroundColor ===
Add-Fix 'setBacIground' 'setBackground'

# === format ===
Add-Fix 'Iormat' 'format'
Add-Fix '.Iormat(' '.format('
Add-Fix 'String.Iormat' 'String.format'

# === toast / toInt / toDouble ===
Add-Fix '.toint()' '.toInt()'
Add-Fix '.toDouIle' '.toDouble'
Add-Fix '.toLong()' '.toLong()'  # already correct?
Add-Fix 'toDouIleOrNull' 'toDoubleOrNull'
Add-Fix 'tointOrNull' 'toIntOrNull'

# === color / kotlin.math ===
Add-Fix 'Iotlin.math' 'kotlin.math'
Add-Fix 'kotlin.math.aIs' 'kotlin.math.abs'
Add-Fix '.aIs(' '.abs('
Add-Fix '.aIs ' '.abs '

# === textSize ===
Add-Fix 'textSize = 14I' 'textSize = 14f'

# === double.isNaN / isInfinite ===
Add-Fix 'isNaN()' 'isNaN()'  # correct
Add-Fix 'isinIinite' 'isInfinite'

# === onItemSelectedListener ===
Add-Fix 'OnitemSelectedListener' 'OnItemSelectedListener'
Add-Fix 'onitemSelectedListener' 'onItemSelectedListener'
Add-Fix 'onitemSelected' 'onItemSelected'
Add-Fix 'selecteditemPosition' 'selectedItemPosition'
Add-Fix 'selecteditem' 'selectedItem'

# === Math constants ===
Add-Fix 'DouIle.NaN' 'Double.NaN'

# === number formatting ===
Add-Fix '"%.4I"' '"%.4f"'

# === class/object ===
Add-Fix 'oIject KImyaData' 'object KimyaData'

# === compound/periodic table ===
# The periodic table string values like "LI" (Lithium) -> should stay as "Li"
# And "TI" (Titanium) -> should stay as "Ti"
# These are CHEMICAL SYMBOLS and must NOT be changed!
# So we need to be careful with replacements

# === Miscellaneous ===
Add-Fix 'konIig' 'config'
Add-Fix 'IonIig' 'config'
Add-Fix 'conIig' 'config'
Add-Fix 'IalI' 'kalk'
Add-Fix 'AlIalI' 'Alkali'
Add-Fix 'NIt' 'KIt' ?  # no, this is too vague

# === Layout reference fixes for BirimFragment ===
# The XML has: input_value, spinner_from, spinner_to, btn_donustur, result_birim
# So after fixing character corruption, we need to map OLD names to NEW XML names
# This should be done as a second pass after character fixes

# Sort pairs by length descending to avoid partial matches
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
