# Memoria

Carnet de bord mobile (Android) pour suivre au quotidien les échanges et
transactions de cartes Pokémon : photographier les éléments, taguer en entrant
(acquis) / sortant (cédé), enregistrer prix d'achat et de vente, le tout
pilotable à la voix.

Cible : Samsung Galaxy Z Flip 6, Android 14+ (minSdk 29, targetSdk 34).

## Stack
- Kotlin + Jetpack Compose
- AGP 8.6, Gradle 8.10
- Room (base locale) + sauvegarde Google Drive
- CameraX (photos)
- Voix : transcription OpenAI → extraction LLM (sens / prix / nom)
- Reconnaissance carte : LLM vision + correction vocale
- Prix marché : TCGPricer (https://www.d8a.fr)

## Build local

Le toolchain n'est pas dans le repo. Sur uTCGDEV :

```bash
source /opt/datas/tools/android-env.sh
./gradlew assembleDebug
```

`local.properties` (gitignored) pointe vers `/opt/datas/android-sdk`.

## Suivi
Jira projet **AOO**, épic **AOO-11** (Memoria).

## Versioning
`VERSION` à la racine + `versionName` dans `app/build.gradle.kts`. Bump patch à
chaque commit qui modifie le code.
