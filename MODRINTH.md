# DashLoader (Unofficial Continuation)

> Unofficial continuation of [DashLoader by AlphaQ](https://github.com/alphaqu/DashLoader) (LGPL-3.0-only), ported to newer Minecraft versions. All credit for the original work goes to AlphaQ and contributors. The original project stopped at 1.21.4 - this fork keeps it alive.

## What is DashLoader?

DashLoader caches Minecraft's loaded game resources - **models, textures, fonts, translations and sounds** - after the first launch. On every subsequent start, the cache is loaded instead of re-baking everything, making **game startup significantly faster** (especially with large modpacks).

## How it works

1. **First launch:** DashLoader watches the normal resource loading and writes everything into a cache folder (`dashloader-cache`) next to your instance. A toast notifies you while caching.
2. **Next launches:** The cached data is loaded directly - vanilla loading steps are skipped where possible.

If the cache is outdated or corrupt, it is discarded automatically and rebuilt - you never end up with a broken game because of DashLoader.

## Supported versions

| Status             | Versions                                   |
|--------------------|--------------------------------------------|
| ✅ Stable           | 1.21.4 – 1.21.11 (Fabric)                  |
| ⚠️ Beta / testing  | 26.1, 26.1.1, 26.1.2, 26.2, 26.3 (Fabric) |

Requires **Fabric Loader ≥ 0.19.5**. Java 21 for 1.21.x, Java 25 for 26.x.

## Configuration

- In-game config screen (ModMenu supported) or `config/dashloader.json`
- Toggle individual cache modules (models, fonts, shaders, translations, …)
- Reload the cache with `/dash reload` or wipe it via the config screen

## Known limitations

- GPU-side caches (atlas textures, shaders) are **not** cached on 1.21.5+ - Mojang removed the hooks DashLoader relied on. CPU-side caching still applies.
- The very first launch after installing (or after a Minecraft update) is **not** faster - that's when the cache is built.
- 26.x builds are work in progress: please report issues on the [issue tracker](https://github.com/Malionaro/DashLoader/issues) with your `latest.log` attached.

## For pack makers

DashLoader is client-side only and safe to ship in modpacks. Consider pre-shipping a cache for identical modpack versions to give players fast first starts - see the [wiki](https://github.com/Malionaro/DashLoader) for details.

## License & credits

- Licensed under **LGPL-3.0-only** (see `LICENCE`)
- Original project: [alphaqu/DashLoader](https://github.com/alphaqu/DashLoader) by AlphaQ
- This fork: ports, multi-version build matrix, and fixes for 1.21.5+
