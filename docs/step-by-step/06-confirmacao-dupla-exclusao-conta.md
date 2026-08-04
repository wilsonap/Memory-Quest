# 06 — Confirmação em duas etapas para exclusão de conta

## Objetivo
Aumentar a segurança da exclusão de conta exigindo confirmação textual além do AlertDialog informativo.

## Fluxo implementado

1. Toque em **Excluir conta e todos os dados** → 1º AlertDialog (informativo).
2. **Cancelar** → fecha o diálogo.
3. **Excluir definitivamente** → abre o 2º AlertDialog (confirmação final), **sem** chamar `deleteAccount()`.
4. No 2º diálogo, o usuário deve digitar exatamente `EXCLUIR` no `OutlinedTextField`.
5. Botão **Excluir conta** (vermelho) permanece desabilitado até a digitação correta.
6. Ao confirmar → fecha o diálogo → overlay de progresso → `deleteAccount()`.
7. Sucesso/erro mantém o fluxo anterior (Snackbar + navegação ou mensagem de erro).

## Arquivos alterados

| Arquivo | Função |
|---------|--------|
| `SettingsScreen.kt` | Dois diálogos, campo de confirmação, overlay de loading |
| `values/strings.xml` | Strings do 2º diálogo e palavra-chave `EXCLUIR` |

## O que não foi alterado

- `DeleteAccountRepository.kt`
- `MainViewModel.deleteAccount()`
- Lógica de limpeza Firestore / Auth / Room
