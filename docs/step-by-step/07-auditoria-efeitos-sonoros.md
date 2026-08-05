# Auditoria SFX — Memory Quest

**Data:** 05/08/2026  
**Problema:** APK local (Android Studio) sem efeitos sonoros; APK AI Studio com SFX OK.

---

## 1. Evidência no Logcat (dispositivo Mi 9T, Android 10 / API 29)

```
loadSoundEffects: effect=BUTTON_CLICK ... -> soundId=6
...
onLoadComplete FAILED: soundId=1, status=-10000
onLoadComplete FAILED: soundId=2, status=-10000
... (todos os 10 samples)
playSfx REJECTED: soundId=6 ... ainda nao concluiu onLoadComplete!
```

- `soundEffectsEnabled=true`, `sfxVolume=0.8` (preferências OK)
- SoundPool era criado e `load()` retornava soundId ≠ 0
- Decode nativo falhava: `status=-10000` = `AMEDIA_ERROR_UNKNOWN`
- Por isso `loadedSoundIds` nunca era preenchido e todo `play()` era rejeitado

---

## 2. Causa exata

Os arquivos MP3 em `app/src/main/res/raw/` foram **corrompidos por conversão UTF-8**.

Bytes binários do MPEG (ex.: sync `0xFF`) foram substituídos por `EF BF BD` (U+FFFD, replacement character).

| Arquivo | Antes (f4c8636) | Depois (5aac824) |
|---------|-----------------|------------------|
| button.mp3 | 15597 bytes, 261 frames MPEG, 0× EFBFBD | 28109 bytes, 0 frames MPEG, 6360× EFBFBD |

**Commit responsável:** `5aac824` — *build: downgrade targetSdk to 35 and update audio*  
**Arquivos:** todos os `*.mp3` em `app/src/main/res/raw/`  
**Não é bug de SoundPool/lifecycle/DataStore** no fluxo de SFX.

### Por que AI Studio funcionava

O APK do AI Studio foi gerado com os binários válidos (pré-`5aac824`).  
O build local do Android Studio empacota os MP3 já corrompidos do working tree/HEAD.

### Por que música “às vezes” parecia ok

Só `music_home.mp3` era válido no histórico.  
`music_game/shop/ranking/defeat/victory` já estavam corrompidos desde o commit inicial.

---

## 3. O que NÃO era a causa (descartado com logs)

| Hipótese | Resultado |
|----------|-----------|
| `soundEffectsEnabled=false` / volume 0 | Preferências lidas true / 0.8 |
| SoundPool liberado no `onDestroy` | Já removido; pool existia |
| `createAttributionContext` | Device API 29 — ramo nem executa |
| Recursos ausentes no APK | `resId` e `soundId` OK; falha no decode |
| Ciclo de vida Compose | Singleton Application; sem destroy do manager |

---

## 4. Correção aplicada

1. Restaurar SFX válidos de `f4c8636`
2. Substituir BGM corrompidos por cópia válida de `music_home.mp3` (provisório até reexportar faixas)
3. `.gitattributes` com `*.mp3 binary` (e afins) para evitar nova corrupção
4. Logs TAG `MemoryQuestAudio` em Application / MainActivity / GameAudioManager
5. Remover `createAttributionContext` dos audio managers (não necessário para SFX)

---

## 5. Arquivos que influenciam SFX

| Arquivo | Papel |
|---------|-------|
| `GameAudioManager.kt` | SoundPool, load, playSfx |
| `MusicManager.kt` | BGM (ExoPlayer) |
| `DataStoreManager.kt` | sound_enabled, sfx_volume |
| `MainViewModel.kt` | observeSettings + playButton/loja |
| `GameViewModel.kt` | flip/match/mismatch/powerups |
| `VictoryScreen.kt` | fanfarra / coins |
| `SettingsScreen.kt` | toggle + teste em sequência |
| `NavGraph.kt` | playButton na navegação |
| `MainActivity.kt` | foreground hooks |
| `app/src/main/res/raw/*.mp3` | **payload real do áudio** |

---

## 6. Validação pós-correção (Mi 9T, API 29)

```
OnLoadComplete SUCCESS: soundId=1..10, status=0 | loadedCount=10
prefs soundEffectsEnabled=true
prefs soundEffectsVolume=0.8
play() OK: effect=BUTTON_CLICK, soundId=6, left=0.8, right=0.8,
           priority=1, loop=0, rate=1.0, streamId=1
```

Antes: `status=-10000` em todos → nenhum play.  
Depois: `status=0` + `streamId=1` → SFX funcionando.

---

## 7. Próximos passos

1. Reexportar BGM distintos (hoje são cópia provisória de `music_home.mp3`)
2. Manter `.gitattributes` (`*.mp3 binary`)
3. Remover logs `MemoryQuestAudio` verbosos quando estabilizar
4. Não reintroduzir assets via editor de texto / conversão UTF-8
