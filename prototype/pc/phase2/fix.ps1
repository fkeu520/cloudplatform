$c = Get-Content "D:\work\AI\output\platform\prototype\pc\phase2\workflow-task.html" -Raw
$c = $c -replace '`\)\.join\(\x27\x27\);', ').join(\x27\x27);'
$c = $c -replace '</div>`', '</div>'
$c = $c -replace '</body>`', '</body>'
Set-Content "D:\work\AI\output\platform\prototype\pc\phase2\workflow-task.html" -Value $c -NoNewline -Encoding UTF8
Write-Host "Done"