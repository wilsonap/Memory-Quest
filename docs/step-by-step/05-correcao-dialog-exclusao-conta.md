# 05 — Correção do AlertDialog de exclusão de conta (Configurações)

## Problema
O botão **"Excluir conta e todos os dados"** executava a exclusão sem confirmação adequada e o feedback pós-ação usava Toast em vez de Snackbar.

## Solução
Restauração e reforço do fluxo de confirmação na `SettingsScreen`, sem alterar `DeleteAccountRepository` nem `MainViewModel.deleteAccount()`.

## Arquivos alterados

| Arquivo | Função |
|---------|--------|
| `app/src/main/java/com/example/ui/screens/settings/SettingsScreen.kt` | AlertDialog, loading, Snackbar, tratamento sucesso/erro |
| `app/src/main/java/com/example/ui/navigation/NavGraph.kt` | Navegação para Home após Snackbar de sucesso |
| `app/src/main/res/values/strings.xml` | Textos do diálogo e mensagem de sucesso |

## Fluxo restaurado

1. Toque em **Excluir conta e todos os dados** → abre `AlertDialog` (`showDeleteAccountDialog = true`).
2. **Cancelar** → fecha o diálogo, nenhuma ação.
3. **Excluir definitivamente** (vermelho) → único botão que chama `onDeleteAccount` / `deleteAccount()`.
4. Durante exclusão → botões desabilitados + `CircularProgressIndicator`.
5. Sucesso → Snackbar `"Conta excluída com sucesso."` → navegação para Home → gates de Termos e nome de usuário reativados pelo `NavGraph`.
6. Erro → permanece na tela, diálogo aberto, Snackbar com mensagem do resultado.

## Onde o AlertDialog foi restaurado

`SettingsScreen.kt`, bloco `if (showDeleteAccountDialog) { AlertDialog(...) }` (antes do `Scaffold` principal).
