# Correção — Ganhar Moedas Grátis (loading + anti-duplo clique)

**Data:** 16/08/2026  
**Escopo:** fluxo Rewarded Ad da loja (`Ganhar Moedas Grátis`)

---

## Problema

Toques repetidos iniciavam vários `loadAd`/`show` sem feedback visual.

## Solução

1. `MainViewModel.isRewardedAdProcessing` + gate `AtomicBoolean` (bloqueia nova sessão)
2. `rewardedAdRewardGranted` + `rewardDelivered` no `RewardedManager` (recompensa idempotente)
3. `ShopScreen`: botão desabilitado + “Carregando anúncio...” + `CircularProgressIndicator`
4. Erro de load libera o botão e mostra mensagem amigável

## Arquivos

- `MainViewModel.kt`
- `RewardedManager.kt`
- `ShopScreen.kt`
- `NavGraph.kt`
