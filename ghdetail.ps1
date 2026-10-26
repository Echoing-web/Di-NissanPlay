$nums = @(880, 871, 1108, 1084, 1085, 1119, 1071)
foreach ($n in $nums) {
  $i = Invoke-RestMethod "https://api.github.com/repos/shihabal3amri/DiPlay/issues/$n"
  Write-Output "== #$n [$($i.state)] $($i.title) ($($i.created_at)) =="
  $body = $i.body
  if ($body) { if ($body.Length -gt 1500) { $body = $body.Substring(0,1500) + '...[truncated]' }; Write-Output $body }
  Write-Output ""
}
