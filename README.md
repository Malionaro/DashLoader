# DashLoader — Unofficial Continuation (1.21.4 – 26.3)

> Community continuation of [DashLoader by AlphaQ](https://github.com/alphaqu/DashLoader) (LGPL-3.0-only).
> The original project stopped at 1.21.4 — this fork ports it to newer versions and fixes cache failures.
> Full credit for the original work goes to AlphaQ and all contributors.

DashLoader accelerates Minecraft startup by caching all loaded game resources (models, textures, fonts, translations) on the first launch and loading them back from cache on every later launch.

## Links

- 📦 Modrinth: https://modrinth.com/mod/dashloader
- 🚀 Releases (all versions): https://github.com/Malionaro/DashLoader/releases
- 📖 Wiki: https://github.com/Malionaro/DashLoader/wiki
- 🐛 Issues: https://github.com/Malionaro/DashLoader/issues
- 💬 Upstream Discord: https://discord.gg/8F8MaYzk5h
- 📜 License: LGPL-3.0-only (`LICENCE`)
- ⬆️ Upstream: https://github.com/alphaqu/DashLoader (archived at 1.21.4)

## Versions

| Status | Versions | Java | Loader |
|---|---|---|---|
| ✅ Stable | 1.21.4 – 1.21.11 | 21 | Fabric ≥ 0.19.5, Quilt |
| ⚠️ Beta | 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | 25 | Fabric ≥ 0.19.5, Quilt |

Each Minecraft version lives on its own branch (`fabric-<mcVersion>`).

## Notes

- The **first launch is significantly slower** — that's when the cache is built. Every later launch is much faster.
- Uncacheable mod assets are **skipped with a warning** and loaded vanilla instead of breaking the cache.
- GPU-side caches (atlases, shaders) are not cached on 1.21.5+ — Mojang removed the required hooks.
- Press `F3 + T` to rebuild the cache (e.g. mod/resource-pack development).

## For developers

```bash
# build active version
./gradlew build
# jars land in build/libs/dashloader-<version>+<mc>.jar
```

Requires Hyphen + Taski from `mavenLocal` (see guide in repo) since `notalpha.dev` maven is offline.
