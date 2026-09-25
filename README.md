<p align="center"><img src="./src/main/resources/dashloader/textures/icon.png" alt="Logo"></p>
<h1 align="center">DashLoader<br/>

<a href="https://github.com/Malionaro/DashLoader/releases"><img alt="GitHub Releases" src="https://img.shields.io/github/v/release/Malionaro/DashLoader?label=Download&logo=github&color=%2336A853"></a>
<a href="https://github.com/alphaqu/DashLoader"><img alt="Upstream Project" src="https://img.shields.io/badge/Upstream-alphaqu%2FDashLoader?logo=github&label=Upstream&color=%23181717"></a>
<a href="https://ko-fi.com/notequalalpha"><img alt="ko-fi page" src="https://img.shields.io/badge/Support%20AlphaQ-kofi?logo=kofi&logoColor=%23FF6433&label=Ko-fi&color=%23FF6433"></a>
</h1>

> [!IMPORTANT]
> This is an **unofficial continuation** of DashLoader. Upstream development stopped at Minecraft 1.21.4 — this fork keeps it alive for 1.21.4 through 26.3, plus NeoForge and Forge ports.
>
> Grab the builds on the [Releases page](https://github.com/Malionaro/DashLoader/releases) and [please report any issues here](https://github.com/Malionaro/DashLoader/issues)!

### Welcome to DashLoader!

Ever hated staring at the Minecraft loading screen? DashLoader accelerates the asset loading system by caching all of its content, leading to a much faster game load.

It does this by caching everything on first launch and loading back that exact cache on the next launch. The cache loading is hyper fast and scalable, utilizing your entire system.

### Supported versions

| Loader | Minecraft | Java | Release |
|---|---|---|---|
| Fabric | 1.21.4 – 1.21.11 | 21 | [beta.9.3](https://github.com/Malionaro/DashLoader/releases) |
| Fabric | 26.1 – 26.3 | 25 | [beta.9.3](https://github.com/Malionaro/DashLoader/releases) |
| NeoForge | 1.21.11, 26.2, 26.3 | 21 / 25 | [beta.9.3](https://github.com/Malionaro/DashLoader/releases) |
| Forge | 1.16.5 | 8 | [beta.9.3](https://github.com/Malionaro/DashLoader/releases) |

Requires Fabric Loader 0.19.5 or newer on Fabric. Quilt is compatible with the Fabric builds.

### Important notes

- The **first launch** with DashLoader will be **significantly slower**, because it needs to create a cache containing all assets Minecraft normally loads. This also happens every time you change a mod or resource pack if that configuration has no cache yet.
- DashLoader has been known to be incompatible with lots of mods. DashLoader 3.0 massively improved compatibility by no longer forcing mod developers to add explicit support: assets that cannot be cached simply load normally (which hurts speed, as the vanilla loading system is quite slow). This fork additionally **skips uncacheable assets with a warning instead of crashing the cache creation**.
- If you develop mods or create resource packs, press <code>F3 + T</code> to recreate the cache and load your new assets in.

### Community

The original community gathers around the [upstream project](https://github.com/alphaqu/DashLoader). For this fork, please use [GitHub Issues](https://github.com/Malionaro/DashLoader/issues).

### Sponsors

[YourKit](https://www.yourkit.com/java/profiler/) makes amazing profilers for both Java and .NET. Upstream uses their Java Profiler to understand where to optimize further and make DashLoader faster.

[JetBrains](https://www.jetbrains.com/) creates excellent IDEs and provided upstream with access to their enterprise products to develop DashLoader and Hyphen.

### Donate

AlphaQ has a [Ko-Fi page](https://ko-fi.com/notequalalpha) if you would like to support the original author. Please only support if you like what they do, and you are not in a bad financial situation to do so.

### Credits

DashLoader was created by [AlphaQ (NotAlpha)](https://github.com/alphaqu) and contributors. This continuation is maintained by [Malionaro](https://github.com/Malionaro), with resilience fixes, version ports, and translations for the community.
