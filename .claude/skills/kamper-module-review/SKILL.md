# kamper-module-review

## Purpose

Reviews a Kamper module against the project's locked conventions — naming patterns, 4-class structure, INVALID sentinel, Builder/DEFAULT pattern, `expect`/`actual` completeness across all 7 platform actual source sets, safety try/catch rule, and test coverage — and emits a pass/fail checklist with file references and a READY / NOT READY verdict.

## When to Use

- User invokes `/kamper-module-review <name>` where `<name>` is the module directory under `libs/modules/`.
- After scaffolding a new module with `/kamper-new-module` and before merging the first PR.
- During code review of any change touching a module's surface (Info, Config, Watcher, Performance, Module.kt expect/actual files).
- When the agent wants to verify that a module is complete before running `./gradlew test`.

## Steps

1. **Validate the module exists** — `libs/modules/<name>/` must be a directory. If not, abort with a clear error.

2. **Walk the four checklist groups** in order (Naming, Completeness, Safety, Testing). For each item:
   - Use `grep`, `ls`, or `cat` to verify presence of the artifact or pattern.
   - Mark each item `PASS`, `FAIL`, or `PARTIAL` with the specific file path inspected.

3. **For the expect/actual completeness check**, verify that each of the 7 platform actual source sets contains `actual val {Name}Module`:
   - `androidMain/kotlin/com/smellouk/kamper/{name}/Module.kt`
   - `iosMain/kotlin/com/smellouk/kamper/{name}/Module.kt`
   - `jvmMain/kotlin/com/smellouk/kamper/{name}/Module.kt`
   - `macosMain/kotlin/com/smellouk/kamper/{name}/Module.kt`
   - `tvosMain/kotlin/com/smellouk/kamper/{name}/Module.kt`
   - `jsMain/kotlin/com/smellouk/kamper/{name}/Module.kt`
   - `wasmJsMain/kotlin/com/smellouk/kamper/{name}/Module.kt`

4. **For the safety check**, search every file under `libs/modules/<name>/src/` for:
   - `while (true)` — FAIL, must be `while (isActive)`
   - `println(` in production source — FAIL, must be `logger.log(...)`
   - `// TODO` or `// FIXME` — FAIL, Detekt treats these as build errors
   - Platform source method bodies calling platform APIs without `try/catch` — FAIL

5. **Run `./gradlew :libs:modules:<name>:jvmTest`** if Gradle commands are in the `.claude/settings.json` allowlist, and report pass/fail. (See `kamper-check` skill for the pre-approved command list.)

6. **Emit a final report** as a markdown checklist with each item marked, a total pass/fail count, and a `READY` / `NOT READY` verdict.

## Reference

### Convention Checklist (mirrors .planning/codebase/CONVENTIONS.md)

#### Naming

- [ ] Module directory: `libs/modules/{name}/`
- [ ] Package: `com.smellouk.kamper.{name}`
- [ ] Info class: `{Name}Info` (data class, implements `Info`)
- [ ] Config class: `{Name}Config` (data class, implements `Config`)
- [ ] Watcher class: `{Name}Watcher` (internal, extends `Watcher<{Name}Info>`)
- [ ] Performance class: `{Name}Performance` (internal, extends `Performance<...>`)

#### Completeness

- [ ] `{Name}Info` companion `INVALID` constant exists
- [ ] `{Name}Config.Builder` object with `build()` function exists
- [ ] `{Name}Config.DEFAULT` companion val exists
- [ ] `expect val {Name}Module` in `commonMain/Module.kt`
- [ ] `actual val {Name}Module` in ALL 7 platform actual source sets:
      `androidMain`, `iosMain`, `jvmMain`, `macosMain`, `tvosMain`, `jsMain`, `wasmJsMain`
- [ ] Registered in `settings.gradle.kts` as `include(":libs:modules:{name}")`

#### Safety

- [ ] All platform calls in `{Name}InfoSource` wrapped in `try/catch`
- [ ] Watcher loop uses `while (isActive)`, not `while (true)`
- [ ] No `println(...)` in production code — uses `logger.log(...)`
- [ ] No `// TODO:` or `// FIXME:` comments (Detekt `ForbiddenComment` rule)

#### Testing

- [ ] `{Name}ConfigBuilderTest` exists in `commonTest`
- [ ] `{Name}InfoMapperTest` exists in `commonTest`
- [ ] Platform source test exists in `androidTest` or `jvmTest`

### Expect/actual source set locations

For a module named `{name}` with PascalCase form `{Name}`, the expect declaration lives at:

```
libs/modules/{name}/src/commonMain/kotlin/com/smellouk/kamper/{name}/Module.kt
```

The 7 actual declarations live at:
```
libs/modules/{name}/src/androidMain/kotlin/com/smellouk/kamper/{name}/Module.kt
libs/modules/{name}/src/iosMain/kotlin/com/smellouk/kamper/{name}/Module.kt
libs/modules/{name}/src/jvmMain/kotlin/com/smellouk/kamper/{name}/Module.kt
libs/modules/{name}/src/macosMain/kotlin/com/smellouk/kamper/{name}/Module.kt
libs/modules/{name}/src/tvosMain/kotlin/com/smellouk/kamper/{name}/Module.kt
libs/modules/{name}/src/jsMain/kotlin/com/smellouk/kamper/{name}/Module.kt
libs/modules/{name}/src/wasmJsMain/kotlin/com/smellouk/kamper/{name}/Module.kt
```

### Source files to read for context

- `.planning/codebase/CONVENTIONS.md` — full naming and code style rules
- `.planning/codebase/ARCHITECTURE.md` — 4-layer model and module pattern
- `libs/modules/cpu/` — canonical reference module; the gold-standard implementation
- `CLAUDE.md` — project-level rules (commit format, build commands, layer model)

### Common findings and fixes

| Finding | Fix |
|---------|-----|
| Missing INVALID sentinel | Add `companion object { val INVALID = {Name}Info(-1.0) }` to the Info class |
| Missing `actual val` on a platform | Add `actual val {Name}Module` in `{platform}Main/Module.kt` |
| `while (true)` in Watcher | Replace with `while (isActive)` and add `import kotlinx.coroutines.isActive` |
| `println(...)` in production | Inject `Logger` via constructor; use `logger.log(...)` instead |
| Platform call without `try/catch` | Wrap in `try { ... } catch (e: Exception) { logger.log(e.stackTraceToString()); return INVALID }` |
| Missing test files | Add `{Name}ConfigBuilderTest.kt` and `{Name}InfoMapperTest.kt` in `commonTest` |
| Not in `settings.gradle.kts` | Add `include(":libs:modules:{name}")` in alphabetical order in the modules block |
