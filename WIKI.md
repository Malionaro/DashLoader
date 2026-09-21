# DashLoader Wiki - Unofficial Continuation (1.21.4 – 26.3)

> This wiki belongs to the community fork (`Malionaro/DashLoader`). The original by AlphaQ (`alphaqu/DashLoader`, LGPL-3.0-only) stopped at 1.21.4. This fork ports DashLoader to **1.21.4 – 26.3**.

## Contents

- [Installation](#installation)
- [How it works](#how-it-works)
- [Configuration](#configuration)
- [Managing the cache](#managing-the-cache)
- [Troubleshooting](#troubleshooting)
- [Known mod compatibility](#known-mod-compatibility)
- [For modpack makers](#for-modpack-makers)
- [Versions & branches](#versions--branches)
- [FAQ](#faq)

## Installation

1. Install **Fabric Loader ≥ 0.19.5** (plus **Java 25** for 26.x, **Java 21** for 1.21.x).
2. Download the matching DashLoader file from Modrinth - the `+1.21.x` / `+26.x` in the filename must match your Minecraft version.
3. Drop the `.jar` into the `mods` folder. Done - no further setup needed.

## How it works

1. **First launch (SAVE):** DashLoader observes normal loading and writes everything into the `dashloader-cache/` folder next to your instance. A toast reports "Caching…". This launch is **slower than normal** - that's expected.
2. **Every later launch (LOAD):** Data is loaded straight from the cache. Vanilla loading steps are skipped where possible → **much faster startup**, especially with large modpacks.
3. **Cache invalidation:** Any change (mod added/removed/updated, resource pack switch, Minecraft update) automatically builds a fresh cache (detected via mod hash).

## Configuration

- In-game: ModMenu → DashLoader → Settings (or the file `config/dashloader.json`).
- Individual modules can be toggled: `CACHE_MODEL_LOADER`, `CACHE_SPRITE_CONTENT`, `CACHE_SPRITE_STITCHING`, `CACHE_ATLASES`, `CACHE_FONT`, `CACHE_SHADER`, `CACHE_SPLASH_TEXT` and more.
- `showCachingToast`, `compression`, `maxCaches`, `singleThreadedReading`, custom splash lines (`customSplashLines`, separated with `;`).
- Other mods can selectively disable options via `dashloader:disableoption` in their `fabric.mod.json` (e.g. VulkanMod does this for shaders/atlases).

## Managing the cache

| Action | How |
|---|---|
| Rebuild the cache | Delete the `dashloader-cache/` folder, then launch once |
| Reload assets (resource pack / mod dev) | `F3 + T` |
| Reload the cache | `/dash reload` |
| Toggle the cache toast | Config → `Show Caching Toast` |

## Troubleshooting

**"Failed caching" / "Failed to save cache" on startup**
- Since this fork, **a single uncacheable asset no longer aborts the save** - it is skipped with a `Skipping uncacheable …` log line and loaded vanilla. If the error still occurs: back up `logs/latest.log` and report it as an issue.
- Ultimate fix: delete `dashloader-cache/` and let it rebuild once.

**Game doesn't start at all / crash on startup**
- Check whether the crash also happens **without** DashLoader (the mod hash changes, so always test without it first).
- A log snippet with `Could not create DashObject …` means the asset will be skip-handled on the next start; still report it as an issue (mod name + `latest.log`).

**Transparent textures render opaque (with Sodium)**
- Known upstream bug with `CacheSpriteContents` enabled. Workaround: disable `CACHE_SPRITE_CONTENT` in the config.

**First launch is slow**
- Normal - that's when the cache is built. Only the **second** launch shows the speedup.

**Cache rebuilds on every launch**
- Happens with mod setups that change every start (e.g. dynamically generated content). Check the log for `Mod hash`; report an issue.

## Known mod compatibility

This fork's principle: **unknown assets are skipped, not crashed.** Handled cases:

| Mod / case | Status |
|---|---|
| Refined Storage 2 (cable models, `#121`) | ✅ Startup + cache fine (cable parts load vanilla) |
| Fusion / CTM sprites (`#85`) | ✅ Startup + cache fine (custom sprites load vanilla) |
| Custom fonts / emoji fonts (Glyphix etc., `#61`) | ✅ Broken fonts are skipped |
| Sodium | ✅ Tested working on 26.2 (with Iris). Known limitation: transparent textures with `CacheSpriteContents` (see above) |
| Iris / Distant Horizons, Create, Xaero's | ⚠️ Iris tested working on 26.2 - others unconfirmed on 1.21.4+, please test and report |

Uncached assets cost some startup time, but the game runs.

## For modpack makers

- DashLoader is **client-side only** and safe to ship in packs.
- For identical pack versions, a prebuilt `dashloader-cache/` can be bundled → even the very first launch is fast for players.
- Note: the cache is valid per mod combination (hash). Every pack change rebuilds it.

## Versions & branches

| Status | Versions |
|---|---|
| ✅ Stable | 1.21.4 – 1.21.11 |
| ⚠️ Beta | 26.1, 26.1.1, 26.1.2, 26.2, 26.3 |

GPU caches (atlases, shaders) are **not** cached on 1.21.5+ - Mojang removed the required hooks. CPU-side caching (models, fonts, sprites) still applies.

## FAQ

**Is this official?**
No - a community continuation. The original is archived/stopped at 1.21.4. All credit for the base goes to AlphaQ.

**Forge / NeoForge?**
No (upstream: "wont fix"). Fabric only.

**Older versions (1.20.x, 1.19.x)?**
This fork only maintains 1.21.4+. Upstream files for older versions exist but are not developed further here.

**Where do I report bugs?**
In the [issue tracker](https://github.com/Malionaro/DashLoader/issues) with `latest.log` attached (PrismLauncher: instance → Logs). Please delete `dashloader-cache/` first and reproduce the error.
