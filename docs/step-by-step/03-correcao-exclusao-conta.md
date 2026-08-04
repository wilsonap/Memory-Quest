# Correção — Exclusão de Conta (Delete Account)

**Data:** 04/08/2025  
**Status:** Concluído

---

## Problema

Contas recém-criadas falhavam ao excluir com mensagem genérica:
*"Não foi possível excluir todos os seus dados. Nenhuma alteração foi concluída."*

## Causa

O fluxo **não existia implementado** no repositório. A tela de Configurações apenas reiniciava progresso local (`resetGameProgress`), sem:

- batch Firestore para apagar documentos remotos;
- `FirebaseAuth.currentUser.delete()`;
- limpeza de consentimento/username;
- tratamento de documentos ausentes.

Contas novas (só Auth + consentimento, sem username/leaderboard) exigiam deletes idempotentes — qualquer lógica que exige `exists() == true` falharia.

## Solução

Novo `DeleteAccountRepository` com ordem:

1. Validar internet e `currentUser`
2. Resolver `normalizedName` local (vazio → pular `usernames/`)
3. `WriteBatch.delete()` em: `usernames`, `leaderboard`, `username_settings`, `user_consents`
4. `batch.commit()` — documentos inexistentes não são erro
5. `currentUser.delete()` — antes da limpeza local (preserva dados se auth falhar)
6. Limpar Room, DataStore (consentimento, cache username), avatar, pending_sync
7. `ensureInitialized()` — jogador novo com username vazio
8. **Não** chama `signInAnonymously()` — nova conta só ao aceitar termos

Logs com tag `MemoryQuestDeleteAccount` em cada etapa.

## Firestore Rules (verificado)

| Coleção | Delete permitido |
|---------|------------------|
| `leaderboard/{uid}` | `auth.uid == userId` ✅ |
| `username_settings/{uid}` | `auth.uid == userId && resource.data.uid == auth.uid` ✅ |
| `user_consents/{uid}` | `auth.uid == userId` (via write) ✅ |
| `usernames/{name}` | `resource.data.uid == auth.uid` ✅ |

`leaderboard` não exige `resource.data.uid` (regra atual mais permissiva que a sugerida).

## Arquivos alterados

- `DeleteAccountRepository.kt` (novo)
- `DeleteAccountResult.kt` (novo)
- `GameRepository.kt` — `wipeAllLocalDataForAccountDeletion()`
- `MemoryQuestDao.kt` — queries DELETE
- `DataStoreManager.kt` — `clearAccountData()`
- `MainViewModel.kt` — `deleteAccount()`, limpeza de estado
- `SettingsScreen.kt` — diálogo de exclusão
- `NavGraph.kt` — callback + navegação
- `values/strings.xml` — mensagens de erro específicas
- `DeleteAccountRepositoryTest.kt` (novo)

## Testes

`DeleteAccountRepositoryTest` — 4 testes de resolução de `normalizedName`.

Cenários manuais recomendados: A–K conforme especificação do usuário.
