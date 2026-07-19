#!/bin/sh
set -eu

diretorios_temporarios="$(mktemp)"
arquivos_temporarios="$(mktemp)"
trap 'rm -f "$diretorios_temporarios" "$arquivos_temporarios"' EXIT HUP INT TERM

find . -type d -path '*/src/main/resources/db/migration' -print | sort > "$diretorios_temporarios"

quantidade_diretorios=0
falhou=0

while IFS= read -r diretorio; do
    [ -n "$diretorio" ] || continue
    quantidade_diretorios=$((quantidade_diretorios + 1))

    : > "$arquivos_temporarios"
    find "$diretorio" -maxdepth 1 -type f -name 'V*__*.sql' \
        -exec basename {} \; | sort > "$arquivos_temporarios"

    if ! awk -v diretorio="$diretorio" '
        {
            nome = $0
            versao = nome
            sub(/^V/, "", versao)
            sub(/__.*/, "", versao)
            gsub(/_/, ".", versao)

            quantidade[versao]++
            arquivos[versao] = arquivos[versao] (arquivos[versao] ? ", " : "") nome
        }
        END {
            duplicada = 0
            for (versao in quantidade) {
                if (quantidade[versao] > 1) {
                    printf "ERRO: versao Flyway %s duplicada em %s: %s\n", \
                        versao, diretorio, arquivos[versao] > "/dev/stderr"
                    duplicada = 1
                }
            }
            exit duplicada
        }
    ' "$arquivos_temporarios"; then
        falhou=1
    fi
done < "$diretorios_temporarios"

if [ "$quantidade_diretorios" -eq 0 ]; then
    echo "ERRO: nenhum diretorio de migrations Flyway foi encontrado." >&2
    exit 1
fi

if [ "$falhou" -ne 0 ]; then
    exit 1
fi

printf 'Flyway: versoes unicas validadas em %s diretorios de migrations.\n' "$quantidade_diretorios"
