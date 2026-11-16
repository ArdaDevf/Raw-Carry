# Modrinth Publish Metadata

## Project

- **Title:** RawCarry
- **Slug (öneri):** `rawcarry` ya da `raw-carry`
- **Summary:** Carry players on your shoulders — sneak right-click pickup, sneak left-click throw, player towers. Lightweight Paper plugin.
- **Categories:** `utility`, `gameplay`
- **License:** MIT
- **Client side:** unsupported / Server side: required
- **Project icon:** gerekli — 400x400 ya da 1:1 görsel (Arda temin eder; yoksa basit omuz-kişilik placeholder)

## Version 1.0.1

- **Version number:** `1.0.1`
- **Channel:** `release`
- **Loaders:** `paper`
- **Game versions:** `1.21`, `1.21.1` … `1.21.11` (api-version 1.21; canlı test 1.21.0'da yapıldı)
- **Files:** `target/raw-carry-1.0.1.jar`
- **Dependencies (project):** yok
- **Changelog:**

```
## 1.0.1
- Fixed the sneak left-click throw sometimes not firing when the attack packet is processed before the sneak state update — the throw now uses a short late-click window.
- Throwing while sneaking no longer damages the player or mob you click on; the hit is cancelled and the throw happens instead.
- Throw velocity is re-applied after dismount position sync to guarantee a clean launch.
- Minor metadata and documentation updates.
```

## Açık maddeler (publish öncesi Arda)

1. **Modrinth hesap** — panelden manuel ya da bana API token ver (Create version API ile direkt yüklerim).
2. **GitHub repo kararı** — proje git repo değil; repo açarsan plugin.yml `website`/`sources` alanları doldurulur + source link Modrinth sayfasına girer. Karar Arda'da.
3. **Project icon** — 1:1 görsel.
4. **Train zinciri canlı testi** — carry + yeet canlı onaylı; `/train` 2+ oyuncu ister, ikinci bir botla self-test yapılabilir ya da release sonrası teste kalır.
5. **Bilinen 3 dar-kenar low bulgu** (DevLog'da) — publish'a engel değil, sonraki patch turunda.
