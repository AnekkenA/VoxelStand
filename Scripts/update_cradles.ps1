$path = Join-Path $PSScriptRoot "..\src\main\resources\assets\voxelstand\models\block\sculpt_stand_weapon.json"
$content = Get-Content -Raw -Path $path -Encoding UTF8

$content = $content.Replace("5.5,`r`n                                      15.2,`r`n                                      7", "8.5,`r`n                                      15.2,`r`n                                      7")
$content = $content.Replace("5.5,`r`n                                      17,`r`n                                      6", "8.5,`r`n                                      17,`r`n                                      6")
$content = $content.Replace("5.5,`r`n                                      17,`r`n                                      9", "8.5,`r`n                                      17,`r`n                                      9")
$content = $content.Replace("8.5,`r`n                                    17,", "11.5,`r`n                                    17,")
$content = $content.Replace("8.5,`r`n                                    19.5,", "11.5,`r`n                                    19.5,")

$content = $content.Replace("23.5,`r`n                                      15.2,`r`n                                      7", "20.5,`r`n                                      15.2,`r`n                                      7")
$content = $content.Replace("23.5,`r`n                                      17,`r`n                                      6", "20.5,`r`n                                      17,`r`n                                      6")
$content = $content.Replace("23.5,`r`n                                      17,`r`n                                      9", "20.5,`r`n                                      17,`r`n                                      9")
$content = $content.Replace("26.5,`r`n                                    17,", "23.5,`r`n                                    17,")
$content = $content.Replace("26.5,`r`n                                    19.5,", "23.5,`r`n                                    19.5,")

Set-Content -Path $path -Value $content -Encoding UTF8
Write-Output "Cradles successfully repositioned."
