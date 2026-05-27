$c = Get-Content "D:\work\AI\output\platform\prototype\pc\phase2\workflow-task.html" -Raw
$c = $c -replace [char]96 + '\)\.join\(\'\'''\)\;', ').join(\'\'\'\');'
Set-Content "D:\work\AI\output\platform\prototype\pc\phase2\workflow-task.html" -Value $c -NoNewline -Encoding UTF8
Write-Host "Done"