<p align="center"><img src="./src/main/resources/dashloader/textures/icon.png" alt="Logo" width="128"></p>
<h1 align="center">DashLoader<br/>

<a href="https://github.com/Malionaro/DashLoader/releases"><img alt="GitHub Releases" src="https://img.shields.io/github/v/release/Malionaro/DashLoader?label=Download&logo=github&color=%2336A853"></a>
<a href="https://github.com/alphaqu/DashLoader"><img alt="Upstream Project" src="https://img.shields.io/badge/Upstream-alphaqu%2FDashLoader?logo=github&label=Upstream&color=%23181717"></a>
<a href="https://ko-fi.com/notequalalpha"><img alt="ko-fi page" src="https://img.shields.io/badge/Support%20Me-kofi?logo=kofi&logoColor=%23FF6433&label=Ko-fi&color=%23FF6433"></a>
</h1>

> [!IMPORTANT]
> Upstream development ended with Minecraft 1.21.4. This repository continues maintenance and ports for newer versions.
>
> Downloads are on the [Releases page](https://github.com/Malionaro/DashLoader/releases) — please report any issues you find with DashLoader [here](https://github.com/Malionaro/DashLoader/issues).

### Welcome to DashLoader!

This mod accelerates the Minecraft asset loading system by caching all of its content. This leads to a much faster game load.

It does this by caching all of its content on first launch and on next launch loading back that exact cache. The cache loading is hyper fast and scalable which utilises your entire system.

### Supported versions

| Loader | Minecraft | Java |
|---|---|---|
| Fabric | 1.21.4 – 1.21.11 | 21 |
| Fabric | 26.1 – 26.3 | 25 |
| NeoForge | 1.21.11, 26.2, 26.3 | 21 / 25 |
| Forge | 1.16.5 | 8 |

Fabric builds require Fabric Loader 0.19.5 or newer. Quilt is compatible with the Fabric builds.

### Important notes

- The first time you launch DashLoader it will be **significantly slower**, because it needs to create a cache which contains all the assets Minecraft normally loads. This will also happen every time you change a mod or resource pack if that configuration does not have an existing cache.
- DashLoader has been known to be incompatible with a lot of mods. DashLoader 3.0 has massively improved compatibility by not forcing mod developers to add explicit support to make their assets cachable. This means that DashLoader will load assets normally for mod assets that cannot be cached. While this improves mod compatibility it hurts speed as the Minecraft loading system is quite slow. Assets that cannot be cached are skipped with a warning instead of aborting the cache creation.
- If you use DashLoader for developing mods or creating resource packs you can press <code>F3 + T</code> to recreate the cache to load your new assets in.

### Community

The original community gathers around the [upstream project](https://github.com/alphaqu/DashLoader). For this codebase, please use [GitHub Issues](https://github.com/Malionaro/DashLoader/issues).

### Sponsors

[YourKit](https://www.yourkit.com/java/profiler/) makes amazing profilers for both Java and .NET. We use their Java Profiler to understand where to optimize further and make DashLoader faster.

[JetBrains](https://www.jetbrains.com/) creates excellent IDEs for all programmers and have provided us with access to their enterprise products for use to develop DashLoader and Hyphen.

### Donate

I have a [Ko-Fi page](https://ko-fi.com/notequalalpha) if you would like to support me. Please only support me if you like what I do, and you are not in a bad financial situation to do so.
