#!/usr/bin/env bash
set -euo pipefail

RAIZ_PROJETO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

declarar_testes() {
    case "$1" in
        "backend/servico-autenticacao")
            echo "br.com.cloudport.servicoautenticacao.migracao.VerificacaoMigracoesTest"
            ;;
        "backend/servico-gate")
            echo "br.com.cloudport.servicogate.migracao.VerificacaoMigracoesTest"
            ;;
        "backend/servico-navio")
            echo "br.com.cloudport.serviconavio.migracao.VerificacaoMigracoesTest"
            ;;
        "backend/servico-rail")
            echo "br.com.cloudport.servicorail.migracao.VerificacaoMigracoesTest"
            ;;
        "backend/servico-yard")
            echo "br.com.cloudport.servicoyard.migracao.VerificacaoMigracoesTest"
            ;;
        *)
            echo ""
            ;;
    esac
}

SERVICOS=(
    "backend/servico-autenticacao"
    "backend/servico-gate"
    "backend/servico-navio"
    "backend/servico-rail"
    "backend/servico-yard"
)

for SERVICO in "${SERVICOS[@]}"; do
    TESTE_CLASSE="$(declarar_testes "$SERVICO")"
    if [[ -z "$TESTE_CLASSE" ]]; then
        continue
    fi

    if [[ -f "${RAIZ_PROJETO}/${SERVICO}/pom.xml" ]]; then
        echo "Verificando migrações em ${SERVICO}..."
        (cd "${RAIZ_PROJETO}/${SERVICO}" && ./mvnw -q -DskipTests=false -Dtest="${TESTE_CLASSE}" test)
    fi
    echo
done

echo "Verificação das migrações concluída."
