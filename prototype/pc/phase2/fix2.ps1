$c = Get-Content "D:\work\AI\output\platform\prototype\pc\phase2\workflow-task.html" -Raw
$c = $c -replace '\x27\x27', "''"
Set-Content "D:\work\AI\output\platform\prototype\pc\phase2\workflow-task.html" -Value $c -NoNewline -Encoding UTF8
Write-Host "Done"