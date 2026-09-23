# DashLoader 5.1.0-beta.9.1-26.2 NeoForge (Unofficial Continuation) - ALPHA

First NeoForge port (26.2, NeoForge 26.2.0.88). Experimental.

## What works

- Mod discovery, init, full SAVE and LOAD cycle verified
- Model, sprite, font (except Unihex, vanilla fallback) and stitch caching
- Toast, config screen via NeoForge extension point
- `UNSAFE_MIPMAP_GENERATION` auto-disabled (MixinExtras conflict)

## Known limitations

- `MainMixin`/`BootstrapMixin` excluded (early-boot touch kills NeoForge startup; bootstrap is redundant, timing log lost)
- Unihex fonts load vanilla (flattened cache types not yet wired to all loaders)
- Per-mod `dashloader:disableoption` opt-outs unsupported (no NeoForge custom values)
- Quilt detection inactive on this loader

Requires NeoForge 26.2.0.88 and Java 25. Fabric/Quilt builds are separate files (plus-sign versions).
