param(
    [Parameter(Mandatory = $true)][string]$Src,
    [Parameter(Mandatory = $true)][string]$Docx,
    [Parameter(Mandatory = $true)][string]$Pdf
)
# Apre la bozza generata con Word, aggiorna sommario e campi,
# salva il .docx definitivo ed esporta il PDF con i segnalibri dei capitoli.
$ErrorActionPreference = 'Stop'
$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0
try {
    $doc = $word.Documents.Open($Src, $false, $false, $false)
    $doc.SetCompatibilityMode(15)
    $doc.Repaginate()
    foreach ($toc in $doc.TablesOfContents) { $toc.Update() }
    $doc.Fields.Update() | Out-Null
    foreach ($sec in $doc.Sections) {
        foreach ($h in $sec.Headers) { $h.Range.Fields.Update() | Out-Null }
        foreach ($f in $sec.Footers) { $f.Range.Fields.Update() | Out-Null }
    }
    $doc.Repaginate()
    foreach ($toc in $doc.TablesOfContents) { $toc.UpdatePageNumbers() }
    # Segoe UI e Consolas sono presenti in ogni Windows: incorporarli porterebbe il file a oltre 3 MB.
    $doc.EmbedTrueTypeFonts = $false
    $doc.SaveAs2($Docx, 16)
    # 17 = PDF; 1 = segnalibri dai titoli; tag di struttura per l'accessibilità.
    $doc.ExportAsFixedFormat($Pdf, 17, $false, 0, 0, 1, 1, 0, $true, $true, 1, $true, $true, $false)
    "pagine: " + $doc.ComputeStatistics(2)
    $doc.Close($false)
}
finally {
    $word.Quit()
}
