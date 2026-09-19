# DashLoader Wiki (Unofficial Continuation)

> Diese Wiki gehört zum Community-Fork (`Malionaro/DashLoader`). Original von AlphaQ (`alphaqu/DashLoader`, LGPL-3.0-only), das bei 1.21.4 stehen geblieben ist. Dieser Fork portiert DashLoader auf **1.21.4 – 26.3**.

## Inhalt

- [Installation](#installation)
- [So funktioniert's](#so-funktionierts)
- [Konfiguration](#konfiguration)
- [Cache verwalten](#cache-verwalten)
- [Fehlerbehebung (Troubleshooting)](#fehlerbehebung-troubleshooting)
- [Bekannte Mod-Kompatibilität](#bekannte-mod-kompatibilität)
- [Für Modpack-Ersteller](#für-modpack-ersteller)
- [Versionen & Branches](#versionen--branches)
- [FAQ](#faq)

## Installation

1. **Fabric Loader ≥ 0.19.5** installieren (für 26.x zusätzlich **Java 25**, für 1.21.x **Java 21**).
2. Die passende DashLoader-Datei von Modrinth laden — das `+1.21.x` / `+26.x` im Dateinamen muss zu deiner Minecraft-Version passen.
3. Die `.jar` in den `mods`-Ordner legen. Fertig — keine weitere Einrichtung nötig.

## So funktioniert's

1. **Erster Start (SAVE):** DashLoader beobachtet das normale Laden und schreibt alles in den Ordner `dashloader-cache/` neben der Instanz. Ein Toast meldet „Caching…". Dieser Start ist **langsamer als normal** — das ist erwartet.
2. **Alle weiteren Starts (LOAD):** Die Daten werden direkt aus dem Cache geladen. Vanilla-Ladeschritte werden wo möglich übersprungen → **deutlich schnellerer Start**, besonders mit großen Modpacks.
3. **Cache-Ungültigkeit:** Bei jeder Änderung (Mod dazu/weg, Update, Resourcepack-Wechsel, Minecraft-Update) wird automatisch ein neuer Cache gebaut (Erkennung per Mod-Hash).

## Konfiguration

- Ingame: ModMenu → DashLoader → Einstellungen (oder Datei `config/dashloader.json`).
- Einzelne Module schaltbar: `CACHE_MODEL_LOADER`, `CACHE_SPRITE_CONTENT`, `CACHE_SPRITE_STITCHING`, `CACHE_ATLASES`, `CACHE_FONT`, `CACHE_SHADER`, `CACHE_SPLASH_TEXT` u. a.
- `showCachingToast`, `compression`, `maxCaches`, `singleThreadedReading`, eigene Splash-Texte (`customSplashLines`, mit `;` trennen).
- Andere Mods können per `dashloader:disableoption` in ihrer `fabric.mod.json` gezielt Optionen abschalten (macht z. B. VulkanMod für Shader/Atlases).

## Cache verwalten

| Aktion | Wie |
|---|---|
| Cache neu bauen | `dashloader-cache/`-Ordner löschen, dann einmal starten |
| Assets neu laden (Resourcepack-/Mod-Dev) | `F3 + T` |
| Cache-Reload | `/dash reload` |
| Cache-Toast an/aus | Config → `Show Caching Toast` |

## Fehlerbehebung (Troubleshooting)

**„Failed caching" / „Failed to save cache" beim Start**
- Seit diesem Fork bricht **ein einzelnes nicht-cachbares Asset den Save nicht mehr ab** — es wird mit `Skipping uncacheable …` im Log übersprungen und vanilla geladen. Wenn der Fehler trotzdem kommt: `logs/latest.log` sichern und als Issue melden.
- Ultimative Lösung: `dashloader-cache/` löschen und einmal neu bauen lassen.

**Spiel startet gar nicht / Crash beim Start**
- Prüfen, ob der Crash auch **ohne** DashLoader passiert (Mod-Hash ändert sich, also immer erst ohne testen).
- Log-Ausschnitt mit `Could not create DashObject …` → das Asset wird beim nächsten Start per Skip behandelt; trotzdem bitte als Issue melden (Mod-Name + `latest.log`).

**Transparente Texturen werden opak (mit Sodium)**
- Bekannter Upstream-Fehler mit aktivem `CacheSpriteContents`. Workaround: `CACHE_SPRITE_CONTENT` in der Config deaktivieren.

**Erster Start ist langsam**
- Normal — da wird der Cache gebaut. Erst der **zweite** Start zeigt den Gewinn.

**Cache wird bei jedem Start neu gebaut**
- Passiert bei Mod-Zusammenstellungen, die sich bei jedem Start ändern (z. B. dynamisch generierte Inhalte). Log auf `Mod hash` prüfen; Issue melden.

## Bekannte Mod-Kompatibilität

Grundsatz dieses Forks: **Unbekannte Assets werden übersprungen, nicht gecrasht.** Konkret behandelt:

| Mod / Fall | Status |
|---|---|
| Refined Storage 2 (Kabel-Models, `#121`) | ✅ Start + Cache ok (Kabel-Parts werden vanilla geladen) |
| Fusion / CTM-Sprites (`#85`) | ✅ Start + Cache ok (Custom-Sprites werden vanilla geladen) |
| Custom Fonts / Emoji-Fonts (Glyphix u.ä., `#61`) | ✅ Fehlerhafte Fonts werden übersprungen |
| Sodium | ✅ Bekannte Einschränkung: transparente Texturen mit `CacheSpriteContents` (siehe oben) |
| Iris / Distant Horizons, Create, Xaero's | ⚠️ Keine bestätigten Probleme auf 1.21.4+ — bitte testen und melden |

Nicht gecachte Assets kosten etwas Startzeit, aber das Spiel läuft.

## Für Modpack-Ersteller

- DashLoader ist **nur clientseitig** und kann bedenkenlos in Packs liegen.
- Für identische Pack-Versionen kann ein vorbereiteter `dashloader-cache/` mitgeliefert werden → auch der allererste Start der Spieler ist schnell.
- Hinweis: Der Cache ist pro Mod-Kombination gültig (Hash). Jede Pack-Änderung baut ihn neu.

## Versionen & Branches

| Status | Versionen |
|---|---|
| ✅ Stabil | 1.21.4 – 1.21.11 |
| ⚠️ Beta | 26.1, 26.1.1, 26.1.2, 26.2, 26.3 |

GPU-Caches (Atlases, Shader) sind ab 1.21.5 **nicht** gecacht — Mojang hat die nötigen Hooks entfernt. CPU-seitiges Caching (Models, Fonts, Sprites) greift weiterhin.

## FAQ

**Ist das offiziell?**
Nein — Community-Fortführung. Das Original ist archiviert/steht bei 1.21.4. Alle Credits für die Basis gehen an AlphaQ.

**Forge / NeoForge?**
Nein (Upstream: „wont fix"). Nur Fabric.

**Ältere Versionen (1.20.x, 1.19.x)?**
Dieser Fork pflegt nur 1.21.4+. Upstream-Dateien für ältere Versionen existieren, werden hier aber nicht weiterentwickelt.

**Wo melden ich Fehler?**
Im [Issue-Tracker](https://github.com/Malionaro/DashLoader/issues) mit `latest.log` (bei PrismLauncher: Instanz → Logs). Vorher bitte `dashloader-cache/` löschen und Fehler reproduzieren.
