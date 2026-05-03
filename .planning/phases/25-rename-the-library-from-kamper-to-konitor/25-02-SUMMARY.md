---
plan: 25-02
phase: 25-rename-the-library-from-kamper-to-konitor
status: complete
completed: 2026-05-03
---

# Plan 25-02 Summary — Source Directory and Package Rename

## What Was Built

All source directories renamed from `com/smellouk/kamper/` to `com/smellouk/konitor/` across every module in `libs/`, and all package declarations and import statements updated from `com.smellouk.kamper.*` to `com.smellouk.konitor.*` in all `.kt` files.

## Tasks Completed

**Task 1: Rename source directories**
- Renamed 142+ `kamper` → `konitor` source directories across all lib modules using `git mv`
- Engine commonMain dot-separated directory renamed: `com.smellouk.kamper` → `com.smellouk.konitor`
- All standard slash-separated source sets renamed across: `libs/api`, `libs/engine`, `libs/modules/*`, `libs/ui/kmm`, `libs/ui/rn/android`, `libs/integrations/*`, `libs/rn`, `libs/apple-sdk`

**Task 2: Update package declarations and import statements**
- Applied `sed -i '' 's/com\.smellouk\.kamper/com.smellouk.konitor/g'` across all 548 `.kt` files in `libs/`
- Zero `com.smellouk.kamper` package declarations remain
- Zero `com.smellouk.kamper` import statements remain

## Verification Results

- `find libs -type d -name "kamper" | grep -v "/build/" | grep -v "/.cxx/"` → **0 results**
- `find libs -type d -name "konitor" | grep -v "/build/" | grep -v "/.cxx/"` → **142 results**
- `test -d libs/engine/src/commonMain/kotlin/com.smellouk.konitor` → **EXISTS**
- `grep -r "^package com.smellouk.kamper" libs --include="*.kt" | grep -v "/build/"` → **0 matches**
- `grep -r "import com.smellouk.kamper" libs --include="*.kt" | grep -v "/build/"` → **0 matches**

## Self-Check: PASSED

All acceptance criteria met. Source directory rename and package namespace update complete.

## Key Files Modified

- All `.kt` files under `libs/api/src/`, `libs/engine/src/`, `libs/modules/*/src/`, `libs/ui/kmm/src/`, `libs/integrations/*/src/`, `libs/rn/src/`, `libs/apple-sdk/src/`
- Source directories under all of the above (git mv)
