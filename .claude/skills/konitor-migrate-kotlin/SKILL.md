# konitor-migrate-kotlin

## Purpose

Guides a safe Kotlin version bump in Konitor — updates the single `kotlin` entry in `gradle/libs.versions.toml`, checks Kotlin↔Compose, Kotlin↔Coroutines, and Kotlin↔Mokkery compatibility before touching any file, and verifies the build passes before committing.

## When to Use

- User invokes `/konitor-migrate-kotlin <new-version>` (e.g. `/konitor-migrate-kotlin 2.4.0`).
- When a new Kotlin release is available.
- When a new coroutines or Compose Multiplatform version requires a higher minimum Kotlin.
- When the build emits "This build uses Kotlin X.Y.Z; the minimum required version is …".

## Steps

1. **Record current state**:

   ```bash
   grep "^kotlin " gradle/libs.versions.toml
   grep "^coroutines " gradle/libs.versions.toml
   grep "^compose " gradle/libs.versions.toml
   grep "^mokkery " gradle/libs.versions.toml
   grep "^agp " gradle/libs.versions.toml
   cat gradle/wrapper/gradle-wrapper.properties
   ```

   Note: `KOTLIN_OLD`, `COROUTINES_CURRENT`, `COMPOSE_CURRENT`, `MOKKERY_CURRENT`, `AGP_CURRENT`, `GRADLE_CURRENT`.

2. **Check compatibility before editing** — verify `<new-version>` is supported by the current:
   - Compose Multiplatform (`compose` version in `libs.versions.toml`) — see Reference table
   - Coroutines (`coroutines` version) — coroutines 1.9+ requires Kotlin 2.0+
   - Mokkery (`mokkery` version) — check mokkery release notes for Kotlin floor
   - Kotlin Compose plugin — in this repo it shares the `kotlin` version alias; they move together automatically
   - Minimum Gradle version required by the new KGP — see `/konitor-migrate-gradle` Reference

   If any dependency requires a version bump first, do that before proceeding.

3. **Update the single version entry**:

   Edit `gradle/libs.versions.toml` — change only the `kotlin` line:

   ```toml
   kotlin = "<new-version>"
   ```

   The following all resolve through this alias automatically — do not touch them individually:
   - `kotlin-gradle-plugin` library (build-logic classpath)
   - `kotlin-multiplatform`, `kotlin-android`, `kotlin-jvm`, `kotlin-compose` plugins
   - `kotlin-android` extension on all Android modules

4. **Run a build-logic classpath check**:

   ```bash
   ./gradlew :build-logic:validatePlugins --no-daemon 2>&1 | tail -30
   ```

5. **Run the full unit test sweep**:

   ```bash
   ./gradlew test
   ```

6. **Run detekt**:

   ```bash
   ./gradlew detekt
   ```

   Note: Detekt 1.x may emit a "Kotlin version mismatch" warning when Kotlin ≥ 2.1. This is informational only and does not block the build. If detekt itself needs a bump, edit the `detekt` version in `libs.versions.toml`.

7. **Commit if all green**:

   ```
   chore(deps): bump kotlin from <old> to <new-version>
   ```

8. **If the build fails**, revert the `kotlin` line to `KOTLIN_OLD`, report the exact error, and identify whether it is a coroutines/Compose floor violation, a Mokkery incompatibility, or a K2 compiler diagnostic.

## Reference

### File locations

| What | File | Key |
|------|------|-----|
| Kotlin version | `gradle/libs.versions.toml` | `kotlin` in `[versions]` |
| Coroutines version | `gradle/libs.versions.toml` | `coroutines` |
| Compose MP version | `gradle/libs.versions.toml` | `compose` |
| Mokkery version | `gradle/libs.versions.toml` | `mokkery` |
| Kotlin Compose plugin | `gradle/libs.versions.toml` | `kotlin-compose` plugin — shares `kotlin` alias |

Current repo: **Kotlin 2.3.21**, coroutines **1.10.2**, Compose MP **1.9.3**, Mokkery **3.3.0**

### Kotlin ↔ Compose Multiplatform compatibility

The `compose` version in `libs.versions.toml` controls the `org.jetbrains.compose` plugin. This plugin has a hard dependency on a specific Kotlin version range.

| Compose MP | Supported Kotlin |
|-----------|-----------------|
| 1.7.x | 2.0.x |
| 1.8.x | 2.1.x |
| 1.9.x | 2.3.x |
| 2.0.x | 2.4.x (expected) |

Rule: if the new Kotlin version is outside the supported range for `COMPOSE_CURRENT`, bump `compose` in `libs.versions.toml` to a compatible release at the same time.

Note: `kotlin-compose` (`org.jetbrains.kotlin.plugin.compose`) is a separate plugin from `compose-multiplatform` (`org.jetbrains.compose`). The former shares the `kotlin` version alias and moves automatically. The latter (`compose` alias) must be bumped manually when Kotlin jumps to a new minor.

### Kotlin ↔ Coroutines compatibility

| Coroutines | Minimum Kotlin |
|-----------|---------------|
| 1.8.x | 1.9.0 |
| 1.9.x | 2.0.0 |
| 1.10.x | 2.0.0 |

Current: coroutines 1.10.2 → minimum Kotlin 2.0. Any Kotlin ≥ 2.0 is safe for 1.10.x.

### Kotlin ↔ Mokkery compatibility

Mokkery uses the Kotlin compiler plugin API. Incompatible Kotlin versions manifest as:

- `Unsupported Kotlin version X.Y.Z for dev.mokkery`
- Compilation errors in generated mock code

Check https://github.com/lupuuss/Mokkery/releases for the latest supported Kotlin versions before bumping.

### K2 compiler notes (Kotlin 2.x)

Konitor is already on K2 (Kotlin 2.3.21). K2-specific considerations for future bumps:

- K2 error messages are stricter than K1 — some code that only produced warnings under K1 becomes errors under K2.
- `@OptIn` annotations must be complete — K2 enforces opt-in propagation more strictly.
- Smart-cast improvements in K2 may cause `Type mismatch` in code that relied on K1's looser inference.
- If the Kotlin bump is a minor (e.g. 2.3 → 2.4), check the [What's New in Kotlin 2.4] changelog for deprecations that become errors.

### What NOT to change

- Do not edit individual module `build.gradle.kts` files — all Kotlin plugin references go through the convention plugins.
- Do not change the `kotlin-compose` plugin version separately — it shares the `kotlin` alias in this repo and must stay in sync.
- Do not run `connectedAndroidTest` or `assembleXCFramework` to verify.
- Do not bump `coroutines` or `compose` unless the compatibility check in Step 2 requires it.
