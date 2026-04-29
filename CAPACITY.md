# Capacity & Scaling Limits

Kamper enforces bounded buffers in memory-sensitive subsystems so long-running app sessions cannot
grow without bound. This document lists the known scaling limits per module, the eviction policy
when each limit is reached, and a rough memory ceiling estimate. All limits are **per Kamper instance
in the host process** — the library does not account for multi-process apps that run more than one
Kamper.

All defaults below are configurable; this document covers only the out-of-the-box behaviour.

## IssuesModule — `maxStoredIssues`

| Property | Value |
|----------|-------|
| Configuration field | `IssuesConfig.maxStoredIssues` |
| Default | `200` |
| Eviction policy | FIFO (oldest issue dropped) |
| Drop notification | `IssuesConfig.onDroppedIssue: ((DroppedIssueEvent) -> Unit)?` is invoked once per drop |
| Memory ceiling estimate | `200 × ~1 KB ≈ 200 KB` per IssuesModule instance |

`IssuesWatcher` maintains an `ArrayDeque<Issue>` accumulator capped at `maxStoredIssues`. When the
accumulator is at capacity and a new issue arrives, the oldest issue is removed via `removeFirst()`
and the new issue is appended. The drop is reported through the `onDroppedIssue` callback (if
configured) with a monotonic `totalDropped` counter; the counter resets to `0` on each
`startWatching` call and on `clean()`.

Listener delivery is **not** affected by the cap — every issue is delivered to every registered
`InfoListener<IssueInfo>` regardless of whether it was retained in the internal accumulator.

Tune `maxStoredIssues` higher if your app generates issues in bursts (for example, an ANR storm
during a slow start). Tune lower for memory-constrained devices.

## RecordingManager — `DEFAULT_MAX_RECORDING_SAMPLES`

| Property | Value |
|----------|-------|
| Configuration constant | `DEFAULT_MAX_RECORDING_SAMPLES` (in `RecordingManager.kt`) |
| Default | `4_200` (≈ 10 minutes at 7 metrics/s) |
| Eviction policy | FIFO ring buffer (oldest sample dropped) |
| Drop notification | None — drops are silent |
| Memory ceiling estimate | `4_200 × ~24 B ≈ 100 KB` per RecordingManager instance |
| Configurability | `RecordingManager(maxSamples = N)` constructor argument |

`RecordingManager` records `RecordedSample(timestampNs, trackId, value)` tuples in an
`ArrayDeque<RecordedSample>`. When the buffer is full, `record()` drops the oldest sample before
appending the new one. The buffer is exported as a Perfetto trace via
`exportTrace(): ByteArray` (in-memory) or `exportTraceToFile(out: OutputStream)` (streaming
gzip — preferred for long recordings).

A 10-minute recording at the default sample rate produces a `.perfetto-trace.gz` file of roughly
50 KB on disk after gzip compression of the protobuf wire format.

## CPU performance ring buffer

Phase 5 (CPU Performance Recording Buffer) introduced a ring buffer for CPU sample replay. Capacity
is module-internal and not exposed as a public configuration field at the time of writing.

| Property | Value |
|----------|-------|
| Buffer location | `libs/modules/cpu/src/commonMain/kotlin/...` |
| Eviction policy | FIFO ring buffer |
| Drop notification | None |

See the Phase 5 SUMMARY documents (`.planning/phases/05-*`) for capacity details. The buffer is
sized so that buffering the longest practical CPU monitoring window adds negligible heap pressure;
if your app needs longer replay windows, the implementation can be parameterized in a future phase.

## Per-module memory impact summary

| Module | Worst-case memory ceiling | Notes |
|--------|---------------------------|-------|
| IssuesModule | ~200 KB | `maxStoredIssues × sizeof(Issue)` |
| RecordingManager | ~100 KB | `DEFAULT_MAX_RECORDING_SAMPLES × sizeof(RecordedSample)` |
| CPU ring buffer | < 50 KB | See Phase 5 SUMMARY for exact sizing |
| All other modules | Bounded by listener count | Modules emit Info objects to listeners; no internal accumulator |

Total Kamper steady-state heap usage is dominated by IssuesModule and RecordingManager when both
are enabled at default capacities. Apps targeting low-RAM devices (≤ 1 GB) can tune these caps
down without losing core functionality.

## Adding new bounded subsystems

When future phases introduce new bounded buffers (caches, accumulators, ring buffers), update this
document with: configuration field, default value, eviction policy, drop notification mechanism,
and a memory ceiling estimate. Bounded subsystems without a documented capacity here are
considered defects.
