<#
 .SYNOPSIS
  Quick test script to verify Anthropic Claude API connectivity from PowerShell.

 .DESCRIPTION
  Sends a minimal message to the Claude 3.5 Sonnet latest model and prints the
  returned assistant text content plus raw JSON (optional). Useful to diagnose
  API key validity, networking or model naming issues.

 .PARAMETER ApiKey
  Anthropic API key. If omitted, the script tries $Env:ANTHROPIC_API_KEY.

 .PARAMETER Model
  Model name (default: claude-3-5-sonnet-latest).

 .PARAMETER Prompt
  User prompt to send (default: "Segðu halló").

 .PARAMETER ShowRaw
  Switch to also output the full JSON response.

 .EXAMPLE
  ./test-anthropic.ps1 -ApiKey "sk-ant-api03-..." -Prompt "Hvað segirðu?"

 .EXAMPLE
  $Env:ANTHROPIC_API_KEY = "sk-ant-api03-..."
  ./test-anthropic.ps1 -ShowRaw

#>
param(
    [string]$ApiKey,
    [string]$Model = 'claude-3-5-sonnet-latest',
    [string]$Prompt = 'Segðu halló',
    [switch]$ShowRaw,
    [switch]$TestModels
)

if (-not $ApiKey) {
    $ApiKey = $Env:ANTHROPIC_API_KEY
}

if (-not $ApiKey) { Write-Error 'No API key provided. Use -ApiKey or set $Env:ANTHROPIC_API_KEY.'; exit 1 }

if ($TestModels) {
    $candidates = @(
        'claude-3-5-sonnet-latest',
        'claude-3-5-sonnet-20241022',
        'claude-3-5-haiku-latest',
        'claude-3-5-haiku-20241022',
        'claude-3-opus-latest',
        'claude-3-opus-20240229',
        'claude-3-sonnet-20240229',
        'claude-instant-1.2'
    )
    Write-Host "Testing candidate models..." -ForegroundColor Cyan
    $ok = @()
    foreach ($m in $candidates) {
        $payload = @{ model = $m; max_tokens = 10; messages = @(@{ role='user'; content='ping' }) }
        $body = ($payload | ConvertTo-Json -Depth 5)
        try {
            $r = Invoke-RestMethod -Uri 'https://api.anthropic.com/v1/messages' -Headers @{ 'x-api-key'=$ApiKey; 'anthropic-version'='2023-06-01' } -ContentType 'application/json' -Method Post -Body $body -ErrorAction Stop
            if ($r.content) { Write-Host "OK  $m" -ForegroundColor Green; $ok += $m } else { Write-Host "NO  $m (no content)" -ForegroundColor Yellow }
        } catch {
            if ($_.Exception.Response -and $_.Exception.Response.StatusCode.value__) {
                Write-Host ("ERR $m : {0}" -f $_.Exception.Response.StatusCode.value__) -ForegroundColor Red
            } else {
                Write-Host ("ERR $m : {0}" -f $_.Exception.Message) -ForegroundColor Red
            }
        }
        Start-Sleep -Milliseconds 300
    }
    $joined = ($ok -join ', ')
    Write-Host "Working models: $joined" -ForegroundColor Green
    if (-not $ok) { Write-Warning 'No candidate models worked. Key may lack model access.' }
    return
}

# Build body using native hashtable + ConvertTo-Json for Windows PowerShell 5.1 compatibility
$payload = @{ 
    model      = $Model
    max_tokens = 128
    messages   = @(@{ role = 'user'; content = $Prompt })
}
$body = ($payload | ConvertTo-Json -Depth 5)

try {
    $headers = @{ 'x-api-key' = $ApiKey; 'anthropic-version' = '2023-06-01' }
    $response = Invoke-RestMethod -Uri 'https://api.anthropic.com/v1/messages' `
        -Headers $headers -ContentType 'application/json' -Method Post -Body $body -ErrorAction Stop
} catch {
    Write-Host "Request failed:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Yellow
    if ($_.ErrorDetails) { Write-Host $_.ErrorDetails.Message }
    # If model not found, suggest sweep automatically
    if ($_.ErrorDetails -and $_.ErrorDetails.Message -match 'not_found_error' -and -not $TestModels) {
        Write-Host "Attempting model sweep because requested model was not found..." -ForegroundColor Cyan
        & $PSCommandPath -ApiKey $ApiKey -TestModels
        exit 3
    }
    exit 2
}

if ($ShowRaw) {
    Write-Host '--- RAW JSON ---' -ForegroundColor Cyan
    $response | ConvertTo-Json -Depth 10
    Write-Host '----------------'
}

# Extract first text block
$text = $null
foreach ($c in $response.content) {
    if ($c.type -eq 'text') { $text = $c.text; break }
}

if ($text) {
    Write-Host "Assistant:" -ForegroundColor Green
    Write-Host $text
} else {
    Write-Warning 'No text content returned.'
}

if ($response.usage) {
    $inTok = $response.usage.input_tokens
    $outTok = $response.usage.output_tokens
    Write-Host "Model: $($response.model) | InputTokens=$inTok OutputTokens=$outTok"
} else {
    Write-Host "Model: $($response.model)"
}

exit 0
