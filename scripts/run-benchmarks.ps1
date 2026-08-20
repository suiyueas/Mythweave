# ============================================================
# Mythweave Benchmark 一键复现脚本（Windows PowerShell 5.1+）
# 用法: .\scripts\run-benchmarks.ps1 [-Experiment 1|2|3|4|all]
# 说明: 逐项检查依赖（ES/MySQL/Redis/API Key），运行实验并提示结果文件位置
# ============================================================
param(
    [ValidateSet("1", "2", "3", "4", "all")]
    [string]$Experiment = "all"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$sb = Join-Path $root "springboot"

function Test-Port($host, $port) {
    try {
        $tcp = New-Object System.Net.Sockets.TcpClient
        $tcp.Connect($host, $port)
        $tcp.Close()
        return $true
    } catch {
        return $false
    }
}

function Assert-Env($name, $hint) {
    $v = [Environment]::GetEnvironmentVariable($name)
    if ([string]::IsNullOrWhiteSpace($v)) {
        Write-Host "[SKIP] 未配置 $name（$hint），相关实验将跳过" -ForegroundColor Yellow
        return $false
    }
    return $true
}

function Check-Deps($needEs, $needQwen, $needDeepseek) {
    $ok = $true
    if ($needEs -and -not (Test-Port "localhost" 9200)) {
        Write-Host "[FAIL] Elasticsearch 未启动 (localhost:9200)，实验①②需要" -ForegroundColor Red
        $ok = $false
    } else {
        Write-Host "[ OK ] Elasticsearch 9200" -ForegroundColor Green
    }
    if (-not (Test-Port "localhost" 3306)) {
        Write-Host "[WARN] MySQL 未启动 (3306) - @SpringBootTest 需要，可能启动失败" -ForegroundColor Yellow
    } else {
        Write-Host "[ OK ] MySQL 3306" -ForegroundColor Green
    }
    if (-not (Test-Port "localhost" 6379)) {
        Write-Host "[WARN] Redis 未启动 (6379) - 应用上下文可能降级启动" -ForegroundColor Yellow
    } else {
        Write-Host "[ OK ] Redis 6379" -ForegroundColor Green
    }
    if ($needQwen -and -not (Assert-Env "QW_API_KEY" "千问 Embedding Key")) { $ok = $false }
    if ($needDeepseek -and -not (Assert-Env "DEEPSEEK_API_KEY" "DeepSeek Key")) { $ok = $false }
    return $ok
}

function Run-Mvn($args) {
    Write-Host "`n>>> mvn $args" -ForegroundColor Cyan
    & mvn @args
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[FAIL] mvn 执行失败 (exit=$LASTEXITCODE)" -ForegroundColor Red
        exit $LASTEXITCODE
    }
}

Write-Host "========== Mythweave Benchmark ==========" -ForegroundColor Cyan
Write-Host "工作目录: $sb`n"

switch ($Experiment) {
    "1" {
        if (Check-Deps $true $true $false) {
            Run-Mvn @("-f", $sb, "test", "-Dtest=HybridSearchBenchmark", "-DfailIfNoTests=false")
            Write-Host "结果: docs/benchmark-results/hybrid-search.md" -ForegroundColor Green
        }
    }
    "2" {
        if (Check-Deps $true $true $false) {
            Run-Mvn @("-f", $sb, "test", "-Dtest=HybridSearchBenchmark", "-DfailIfNoTests=false")
            Write-Host "结果: docs/benchmark-results/hybrid-search-latency.md" -ForegroundColor Green
        }
    }
    "3" {
        $mock = $false
        if (-not (Assert-Env "DEEPSEEK_API_KEY" "DeepSeek Key（无 Key 时用 Mock 模式）")) {
            $mock = $true
        }
        if (Check-Deps $false $false ($true -and -not $mock)) {
            if ($mock) {
                Run-Mvn @("-f", $sb, "test", "-Dtest=AgentParallelBenchmark", "-DfailIfNoTests=false", "-Dbenchmark.agent.mock=true")
                Write-Host "Mock 结果: docs/benchmark-results/agent-parallel.md（真实模式需配置 Key 后去掉 -Dbenchmark.agent.mock=true 重跑）" -ForegroundColor Green
            } else {
                Run-Mvn @("-f", $sb, "test", "-Dtest=AgentParallelBenchmark", "-DfailIfNoTests=false")
                Write-Host "真实结果: docs/benchmark-results/agent-parallel.md" -ForegroundColor Green
            }
        }
    }
    "4" {
        if (Check-Deps $false $false $true) {
            Write-Host "`nTTFT 观测说明：" -ForegroundColor Cyan
            Write-Host "  1. 配置 DEEPSEEK_API_KEY 后启动应用"
            Write-Host "  2. 触发一次 SSE 流式写作，采集日志: DeepSeek流式首字延迟(模型=xxx): Nms"
            Write-Host "  3. 连续 N 次采样后汇总 P50/P95，写入 docs/benchmark-results/ttft.md"
        }
    }
    default {
        Write-Host "一键全量（实验①②③ Mock 保底 + ④指引）`n" -ForegroundColor Cyan
        & $PSScriptRoot\run-benchmarks.ps1 -Experiment 1
        & $PSScriptRoot\run-benchmarks.ps1 -Experiment 2
        & $PSScriptRoot\run-benchmarks.ps1 -Experiment 3
        & $PSScriptRoot\run-benchmarks.ps1 -Experiment 4
    }
}

Write-Host "`n========== 完成 ==========" -ForegroundColor Cyan