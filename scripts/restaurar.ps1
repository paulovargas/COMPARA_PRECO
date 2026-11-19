param(
    [Parameter(Mandatory=$true)][string]$Arquivo,
    [Parameter(Mandatory=$true)][ValidatePattern('^[a-z][a-z0-9_]{0,40}$')][string]$NovoBanco,
    [string]$Projeto = 'compara-preco-producao'
)
$ErrorActionPreference = 'Stop'
if ($NovoBanco -in @('postgres', 'template0', 'template1', 'compara_preco')) { throw 'Escolha um nome de banco novo para a restauração.' }
$arquivoOrigem = (Resolve-Path -LiteralPath $Arquivo).Path
if (-not (Test-Path -LiteralPath $arquivoOrigem -PathType Leaf)) { throw 'Informe um arquivo de backup.' }
$containers = @(docker ps -q --filter "label=com.docker.compose.project=$Projeto" --filter 'label=com.docker.compose.service=postgres')
if ($LASTEXITCODE -ne 0 -or $containers.Count -ne 1) { throw 'Inicie o PostgreSQL do projeto informado antes de restaurar.' }
$containerBanco = $containers[0]
$temporarioBanco = '/tmp/compara-restaurar-' + [Guid]::NewGuid().ToString('N') + '.dump'
try {
    docker exec $containerBanco createdb -U compara_preco -- $NovoBanco
    if ($LASTEXITCODE -ne 0) { throw 'Não foi possível criar o banco novo. Bancos existentes nunca são apagados ou sobrescritos.' }
    docker cp $arquivoOrigem "${containerBanco}:$temporarioBanco"
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao copiar o backup para restauração.' }
    docker exec $containerBanco pg_restore -U compara_preco -d $NovoBanco --no-owner --no-privileges --exit-on-error --single-transaction $temporarioBanco
    if ($LASTEXITCODE -ne 0) { throw 'Restauração falhou. O banco novo foi mantido para inspeção; não direcione a API para ele.' }
    Write-Output "Backup restaurado no banco novo: $NovoBanco"
} finally {
    docker exec $containerBanco rm -f -- $temporarioBanco | Out-Null
}
