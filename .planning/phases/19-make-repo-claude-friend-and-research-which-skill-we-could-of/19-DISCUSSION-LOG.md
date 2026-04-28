# Phase 19: Claude-Friendly Repo - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-29
**Phase:** 19-make-repo-claude-friend-and-research-which-skill-we-could-of
**Areas discussed:** CLAUDE.md strategy, Skill selection, Skill audience

---

## CLAUDE.md Strategy

| Option | Description | Selected |
|--------|-------------|----------|
| Rich self-contained guide | Inline the essentials: project purpose, module architecture, build commands, Conventional Commits rules, KMP expect/actual pattern, test approach. Agents don't need to explore — everything in one file. | ✓ |
| Thin pointer doc | Short overview + "read these files" section pointing to .planning/codebase/ maps. DRY, lower maintenance, but agents need to follow links. | |
| Two-tier | Rich CLAUDE.md for the repo root, plus per-module sub-CLAUDE.md files. | |

**User's choice:** Rich self-contained guide

---

### CLAUDE.md Sections

| Option | Description | Selected |
|--------|-------------|----------|
| Build & test commands | Canonical Gradle commands, what NOT to run, per-module test commands. | ✓ |
| Module patterns | KMP expect/actual pattern, 8-platform layout, Info/Config/Watcher/Performance hierarchy, Builder pattern, INVALID sentinel. | ✓ |
| Commit & PR rules | Conventional Commits format, no emojis, no resolves #N footers. | ✓ |
| Architecture & key files | Layer diagram, pointer to ADRs, important invariants. | ✓ |

**User's choice:** All four sections

---

## Skill Selection

| Option | Description | Selected |
|--------|-------------|----------|
| kamper-new-module | Scaffolds new metric module: Info/Config/Watcher/Performance, 8 platforms, build.gradle.kts, settings registration, test stubs. | ✓ |
| kamper-check | Right Gradle commands for a given scope; avoids running Android instrumented tests without a device. | ✓ |
| kamper-module-review | Reviews module against Kamper conventions: naming, expect/actual completeness, INVALID sentinel, Builder pattern, test coverage. | ✓ |

**User's choice:** All three skills

---

### kamper-new-module Scope

| Option | Description | Selected |
|--------|-------------|----------|
| Full module skeleton | All 8 platform source sets, build.gradle.kts pre-filled, Info/Config/Watcher/Performance stubs, INVALID sentinel, DEFAULT config, commonTest stubs, settings.gradle.kts registration. Ready to compile on creation. | ✓ |
| Core classes only | Just commonMain classes + one test stub. User manually adds platform actuals and build config. | |
| Interactive (asks per-platform) | Prompts which platforms to include; creates only requested actuals. | |

**User's choice:** Full module skeleton

---

## Skill Audience

| Option | Description | Selected |
|--------|-------------|----------|
| Maintainer (you) — using Claude Code to develop Kamper | Optimized for the maintainer's workflow; external contributors benefit but aren't primary target. | |
| Both equally | CLAUDE.md is contributor-facing and covers what Claude needs for AI-assisted development. Skills work for both humans using Claude and maintainer sessions. | ✓ |

**User's choice:** Both equally

---

### Skill Placement

| Option | Description | Selected |
|--------|-------------|----------|
| .claude/skills/ | Project-local Claude Code skills — automatically picked up when Claude Code runs in this repo. | ✓ |
| .claude/skills/ + documented in CLAUDE.md | Same location but CLAUDE.md explicitly lists available skills. | |

**User's choice:** `.claude/skills/` (project-local)

---

## Claude's Discretion

- Exact wording and prose style of CLAUDE.md sections
- Whether to add a brief "Quick Start" section at the top of CLAUDE.md
- Depth of architecture layer diagram (textual overview is sufficient)

## Deferred Ideas

- CONTRIBUTING.md rewrite — Phase 20 scope
- GitHub issue and PR templates — Phase 20 scope
- Per-module sub-CLAUDE.md files — discussed, deferred; root CLAUDE.md is sufficient
