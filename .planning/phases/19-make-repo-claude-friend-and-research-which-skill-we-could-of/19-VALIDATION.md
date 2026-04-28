---
phase: 19
slug: make-repo-claude-friend-and-research-which-skill-we-could-of
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-29
---

# Phase 19 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | File existence checks + grep patterns |
| **Config file** | none — file-based verification |
| **Quick run command** | `ls CLAUDE.md .claude/skills/kamper-new-module/SKILL.md .claude/skills/kamper-check/SKILL.md .claude/skills/kamper-module-review/SKILL.md` |
| **Full suite command** | `ls CLAUDE.md && grep -q "Build & Test" CLAUDE.md && grep -q "Module Pattern" CLAUDE.md` |
| **Estimated runtime** | ~2 seconds |

---

## Sampling Rate

- **After every task commit:** Run `ls CLAUDE.md .claude/skills/`
- **After every plan wave:** Run full grep verification of CLAUDE.md sections
- **Before `/gsd-verify-work`:** All 4 CLAUDE.md sections present + all 3 SKILL.md files exist
- **Max feedback latency:** 5 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 19-01-01 | 01 | 1 | OSS-01 | — | N/A | file-check | `ls CLAUDE.md` | ❌ W0 | ⬜ pending |
| 19-01-02 | 01 | 1 | OSS-01 | — | N/A | grep | `grep -q "Build" CLAUDE.md` | ❌ W0 | ⬜ pending |
| 19-02-01 | 02 | 2 | OSS-01 | — | N/A | file-check | `ls .claude/skills/kamper-new-module/SKILL.md` | ❌ W0 | ⬜ pending |
| 19-02-02 | 02 | 2 | OSS-01 | — | N/A | file-check | `ls .claude/skills/kamper-check/SKILL.md` | ❌ W0 | ⬜ pending |
| 19-02-03 | 02 | 2 | OSS-01 | — | N/A | file-check | `ls .claude/skills/kamper-module-review/SKILL.md` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

*Existing infrastructure covers all phase requirements — this phase creates documentation and skill files, no test framework needed.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| CLAUDE.md accurately describes the repo | OSS-01 | Requires human judgment on accuracy and completeness | Read CLAUDE.md, verify all 4 sections are accurate against actual codebase |
| Skills are discoverable and useful | OSS-01 | Requires human test of skill invocation | Run `/kamper-new-module test` in Claude Code and verify output |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 5s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
