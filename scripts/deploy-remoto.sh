#!/usr/bin/env bash
set -euo pipefail
umask 077

# Executado por SSH, com argumentos previamente validados, sem carregar segredos no log.
app_dir=${1:?Informe diretório absoluto}
image_prefix=${2:?Informe prefixo do registry}
version=${3:?Informe SHA completo}
[[ "$app_dir" =~ ^/[a-zA-Z0-9_./-]+$ && "$app_dir" != / && "$app_dir" != *..* ]] || exit 2
[[ "$image_prefix" =~ ^ghcr\.io/[a-z0-9_.-]+/[a-z0-9_.-]+$ ]] || exit 2
[[ "$version" =~ ^[a-f0-9]{40}$ ]] || exit 2
cd -- "$app_dir"
[[ -f .env.producao ]] || { echo 'Configure .env.producao no servidor.' >&2; exit 2; }
exec 9>.deploy.lock
flock -n 9 || { echo 'Outro deploy está em andamento.' >&2; exit 1; }
export IMAGE_PREFIX="$image_prefix" APP_VERSION="$version"
release_dir="$app_dir/releases/$version"
compose=(docker compose --project-name compara-preco-producao --env-file "$app_dir/.env.producao" -f "$release_dir/compose.producao.yml" -f "$release_dir/compose.registry.yml")
"${compose[@]}" config --quiet
"${compose[@]}" pull api web
for service in api web; do
  revision=$(docker image inspect "$image_prefix-$service:$version" --format '{{ index .Config.Labels "org.opencontainers.image.revision" }}')
  [[ "$revision" == "$version" ]] || { echo 'Imagem n?o corresponde ao commit solicitado.' >&2; exit 1; }
done
"${compose[@]}" up -d --wait --wait-timeout 120 postgres
container=$("${compose[@]}" ps -q postgres)
[[ -n "$container" ]] || exit 1
mkdir -p backups
chmod 700 backups
stamp=$(date -u +%Y%m%dT%H%M%SZ)
backup="$app_dir/backups/$stamp-$version.dump"
[[ ! -e "$backup" ]] || { echo 'Backup já existe; nada será sobrescrito.' >&2; exit 1; }
temporary="/tmp/compara-deploy-$stamp.dump"
trap 'docker exec "$container" rm -f -- "$temporary" >/dev/null 2>&1 || true' EXIT
docker exec "$container" sh -c 'umask 077; exec pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc -f "$1"' sh "$temporary"
docker cp "$container:$temporary" "$backup"
chmod 600 "$backup"
[[ -s "$backup" ]] || { echo 'Backup vazio. Deploy interrompido.' >&2; exit 1; }
"${compose[@]}" up -d --no-build --pull never --wait --wait-timeout 180 api web
"${compose[@]}" restart web
address=$("${compose[@]}" port web 8080)
[[ "$address" =~ ^127\.0\.0\.1:[0-9]+$ ]] || exit 1
for path in /healthz /api/auth/csrf; do
  curl --fail --silent --show-error --retry 12 --retry-delay 3 --retry-connrefused "http://$address$path" -o /dev/null
done
printf '%s\n' "$version" > current-release
printf 'Deploy saudável: %s. Backup: %s\n' "$version" "$backup"
