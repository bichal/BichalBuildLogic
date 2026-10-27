# {{name}}

**One sentence on what {{name}} does for players.**

---

## ✨ Features

| Feature | What it does |
|---|---|
| **Feature** | What it does |

## 📋 Requirements

| | |
|---|---|
| **Minecraft** | 1.21.1 |
| **Loader** | NeoForge 21.1.x |
| **Java** | 21+ |

<!-- publish:off -->

## Building

Clone [BichalBuildLogic](https://github.com/bichal/BichalBuildLogic) next to {{name}} as `bichal-build-logic`:

```bash
git clone https://github.com/bichal/BichalBuildLogic.git bichal-build-logic
cd {{repo}} && ./gradlew buildAndCollect
```

## Releasing

This README is also the description on Modrinth and CurseForge (everything outside `publish:off` blocks). Add
`docs/changelogs/<version>.md`, set `version` under `[mod]`, then `bichal release {{id}} --push`.

<!-- bichal:license -->
<!-- /bichal:license -->

<!-- publish:on -->

---

<!-- bichal:footer -->
<!-- /bichal:footer -->
