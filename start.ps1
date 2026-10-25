# ============================================================
#  智能旅游助手 · 一键启动
#  检查 MySQL → 起后端(3200) → 起前端(5174) → 打开浏览器
#
#  双击 start.cmd 即可（本机 PowerShell 执行策略禁止 .ps1，
#  start.cmd 会用 -ExecutionPolicy Bypass 调用本脚本）
# ============================================================

$ErrorActionPreference = 'Continue'

$root     = Split-Path -Parent $MyInvocation.MyCommand.Path
$backend  = Join-Path $root 'travel-project1'
$frontend = Join-Path $root 'travl-1'
$mvn      = 'D:\software\IntelliJ IDEA 2026.2.0.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd'
$npm      = 'D:\software\nodejs\npm.cmd'

# 后端需要 JDK 17（pom 里 java.version=17）；子进程会继承这个环境变量
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'

Write-Host ''
Write-Host '============================================' -ForegroundColor Cyan
Write-Host '   智能旅游助手 · 一键启动' -ForegroundColor Cyan
Write-Host '============================================' -ForegroundColor Cyan

function Test-PortInUse([int]$port) {
    $conn = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    return $null -ne $conn
}

# ---------- 1. MySQL ----------
$svc = Get-Service -Name MySQL -ErrorAction SilentlyContinue
if ($svc -and $svc.Status -eq 'Running') {
    Write-Host '[1/4] MySQL 服务正常' -ForegroundColor Green
} else {
    Write-Host '[1/4] 警告：MySQL 服务没在运行，请先启动 MySQL 服务' -ForegroundColor Yellow
}

# ---------- 2. 后端 ----------
if (Test-PortInUse 3200) {
    Write-Host '[2/4] 后端已在 3200 运行，跳过启动' -ForegroundColor Green
} else {
    Write-Host '[2/4] 启动后端（新窗口，首次会下载依赖，请耐心等）...'
    Start-Process -FilePath $mvn -ArgumentList '-B', 'spring-boot:run' -WorkingDirectory $backend -WindowStyle Normal
}

# ---------- 3. 前端 ----------
if (Test-PortInUse 5174) {
    Write-Host '[3/4] 前端已在 5174 运行，跳过启动' -ForegroundColor Green
} else {
    Write-Host '[3/4] 启动前端（新窗口）...'
    Start-Process -FilePath $npm -ArgumentList 'run', 'dev' -WorkingDirectory $frontend -WindowStyle Normal
}

# ---------- 4. 等后端就绪 ----------
Write-Host '[4/4] 等待后端就绪（最多 120 秒）...'
$ready = $false
for ($i = 0; $i -lt 60; $i++) {
    try {
        $resp = Invoke-WebRequest -Uri 'http://127.0.0.1:3200/api/travel/hello' -UseBasicParsing -TimeoutSec 2
        if ($resp.StatusCode -eq 200) { $ready = $true; break }
    } catch { }
    Start-Sleep -Seconds 2
}

if ($ready) {
    Write-Host '      后端已就绪' -ForegroundColor Green
} else {
    Write-Host '      后端还没起来，请看后端窗口的日志' -ForegroundColor Yellow
}

if ($env:NO_BROWSER -ne '1') {
    Start-Process 'http://localhost:5174'
    Write-Host ''
    Write-Host '已打开 http://localhost:5174' -ForegroundColor Cyan
} else {
    Write-Host ''
    Write-Host '[NO_BROWSER=1] 跳过打开浏览器，请手动访问 http://localhost:5174'
}

Write-Host ''
Write-Host '提示：后端/前端各占一个窗口，关闭窗口即停止；MySQL 是系统服务，不受影响。'
Write-Host '注意：AI 规划需要 SiliconFlow 账户有余额，余额不足时页面会明确提示。'
Write-Host ''
