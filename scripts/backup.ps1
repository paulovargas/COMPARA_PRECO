param(
    [Parameter(Mandatory=$true)][string]$Destino,
    [string]$Projeto = 'compara-preco-producao',
    [ValidatePattern('^[a-z][a-z0-9_]{0,40}$')][string]$Banco = 'compara_preco'
)
$ErrorActionPreference = 'Stop'
$arquivoDestino = [IO.Path]::GetFullPath($Destino)
if (Test-Path -LiteralPath $arquivoDestino) { throw 'O destino já existe. Escolha outro nome para preservar o backup anterior.' }
$containers = @(docker ps -q --filter "label=com.docker.compose.project=$Projeto" --filter 'label=com.docker.compose.service=postgres')
if ($LASTEXITCODE -ne 0 -or $containers.Count -ne 1) { throw 'Inicie o PostgreSQL do projeto informado antes de gerar backup.' }
$containerBanco = $containers[0]
$temporarioBanco = '/tmp/compara-backup-' + [Guid]::NewGuid().ToString('N') + '.dump'
try {
    docker exec $containerBanco pg_dump -U compara_preco -d $Banco -Fc -f $temporarioBanco
    if ($LASTEXITCODE -ne 0) { throw 'pg_dump falhou; nenhum backup válido foi produzido.' }
    $pastaDestino = [IO.Path]::GetDirectoryName($arquivoDestino)
    New-Item -ItemType Directory -Force -Path $pastaDestino | Out-Null
    docker cp "${containerBanco}:$temporarioBanco" $arquivoDestino
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao copiar backup. Verifique o destino antes de usar o arquivo.' }
    Write-Output "Backup criado: $arquivoDestino"
} finally {
    docker exec $containerBanco rm -f -- $temporarioBanco | Out-Null
}
