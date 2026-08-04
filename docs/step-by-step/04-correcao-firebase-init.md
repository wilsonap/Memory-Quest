# Correção — Crash de Inicialização Firebase

**Data:** 04/08/2025

## Causa exata

1. **`app/google-services.json` ausente** — plugin não gera `google_app_id`, `FirebaseApp.initializeApp()` retorna `null`.
2. **`FirebaseAuth.getInstance()` em parâmetros default** de construtores (`LeaderboardRepository:36`, etc.) executava antes do Firebase estar pronto.
3. **`ConnectivityObserver.onAvailable()`** na `ConnectivityThread` podia acionar `MainViewModel` antes da thread principal.

## Correções aplicadas

- `FirebaseBootstrap` — flag global `isReady` após init bem-sucedido
- Repositórios Firebase com `lazy` + `FirebaseBootstrap.requireReady()`
- `MainActivity` — ViewModels na main thread antes do observer; tela de erro se Firebase indisponível
- `ConnectivityObserver` — callback postado na main thread
- Removido `googleServices.missing.passthrough=true` e `MissingGoogleServicesStrategy.WARN`
- Adicionado `app/google-services.json.example`

## Ação obrigatória do desenvolvedor

Baixar `google-services.json` no Firebase Console para `br.com.autocheckia.memory` e salvar em `app/google-services.json`.
