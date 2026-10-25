# Minimal Peripherals

Petit addon **CC: Tweaked** pour **Minecraft 1.21.11 / Fabric uniquement**.
Dérivé de [Classic Peripherals](https://github.com/Ale32bit/ClassicPeripherals) (MIT, © 2025 AlexDevs).

| Objet | Peripheral Lua |
|---|---|
| Cryptographic Accelerator | `cryptographic_accelerator` (md5/sha*, hmac, blake3, AES, Ed25519, ECDH, base64, deflate/inflate, random) |
| NFC Reader + NFC Card | `nfc_reader` (`write`, `cancelWrite`, `sign`, `cancelSign`, events `nfc_data` / `nfc_write` / `nfc_sign`) |
| RFID Scanner + RFID Badge | `rfid_scanner` (`scan()` -> event `rfid_scan`) |

## Build
```
./gradlew build      # jar dans build/libs/
./gradlew runClient
```
Dépendances : Fabric Loader 0.19.5, Fabric API 0.141.6+1.21.11, CC: Tweaked 1.117.1 (dernier build 1.21.11).

## Différences avec Classic Peripherals
- Données et clé privée stockées dans les composants de l'objet (plus de SavedData serveur).
- Pas de lecture NFC par pocket computer, ni d'upgrades pocket/turtle, ni de teinture des cartes.
- Pas de badge injecté dans les mobs, ni de support Trinkets/Curios/Sable.
- Recettes : la bobine de cuivre est remplacée par un lingot de cuivre.
- Limites fixes (`Limits.java`) à la place du fichier de config.
- Scanner RFID : pas de waterlogging, forme fixe.
