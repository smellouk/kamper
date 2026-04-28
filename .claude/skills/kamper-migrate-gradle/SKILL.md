# kamper-migrate-gradle

## Purpose

Guides a safe Gradle wrapper version bump in Kamper — updates `gradle/wrapper/gradle-wrapper.properties`, checks Gradle↔AGP and Gradle↔Kotlin compatibility before editing, runs the wrapper self-check, and verifies the build before committing.

## When to Use

- User invokes `/kamper-migrate-gradle <new-version>` (e.g. `/kamper-migrate-gradle 8.14`).
- When a new Gradle release is available.
- When a new AGP version requires a higher minimum Gradle (see `/kamper-migrate-agp` Reference).
- When the build emits "The current Gradle version X.Y is not compatible with …".
- When `./gradlew wrapper --gradle-version <X>` is the desired upgrade path.

## Steps

1. **Record current state**:

   ```bash
   cat gradle/wrapper/gradle-wrapper.properties
   grep "^agp " gradle/libs.versions.toml
   grep "^kotlin " gradle/libs.versions.toml
   ```

   Note: `GRADLE_OLD`, `AGP_CURRENT`, `KOTLIN_CURRENT`.

2. **Check compatibility before editing** — verify `<new-version>` satisfies the minimum required by `AGP_CURRENT` and is supported by `KOTLIN_CURRENT` Gradle plugin. Use the table in the Reference section.

3. **Update the wrapper** — change only the `distributionUrl` line in `gradle/wrapper/gradle-wrapper.properties`:

   ```properties
   distributionUrl=https\://services.gradle.org/distributions/gradle-<new-version>-bin.zip
   ```

   Keep `distributionBase`, `distributionPath`, `zipStoreBase`, `zipStorePath` unchanged.

4. **Run the wrapper validation**:

   ```bash
   ./gradlew --version
   ```

   Confirm the printed Gradle version matches `<new-version>`. If it does not, the wrapper properties may not have been updated correctly.

5. **Run a build-logic check** to catch plugin API breaks introduced by the new Gradle release:

   ```bash
   ./gradlew :build-logic:validatePlugins --no-daemon 2>&1 | tail -30
   ```

6. **Run the full unit test sweep**:

   ```bash
   ./gradlew test
   ```

7. **Run detekt**:

   ```bash
   ./gradlew detekt
   ```

8. **Commit if all green**:

   ```
   chore(deps): bump gradle wrapper from <old> to <new-version>
   ```

9. **If the build fails**, revert `distributionUrl` to the old version URL, report the exact error output, and identify whether it is an AGP floor violation or a Gradle API break in `build-logic/`.

## Reference

### File locations

| What | File | Key |
|------|------|-----|
| Gradle wrapper URL | `gradle/wrapper/gradle-wrapper.properties` | `distributionUrl` |
| AGP version | `gradle/libs.versions.toml` | `agp` in `[versions]` |
| Convention plugins | `build-logic/src/main/kotlin/` | All `*Plugin.kt` files |

### Gradle ↔ AGP compatibility (minimum Gradle per AGP)

| AGP | Minimum Gradle |
|-----|---------------|
| 8.9 | 8.11.1 |
| 8.10 | 8.11.1 |
| 8.11 | 8.11.1 |
| 8.12 | 8.11.1 |
| 8.13 | 8.11.1 |
| 8.14+ | Check https://developer.android.com/build/releases/gradle-plugin#updating-gradle |

Current repo: **AGP 8.13.0**, Gradle **8.13**

Rule: never downgrade Gradle below the minimum required by the current AGP. If a new Gradle version is also paired with a new AGP bump, run `/kamper-migrate-gradle` first, then `/kamper-migrate-agp`.

### Gradle ↔ Kotlin Gradle Plugin compatibility

The Kotlin Gradle Plugin (KGP) declares a minimum Gradle version. For KGP 2.x:

| KGP | Minimum Gradle |
|-----|---------------|
| 2.0.x | 7.6.3 |
| 2.1.x | 7.6.3 |
| 2.2.x | 7.6.3 |
| 2.3.x | 8.3 |

Current repo: **Kotlin 2.3.21** → minimum Gradle 8.3. Any Gradle ≥ 8.3 is safe.

### build-logic API break detection

Gradle regularly deprecates internal APIs. Convention plugins in `build-logic/src/main/kotlin/` that use deprecated Gradle APIs will emit warnings (or failures on `--warning-mode=fail`). Common break sites:

- `Project.convention` removed in Gradle 9
- `BaseExtension` replaced by `CommonExtension` for Android DSL
- `SourceSet.compileClasspath` replaced with variant-aware APIs

If `validatePlugins` or `test` fails after a Gradle bump, inspect the build-logic source files for deprecated API usage and update them before committing.

### distributionUrl format

```properties
# bin variant (no Gradle source): preferred for CI and developer machines
distributionUrl=https\://services.gradle.org/distributions/gradle-8.13-bin.zip

# all variant (includes sources): only needed for IDE Gradle source navigation
distributionUrl=https\://services.gradle.org/distributions/gradle-8.13-all.zip
```

Use `-bin` unless the user explicitly requests `-all`.

### What NOT to change

- Do not edit individual module `build.gradle.kts` files — Gradle wrapper is project-wide.
- Do not run `connectedAndroidTest` or `assembleXCFramework` to verify.
- Do not change `gradle-wrapper.jar` manually — it is updated automatically when the wrapper runs.
