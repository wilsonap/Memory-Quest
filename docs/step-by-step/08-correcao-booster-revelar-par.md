# Correção — Booster Revelar Par indisponível após compra

**Data:** 16/08/2026  
**Escopo:** somente fluxo do booster `booster_reveal`

---

## Causa exata

Em `MainViewModel.buyBooster()`, a compra de `booster_reveal` **debitava moedas**, mas o `when` só tratava:

- `booster_life` → `addExtraLives(3)`
- `booster_hint` → `addHints(3)`
- **else → `{}` (não fazia nada)**

O ID da loja (`booster_reveal`) e o da `GameScreen`/`GameViewModel` já eram iguais.  
A `GameScreen` lê `freeRevealsCount` do inventário Room via `inventoryFlow`. Como a compra nunca inseria/incrementava `inventory.itemId = "booster_reveal"`, a quantidade permanecia `0`.

### Bug secundário

`GameViewModel.startLevel()` recriava `GameState(...)` sem `freeRevealsCount`, zerando o valor mesmo que o inventário tivesse unidades (o Flow do Room não reemite sem mudança).

---

## Correção

1. `GameRepository.addInventoryBooster` / `getInventoryQuantity`
2. `buyBooster("booster_reveal")` → `addInventoryBooster("booster_reveal", 1)`
3. `startLevel` carrega `freeRevealsCount` (e demais free*) do inventário
4. `useRevealPair` seleciona/revela o par antes de consumir inventário ou moedas

---

## Arquivos alterados

- `MainViewModel.kt`
- `GameRepository.kt`
- `GameViewModel.kt`
