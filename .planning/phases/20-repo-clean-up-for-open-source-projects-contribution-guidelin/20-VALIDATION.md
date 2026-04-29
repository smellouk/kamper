---
phase: 20
slug: repo-clean-up-for-open-source-projects-contribution-guidelin
status: approved
nyquist_compliant: true
wave_0_complete: true
created: 2026-04-29
---

# Phase 20 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | File existence + grep (documentation-only phase — no test framework) |
| **Config file** | none |
| **Quick run command** | `ls CONTRIBUTING.md .github/ISSUE_TEMPLATE/ .github/PULL_REQUEST_TEMPLATE.md README.md 2>&1` |
| **Full suite command** | `ls CONTRIBUTING.md .github/ISSUE_TEMPLATE/bug-report.yml .github/ISSUE_TEMPLATE/feature-request.yml .github/PULL_REQUEST_TEMPLATE.md README.md && grep -L "maven.pkg.github.com" README.md && echo "PASS"` |
| **Estimated runtime** | ~2 seconds |

---

## Sampling Rate

- **After every task commit:** Run quick run command (file existence check)
- **After every plan wave:** Run full suite command (content verification)
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 2 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 20-01-01 | 01 | 1 | OSS-02 | — | N/A | manual | `ls .github/ISSUE_TEMPLATE/bug-report.yml .github/ISSUE_TEMPLATE/feature-request.yml` | ❌ W0 | ⬜ pending |
| 20-01-02 | 01 | 1 | OSS-02 | — | N/A | manual | `ls .github/PULL_REQUEST_TEMPLATE.md` | ❌ W0 | ⬜ pending |
| 20-02-01 | 02 | 2 | OSS-02 | — | N/A | manual | `ls CONTRIBUTING.md && grep -c "JDK 17" CONTRIBUTING.md` | ❌ W0 | ⬜ pending |
| 20-03-01 | 03 | 3 | OSS-02 | — | N/A | manual | `grep -c "maven.pkg.github.com" README.md \|\| echo "PASS (no stale URLs)"` | ✅ | ⬜ pending |
| 20-03-02 | 03 | 3 | OSS-02 | — | N/A | manual | `! test -f SECURITY.md && echo "PASS (deleted)"` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `.github/ISSUE_TEMPLATE/` directory — create before adding template files
- [ ] Existing `.github/PULL_REQUEST_TEMPLATE.md` — replace (file exists, needs rewrite)

*Wave 0 is minimal: create the ISSUE_TEMPLATE directory before plan 01 tasks run.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| CONTRIBUTING.md prose is clear and complete | OSS-02 | Content quality is subjective | Read CONTRIBUTING.md top-to-bottom; verify all D-01 required sections are present and correct |
| README badge strip renders correctly | OSS-02 | Rendering requires browser/GitHub | Preview README.md on GitHub after push |
| GitHub issue form renders correctly | OSS-02 | GitHub Forms rendering requires GitHub | Create a test issue via GitHub UI to verify form renders |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references (directory creation absorbed into Plan 20-01 Task 1)
- [x] No watch-mode flags
- [x] Feedback latency < 2s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-04-29
