the # konitor-migrate-agp

## Purpose

Guides a safe AGP (Android Gradle Plugin) version bump in Konitor — updates the single `agp` version entry in `gradle/libs.versions.toml`, checks AGP↔Gradle and AGP↔Kotlin compatibility before touching any file, and verifies the build passes before committing.

## When to Use

- User invokes `/konitor-migrate-agp <new-version>` (e.g. `/konitor-migrate-agp 8.14.0`).
- When a new AGP release is available and the team wants to upgrade.
- When Gradle or compileSdk bumps require a minimum AGP version.
- When build warnings say "AGP X.Y.Z is outdated; upgrade to …".

## Steps

1. **Record current state** — read the version that will be replaced:

   ```bash
   grep "^agp " gradle/libs.versions.toml
   cat gradle/wrapper/gradle-wrapper.properties
   grep "^kotlin " gradle/libs.versions.toml
   ```

   Note: `AGP_OLD`, `GRADLE_CURRENT`, `KOTLIN_CURRENT`.

2. **Check compatibility before editing anything** — verify `<new-version>` is compatible with the current Gradle wrapper and Kotlin plugin versions using the table in the Reference section. If the new AGP requires a Gradle version higher than `GRADLE_CURRENT`, run `/konitor-migrate-gradle` first or ask the user how to proceed.

3. **Update the single version entry**:

   Edit `gradle/libs.versions.toml` — change only the `agp` line:

   ```toml
   agp = "<new-version>"
   ```

   All downstream references (`android-gradle-plugin`, `android-library`, `android-application` in `[plugins]`) resolve through this single alias — do not touch them.

4. **Run a sync check** — verify the build-logic classpath resolves:

   ```bash
   ./gradlew :build-logic:validatePlugins --no-daemon 2>&1 | tail -20
   ```

   If this fails with a classpath error, the AGP version may conflict with the Gradle wrapper version — stop and report.

5. **Run the full unit test sweep**:

   ```bash
   ./gradlew test
   ```

   Do NOT run `connectedAndroidTest` — it requires a connected device and must never be invoked autonomously.

6. **Run detekt**:

   ```bash
   ./gradlew detekt
   ```

7. **Commit if all green**:

   ```
   chore(deps): bump agp from <old> to <new-version>
   ```

   No `resolves #N`, no emojis, imperative mood.

8. **If the build fails**, revert the `agp` line to `AGP_OLD`, report the exact error, and suggest the corrective action (usually: bump Gradle wrapper first, or hold at the compatible AGP version).

## Reference

### File locations

| What | File | Key |
|------|------|-----|
| AGP version | `gradle/libs.versions.toml` | `agp` in `[versions]` |
| Gradle wrapper | `gradle/wrapper/gradle-wrapper.properties` | `distributionUrl` |
| Convention plugin classpath | `build-logic/build.gradle.kts` | `android-gradle-plugin` library alias |

### AGP ↔ Gradle compatibility (minimum Gradle required per AGP)

| AGP | Minimum Gradle |
|-----|---------------|
| 8.9 | 8.11.1 |
| 8.10 | 8.11.1 |
| 8.11 | 8.11.1 |
| 8.12 | 8.11.1 |
| 8.13 | 8.11.1 |
| 8.14+ | Check https://developer.android.com/build/releases/gradle-plugin#updating-gradle |

Current repo: **AGP 8.13.0**, Gradle **8.13**, Kotlin **2.3.21**

### AGP ↔ Kotlin compatibility

AGP does not directly version-lock Kotlin, but the Kotlin Gradle Plugin and AGP must be applied to the same project. Conflicts typically surface as:

- `Unsupported plugin type 'KotlinAndroidPluginWrapper'`
- Missing `kotlin-android` extension on `android {}` block

Rule of thumb: if Kotlin is ≥ 2.0 and AGP is ≥ 8.0, they are generally compatible. Check the Kotlin release notes for explicit AGP floor versions if the Kotlin version is also changing in the same PR.

### compileSdk / targetSdk considerations

AGP major bumps sometimes raise the minimum `compileSdk`. These values live in:

- `gradle/libs.versions.toml` → `compile-sdk`, `target-sdk`, `min-sdk`
- `build-logic/src/main/kotlin/AndroidConfigPlugin.kt`

If the new AGP emits a warning like "compileSdk 35 is lower than the minimum required", bump `compile-sdk` in `libs.versions.toml` to match.

### What NOT to change

- Do not edit `build-logic/build.gradle.kts` — the AGP classpath dependency uses `libs.android.gradle.plugin` which already resolves through the `agp` version alias.
- Do not run `connectedAndroidTest` or `assembleXCFramework` to verify — `./gradlew test` is sufficient.
- Do not change individual module `build.gradle.kts` files — all AGP plugin references go through the convention plugins.
