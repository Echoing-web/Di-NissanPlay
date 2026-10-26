$queries = @(
  'repo:shihabal3amri/DiPlay Nissan OR Teana OR Altima',
  'repo:shihabal3amri/DiPlay HSAE OR hangsheng OR anwsdk',
  'repo:shihabal3amri/DiPlay "i.MX6" OR imx6 OR i.mx8',
  'repo:shihabal3amri/DiPlay Sylphy OR X-Trail OR Qashqai OR Sentra'
)
foreach ($qq in $queries) {
  $q = [uri]::EscapeDataString($qq)
  $r = Invoke-RestMethod "https://api.github.com/search/issues?q=$q&per_page=20"
  Write-Output "== $qq =="
  $r.items | ForEach-Object { "#{0} [{1}] {2} ({3})" -f $_.number, $_.state, $_.title, $_.created_at }
}
