# Releasing a new version (solo guide)

Everything happens in `C:\Users\andre\Desktop\Code\enderpocket`, in a PowerShell window.

## 1. Bump the version

Open `gradle.properties` and change the line:

```
mod_version=0.2.1
```

to the new number. Rule of thumb: bugfix → bump the last digit (`0.2.1` → `0.2.2`),
new feature → bump the middle (`0.2.x` → `0.3.0`).

## 2. Build

```powershell
cd C:\Users\andre\Desktop\Code\enderpocket
$env:JAVA_HOME = 'C:\Users\andre\.jdks\jdk-25.0.3+9'
.\gradlew.bat build
```

Wait for `BUILD SUCCESSFUL`. The jar lands in `build\libs\enderpocket-<version>.jar`.
(If the build fails, the error lines above `BUILD FAILED` tell you which file/line to look at.)

## 3. Install into your profile

```powershell
# remove the old one first (adjust the old version number)
Remove-Item "$env:APPDATA\ModrinthApp\profiles\Fabulously Optimized (1)\mods\enderpocket-*.jar"
Copy-Item "build\libs\enderpocket-<version>.jar" "$env:APPDATA\ModrinthApp\profiles\Fabulously Optimized (1)\mods\"
```

Launch the game and test before publishing anything.

## 4. Commit + push

```powershell
git add -A
git commit -m "v<version> — short description of what changed"
git push
```

## 5. Modrinth

modrinth.com → your project → **Versions → Create version**:

- **Version number**: must match the jar (e.g. `0.2.2`)
- **Type**: Release
- **File**: the jar from `build\libs`
- **Game version** 26.2, **loader** Fabric, dependency **Fabric API** (required)
- **Changelog**: markdown; a short `## Fixed` / `## Added` / `## Changed` list is plenty

## Gotchas

- **Java**: the build needs JDK 25 — that's why the `$env:JAVA_HOME` line is there.
  Without it the system default (Java 8) is picked up and the build fails immediately.
- **Version mismatch**: the jar's internal version comes from `gradle.properties`.
  If you forget step 1 you'll build a jar that overwrites the previous version's name —
  easy to confuse yourself; always bump first.
- **libs folder**: `libs\modmenu-*.jar` and `libs\cloth-config-*.jar` are compile-time
  copies used by the build. Don't delete them. If you ever update to a new Minecraft
  version, replace them with the matching jars from your mods folder and update the
  file names in `build.gradle`.
- **New Minecraft version**: also update `minecraft_version`, `loader_version` and
  `fabric_api_version` in `gradle.properties` (values from https://fabricmc.net/develop),
  then expect some code fixes — that's a bigger job than a routine release.
