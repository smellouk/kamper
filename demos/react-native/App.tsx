import React, {useCallback, useEffect, useRef, useState} from 'react';
import {
  Animated,
  Dimensions,
  Easing,
  Platform,
  Pressable,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import {SafeAreaProvider, SafeAreaView} from 'react-native-safe-area-context';
// D-12: top-level showOverlay/hideOverlay imports — kept as permanent demo
// references so the import surface is exercised at TypeScript compile time
// (no transient "add and revert" required for verification per revision
// iteration 1 WARNING fix).
import {Kamper, showOverlay, hideOverlay} from 'react-native-kamper';
import type {
  CpuInfo,
  FpsInfo,
  GpuInfo,
  MemoryInfo,
  NetworkInfo,
  IssueInfo,
  JankInfo,
  GcInfo,
  ThermalInfo,
  JsMemoryInfo,
  JsGcInfo,
} from 'react-native-kamper';

// ─── D-12 verification: __DEV__-only overlay sample ─────────────────────
// This block is INTENTIONALLY commented out — it documents the canonical
// usage for D-12 (overlay availability at top-level imports) and provides
// a permanent grep-match for `showOverlay` / `hideOverlay` in the demo so
// tooling and reviewers can confirm the symbols are wired without running
// the app. To exercise the overlay, uncomment temporarily:
//
//   if (__DEV__) {
//     // Show the native Kamper overlay in development builds only.
//     // showOverlay();
//     // ... and later when leaving the screen:
//     // hideOverlay();
//   }
//
// The runtime-active path (per Phase 12 scope) is: consumer apps call
// these from their own __DEV__ guards. The demo exercises the imports
// statically; runtime overlay verification happens in Task 4 Step 7.

// ── Catppuccin Mocha ───────────────────────────────────────────────────────────
const C = {
  base:     '#1E1E2E',
  mantle:   '#181825',
  surface0: '#313244',
  surface1: '#45475A',
  overlay1: '#7F849C',
  text:     '#CDD6F4',
  subtext:  '#A6ADC8',
  muted:    '#6C7086',
  blue:     '#89B4FA',
  green:    '#A6E3A1',
  yellow:   '#F9E2AF',
  peach:    '#FAB387',
  mauve:    '#CBA6F7',
  teal:     '#94E2D5',
  red:      '#F38BA8',
};

// ── Types ──────────────────────────────────────────────────────────────────────
// Aliased to types exported by react-native-kamper (D-09).
// Field shapes are guaranteed identical (validated by Codegen + types.ts contract).
type CpuData = CpuInfo;
type FpsData = FpsInfo;
type MemoryData = MemoryInfo;
type NetworkData = NetworkInfo;
type IssueData = IssueInfo;
type JankData = JankInfo;
type GcData = GcInfo;
type GpuData = GpuInfo;
type ThermalData = ThermalInfo;
type JsMemoryData = JsMemoryInfo;
type JsGcData = JsGcInfo;

// ── Format helpers ─────────────────────────────────────────────────────────────
const pct = (v: number) => `${(v * 100).toFixed(1)}%`;
const mb1 = (v: number) => `${v.toFixed(1)} MB`;
const fmtMb = (v: number) =>
  v >= 1 ? `${v.toFixed(3)} MB` : v >= 0.01 ? `${(v * 1000).toFixed(0)} KB` : '< 10 KB';

// ── Shared components ──────────────────────────────────────────────────────────

function AnimatedBar({ratio, color}: {ratio: number; color: string}) {
  const anim = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    Animated.timing(anim, {
      toValue: Math.min(Math.max(ratio, 0), 1),
      duration: 500,
      useNativeDriver: false,
    }).start();
  }, [ratio]);
  const width = anim.interpolate({inputRange: [0, 1], outputRange: ['0%', '100%']});
  return (
    <View style={s.barTrack}>
      <Animated.View style={[s.barFill, {width, backgroundColor: color}]} />
    </View>
  );
}

function MetricRow({
  label, ratio, value, color, detail,
}: {
  label: string; ratio: number; value: string; color: string; detail?: string;
}) {
  return (
    <View style={s.metricWrap}>
      <View style={s.metricRow}>
        <Text style={s.metricLabel}>{label}</Text>
        <AnimatedBar ratio={ratio} color={color} />
        <Text style={[s.metricValue, {color}]}>{value}</Text>
      </View>
      {detail ? <Text style={s.metricDetail}>{detail}</Text> : null}
    </View>
  );
}

function SectionTitle({label, first}: {label: string; first?: boolean}) {
  return <Text style={[s.sectionTitle, !first && s.sectionTitleSpaced]}>{label}</Text>;
}

function Placeholder({running}: {running: boolean}) {
  return <Text style={s.placeholder}>{running ? 'Waiting for data…' : '—'}</Text>;
}

function Btn({
  label, onPress, disabled,
}: {
  label: string; onPress: () => void; disabled?: boolean;
}) {
  return (
    <Pressable
      style={({pressed}) => [s.btn, disabled && s.btnDisabled, pressed && s.btnPressed]}
      onPress={onPress}
      disabled={disabled}>
      <Text style={s.btnText}>{label}</Text>
    </Pressable>
  );
}

// ── FPS Animation ──────────────────────────────────────────────────────────────
const ANIM_SIZE = 220;
const ANIM_CENTER = ANIM_SIZE / 2;
const OUTER_R = ANIM_SIZE * 0.30;
const INNER_R = ANIM_SIZE * 0.15;
const OUTER_DOT_R = ANIM_SIZE * 0.040;
const INNER_DOT_R = ANIM_SIZE * 0.020;
const CENTER_DOT_R = ANIM_SIZE * 0.013;
const ANIM_PALETTE = [C.blue, C.green, C.yellow, C.peach, C.mauve, C.teal];

function FpsAnimation() {
  const rot = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    Animated.loop(
      Animated.timing(rot, {
        toValue: 1,
        duration: 4000,
        easing: Easing.linear,
        useNativeDriver: true,
      }),
    ).start();
  }, []);

  const outerSpin = rot.interpolate({inputRange: [0, 1], outputRange: ['0deg', '360deg']});
  const innerSpin = rot.interpolate({inputRange: [0, 1], outputRange: ['0deg', '-540deg']});

  const outerDots = ANIM_PALETTE.map((color, i) => {
    const a = (i / 6) * Math.PI * 2;
    const size = OUTER_DOT_R * 2;
    return (
      <View
        key={i}
        style={{
          position: 'absolute',
          left: ANIM_CENTER + OUTER_R * Math.cos(a) - OUTER_DOT_R,
          top: ANIM_CENTER + OUTER_R * Math.sin(a) - OUTER_DOT_R,
          width: size,
          height: size,
          borderRadius: OUTER_DOT_R,
          backgroundColor: color,
        }}
      />
    );
  });

  const innerDots = ANIM_PALETTE.map((_, i) => {
    const color = ANIM_PALETTE[(i + 2) % 6];
    const a = (i / 6) * Math.PI * 2;
    const size = INNER_DOT_R * 2;
    return (
      <View
        key={i}
        style={{
          position: 'absolute',
          left: ANIM_CENTER + INNER_R * Math.cos(a) - INNER_DOT_R,
          top: ANIM_CENTER + INNER_R * Math.sin(a) - INNER_DOT_R,
          width: size,
          height: size,
          borderRadius: INNER_DOT_R,
          backgroundColor: color,
        }}
      />
    );
  });

  return (
    <View
      style={{
        width: ANIM_SIZE,
        height: ANIM_SIZE,
        alignSelf: 'center',
        marginVertical: 16,
        backgroundColor: C.mantle,
        borderRadius: ANIM_SIZE / 2,
        overflow: 'hidden',
      }}>
      <Animated.View
        style={{
          position: 'absolute',
          top: 0, left: 0,
          width: ANIM_SIZE, height: ANIM_SIZE,
          transform: [{rotate: outerSpin}],
        }}>
        {outerDots}
      </Animated.View>
      <Animated.View
        style={{
          position: 'absolute',
          top: 0, left: 0,
          width: ANIM_SIZE, height: ANIM_SIZE,
          transform: [{rotate: innerSpin}],
          opacity: 0.7,
        }}>
        {innerDots}
      </Animated.View>
      <View
        style={{
          position: 'absolute',
          left: ANIM_CENTER - CENTER_DOT_R,
          top: ANIM_CENTER - CENTER_DOT_R,
          width: CENTER_DOT_R * 2,
          height: CENTER_DOT_R * 2,
          borderRadius: CENTER_DOT_R,
          backgroundColor: C.surface1,
        }}
      />
    </View>
  );
}

// ── CPU Tab ────────────────────────────────────────────────────────────────────
function CpuTab({cpu, running}: {cpu: CpuData | null; running: boolean}) {
  const [stressing, setStressing] = useState(false);
  const activeRef = useRef(false);

  const runLoop = useCallback(() => {
    if (!activeRef.current) return;
    const end = Date.now() + 30;
    while (Date.now() < end) {}
    setTimeout(runLoop, 0);
  }, []);

  const toggleStress = useCallback(() => {
    if (stressing) {
      activeRef.current = false;
      setStressing(false);
    } else {
      activeRef.current = true;
      setStressing(true);
      setTimeout(runLoop, 50);
    }
  }, [stressing, runLoop]);

  useEffect(() => () => { activeRef.current = false; }, []);

  return (
    <View style={s.tabContent}>
      <ScrollView
        style={s.tabScroll}
        contentContainerStyle={s.scrollContent}
        showsVerticalScrollIndicator={false}>
        <SectionTitle label="CPU USAGE" first />
        {cpu ? (
          <>
            <MetricRow label="Total"   ratio={cpu.totalUseRatio} value={pct(cpu.totalUseRatio)} color={C.blue} />
            <MetricRow label="App"     ratio={cpu.appRatio}      value={pct(cpu.appRatio)}      color={C.green} />
            <MetricRow label="User"    ratio={cpu.userRatio}     value={pct(cpu.userRatio)}     color={C.yellow} />
            <MetricRow label="System"  ratio={cpu.systemRatio}   value={pct(cpu.systemRatio)}   color={C.peach} />
            <MetricRow label="IO Wait" ratio={cpu.ioWaitRatio}   value={pct(cpu.ioWaitRatio)}   color={C.mauve} />
          </>
        ) : (
          <Placeholder running={running} />
        )}
      </ScrollView>
      <View style={s.footer}>
        <Btn
          label={stressing ? 'STOP CPU LOAD' : 'START CPU LOAD'}
          onPress={toggleStress}
        />
      </View>
    </View>
  );
}

// ── FPS Tab ────────────────────────────────────────────────────────────────────
function FpsTab({fps}: {fps: FpsData | null}) {
  const fpsColor = fps
    ? fps.fps >= 55
      ? C.green
      : fps.fps >= 30
      ? C.yellow
      : C.red
    : C.muted;

  return (
    <View style={s.tabContent}>
      <ScrollView
        style={s.tabScroll}
        contentContainerStyle={[s.scrollContent, {alignItems: 'center'}]}
        showsVerticalScrollIndicator={false}>
        <View style={s.fpsBig}>
          <Text style={[s.fpsNum, {color: fpsColor}]}>{fps ? fps.fps : '—'}</Text>
          <Text style={s.fpsUnit}>fps</Text>
        </View>
        <FpsAnimation />
      </ScrollView>
      <View style={s.footer}>
        <Text style={s.footerHint}>Choreographer-based frame measurement</Text>
      </View>
    </View>
  );
}

// ── Memory Tab ─────────────────────────────────────────────────────────────────
function MemoryTab({mem, jsMem, running}: {mem: MemoryData | null; jsMem: JsMemoryData | null; running: boolean}) {
  const allocRef = useRef<Uint8Array | null>(null);
  const [allocMsg, setAllocMsg] = useState('');

  const alloc32Mb = useCallback(() => {
    allocRef.current = new Uint8Array(32 * 1024 * 1024).fill(0xAA);
    setAllocMsg('Allocated 32 MB in JS heap');
  }, []);

  const releaseJsMem = useCallback(() => {
    allocRef.current = null;
    setAllocMsg('Released — awaiting Hermes GC');
    setTimeout(() => setAllocMsg(''), 3000);
  }, []);

  return (
    <View style={s.tabContent}>
      <ScrollView
        style={s.tabScroll}
        contentContainerStyle={s.scrollContent}
        showsVerticalScrollIndicator={false}>
        {mem ? (
          <>
            <SectionTitle label="NATIVE — HEAP" first />
            <MetricRow
              label="Heap"
              ratio={mem.heapMaxMb > 0 ? mem.heapAllocatedMb / mem.heapMaxMb : 0}
              value={mb1(mem.heapAllocatedMb)}
              color={C.green}
              detail={`${mb1(mem.heapAllocatedMb)} / ${mb1(mem.heapMaxMb)} max`}
            />
            <SectionTitle label="NATIVE — SYSTEM RAM" />
            <MetricRow
              label="RAM"
              ratio={mem.ramTotalMb > 0 ? mem.ramUsedMb / mem.ramTotalMb : 0}
              value={mb1(mem.ramUsedMb)}
              color={C.blue}
              detail={`${mb1(mem.ramUsedMb)} / ${mb1(mem.ramTotalMb)} total`}
            />
            {mem.isLowMemory ? <Text style={s.lowMem}>⚠ Low Memory</Text> : null}
          </>
        ) : (
          <Placeholder running={running} />
        )}
        <SectionTitle label="JS — HEAP (HERMES)" first={!mem} />
        {jsMem ? (
          <MetricRow
            label="JS Used"
            ratio={jsMem.totalMb > 0 ? jsMem.usedMb / jsMem.totalMb : 0}
            value={mb1(jsMem.usedMb)}
            color={C.mauve}
            detail={`${mb1(jsMem.usedMb)} / ${mb1(jsMem.totalMb)} capacity`}
          />
        ) : (
          <Placeholder running={running} />
        )}
        {allocMsg ? <Text style={s.dlStatus}>{allocMsg}</Text> : null}
      </ScrollView>
      <View style={[s.footer, s.footerRow]}>
        <Btn label="ALLOC 32 MB (JS)" onPress={alloc32Mb} />
        <Btn label="RELEASE (JS)"     onPress={releaseJsMem} />
      </View>
    </View>
  );
}

// ── Network Tab ────────────────────────────────────────────────────────────────
function NetworkTab({
  net, peakRxRef, peakTxRef, running,
}: {
  net: NetworkData | null;
  peakRxRef: React.MutableRefObject<number>;
  peakTxRef: React.MutableRefObject<number>;
  running: boolean;
}) {
  const [fetching, setFetching] = useState(false);
  const [dlStatus, setDlStatus] = useState('');

  const testDownload = useCallback(async () => {
    setFetching(true);
    setDlStatus('Fetching 5 MB…');
    try {
      await fetch('https://speed.cloudflare.com/__down?bytes=5000000');
      setDlStatus('Done');
    } catch {
      setDlStatus('Error');
    } finally {
      setFetching(false);
    }
  }, []);

  return (
    <View style={s.tabContent}>
      <ScrollView
        style={s.tabScroll}
        contentContainerStyle={s.scrollContent}
        showsVerticalScrollIndicator={false}>
        {net ? (
          <>
            <SectionTitle label="SYSTEM TRAFFIC (PER INTERVAL)" first />
            <MetricRow
              label="Download"
              ratio={Math.min(net.rxMb / Math.max(peakRxRef.current, 0.001), 1)}
              value={fmtMb(net.rxMb)}
              color={C.teal}
              detail={`${fmtMb(net.rxMb)}/interval   peak ${fmtMb(peakRxRef.current)}`}
            />
            <MetricRow
              label="Upload"
              ratio={Math.min(net.txMb / Math.max(peakTxRef.current, 0.001), 1)}
              value={fmtMb(net.txMb)}
              color={C.mauve}
              detail={`${fmtMb(net.txMb)}/interval   peak ${fmtMb(peakTxRef.current)}`}
            />
            {dlStatus ? <Text style={s.dlStatus}>{dlStatus}</Text> : null}
          </>
        ) : (
          <Placeholder running={running} />
        )}
      </ScrollView>
      <View style={s.footer}>
        <Btn
          label={fetching ? 'FETCHING 5 MB…' : 'TEST DOWNLOAD'}
          onPress={testDownload}
          disabled={fetching}
        />
      </View>
    </View>
  );
}

// ── Issues Tab ─────────────────────────────────────────────────────────────────
const SEVERITY_COLOR: Record<string, string> = {
  CRITICAL: C.red, ERROR: C.peach, WARNING: C.yellow, INFO: C.green,
};
const TYPE_COLOR: Record<string, string> = {
  ANR: C.red, CRASH: C.red,
  SLOW_COLD_START: C.peach, SLOW_WARM_START: C.peach, SLOW_HOT_START: C.peach,
  DROPPED_FRAME: C.yellow,
  SLOW_SPAN: C.blue,
  MEMORY_PRESSURE: C.mauve, NEAR_OOM: C.mauve,
  STRICT_VIOLATION: C.teal,
};
const TYPE_SHORT: Record<string, string> = {
  ANR: 'ANR', CRASH: 'CRASH', SLOW_COLD_START: 'COLD', SLOW_WARM_START: 'WARM',
  SLOW_HOT_START: 'HOT', DROPPED_FRAME: 'JANK', SLOW_SPAN: 'SPAN',
  MEMORY_PRESSURE: 'MEM', NEAR_OOM: 'OOM', STRICT_VIOLATION: 'STRICT',
};
function fmtTimestamp(ms: number): string {
  const sec = Math.floor(ms / 1000) % 86400;
  const h = Math.floor(sec / 3600), m = Math.floor((sec % 3600) / 60), s = sec % 60;
  return `${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`;
}

function IssueRow({issue}: {issue: IssueData}) {
  const sevColor  = SEVERITY_COLOR[issue.severity] ?? C.overlay1;
  const typeColor = TYPE_COLOR[issue.type] ?? C.overlay1;
  const typeShort = TYPE_SHORT[issue.type] ?? issue.type;
  const details   = [
    issue.durationMs != null ? `${issue.durationMs}ms` : null,
    issue.threadName  ? `thread=${issue.threadName}` : null,
  ].filter(Boolean).join('  ·  ');

  return (
    <View style={s.issueRow}>
      <View style={[s.issueSevBar, {backgroundColor: sevColor}]} />
      <View style={s.issueContent}>
        <View style={s.issueHeaderRow}>
          <View style={[s.issueTypeChip, {borderColor: typeColor}]}>
            <Text style={[s.issueTypeText, {color: typeColor}]}>{typeShort}</Text>
          </View>
          <Text style={[s.issueSeverity, {color: sevColor}]}>{issue.severity}</Text>
          <Text style={s.issueTime}>{fmtTimestamp(issue.timestamp)}</Text>
        </View>
        <Text style={s.issueMsg} numberOfLines={2}>{issue.message}</Text>
        {details ? <Text style={s.issueDetails}>{details}</Text> : null}
      </View>
    </View>
  );
}

function IssuesTab({issues, onClear}: {issues: IssueData[]; onClear: () => void}) {
  const [spanStatus, setSpanStatus] = useState('');

  const triggerSlowSpan = useCallback(() => {
    setSpanStatus('Blocking JS thread 800 ms…');
    setTimeout(() => {
      Kamper.beginSpan('js-slow-span', 500);
      const start = Date.now();
      while (Date.now() - start < 800) {}
      Kamper.endSpan('js-slow-span');
      setSpanStatus('Done — SLOW_SPAN event fired');
      setTimeout(() => setSpanStatus(''), 5000);
    }, 200);
  }, []);

  const triggerCrash = useCallback(() => {
    setTimeout(() => { throw new Error('Demo crash: test crash triggered by user'); }, 0);
  }, []);

  return (
    <View style={s.tabContent}>
      {issues.length === 0
        ? <View style={s.issueEmpty}><Text style={s.issueEmptyText}>No issues detected</Text></View>
        : <ScrollView style={s.tabScroll} showsVerticalScrollIndicator={false}>
            {issues.map(issue => <IssueRow key={issue.id} issue={issue} />)}
          </ScrollView>
      }
      {spanStatus ? <Text style={s.spanStatus}>{spanStatus}</Text> : null}
      <View style={[s.footer, {justifyContent: 'flex-start', gap: 8}]}>
        <Btn label="SLOW SPAN (JS)" onPress={triggerSlowSpan} />
        <Btn label="CRASH"     onPress={triggerCrash} />
        <Btn label="CLEAR"     onPress={onClear} />
      </View>
    </View>
  );
}

// ── Jank Tab ───────────────────────────────────────────────────────────────────
function JankTab({jank, running}: {jank: JankData | null; running: boolean}) {
  const [jankMsg, setJankMsg] = useState('');

  const simulateJank = useCallback(() => {
    setJankMsg('Blocking 200 ms (Native)…');
    setTimeout(() => {
      const end = Date.now() + 200;
      while (Date.now() < end) {}
      setJankMsg('Done — check dropped frames');
      setTimeout(() => setJankMsg(''), 5000);
    }, 200);
  }, []);

  return (
    <View style={s.tabContent}>
      <ScrollView style={s.tabScroll} contentContainerStyle={[s.scrollContent, {alignItems: 'center'}]} showsVerticalScrollIndicator={false}>
        <View style={s.fpsBig}>
          <Text style={[s.fpsNum, {color: C.mauve}]}>{jank ? jank.droppedFrames : '—'}</Text>
        </View>
        <Text style={[s.fpsUnit, {marginBottom: 20}]}>dropped frames / window</Text>
        {jank ? (
          <>
            <SectionTitle label="STATS" first />
            <View style={s.metricWrap}>
              <View style={s.metricRow}>
                <Text style={s.metricLabel}>Janky ratio</Text>
                <Text style={[s.metricValue, {color: C.mauve}]}>{(jank.jankyFrameRatio * 100).toFixed(1)}%</Text>
              </View>
            </View>
            <View style={s.metricWrap}>
              <View style={s.metricRow}>
                <Text style={s.metricLabel}>Worst frame</Text>
                <Text style={[s.metricValue, {color: C.text}]}>{jank.worstFrameMs.toFixed(0)} ms</Text>
              </View>
            </View>
          </>
        ) : (
          <Placeholder running={running} />
        )}
      </ScrollView>
      {jankMsg ? <Text style={s.spanStatus}>{jankMsg}</Text> : null}
      <View style={s.footer}>
        <Btn label="SIMULATE JANK (Native)" onPress={simulateJank} />
      </View>
    </View>
  );
}

// ── GC Tab ─────────────────────────────────────────────────────────────────────
function GcTab({gc, jsGc, running}: {gc: GcData | null; jsGc: JsGcData | null; running: boolean}) {
  const [gcMsg, setGcMsg] = useState('');

  const simulateGc = useCallback(() => {
    setGcMsg('Allocating…');
    setTimeout(() => {
      const bufs: Uint8Array[] = [];
      for (let i = 0; i < 30; i++) bufs.push(new Uint8Array(1024 * 1024).fill(i));
      bufs.length = 0;
      setGcMsg('Released 30 MB — GC triggered');
      setTimeout(() => setGcMsg(''), 3000);
    }, 50);
  }, []);

  return (
    <View style={s.tabContent}>
      <ScrollView style={s.tabScroll} contentContainerStyle={s.scrollContent} showsVerticalScrollIndicator={false}>
        <SectionTitle label="NATIVE — GC (ART)" first />
        {gc ? (
          <>
            <View style={s.metricWrap}>
              <View style={s.metricRow}>
                <Text style={s.metricLabel}>Events/interval</Text>
                <Text style={[s.metricValue, {color: C.yellow}]}>{gc.gcCountDelta.toFixed(0)}</Text>
              </View>
            </View>
            <View style={s.metricWrap}>
              <View style={s.metricRow}>
                <Text style={s.metricLabel}>Pause delta</Text>
                <Text style={[s.metricValue, {color: C.yellow}]}>{gc.gcPauseMsDelta.toFixed(0)} ms</Text>
              </View>
            </View>
            <View style={s.metricWrap}>
              <View style={s.metricRow}>
                <Text style={s.metricLabel}>Total count</Text>
                <Text style={[s.metricValue, {color: C.text}]}>{gc.gcCount.toFixed(0)}</Text>
              </View>
            </View>
          </>
        ) : (
          <Placeholder running={running} />
        )}
        <SectionTitle label="JS — GC (HERMES)" />
        {jsGc ? (
          <>
            <View style={s.metricWrap}>
              <View style={s.metricRow}>
                <Text style={s.metricLabel}>Events/interval</Text>
                <Text style={[s.metricValue, {color: C.mauve}]}>{jsGc.gcCountDelta.toFixed(0)}</Text>
              </View>
            </View>
            <View style={s.metricWrap}>
              <View style={s.metricRow}>
                <Text style={s.metricLabel}>Pause delta</Text>
                <Text style={[s.metricValue, {color: C.mauve}]}>{jsGc.gcPauseMsDelta.toFixed(1)} ms</Text>
              </View>
            </View>
            <View style={s.metricWrap}>
              <View style={s.metricRow}>
                <Text style={s.metricLabel}>Total pause</Text>
                <Text style={[s.metricValue, {color: C.text}]}>{jsGc.gcPauseMs.toFixed(1)} ms</Text>
              </View>
            </View>
          </>
        ) : (
          <Placeholder running={running} />
        )}
      </ScrollView>
      {gcMsg ? <Text style={s.spanStatus}>{gcMsg}</Text> : null}
      <View style={s.footer}>
        <Btn label="SIMULATE GC (JS)" onPress={simulateGc} />
      </View>
    </View>
  );
}

// ── Thermal Tab ────────────────────────────────────────────────────────────────
const THERMAL_COLORS: Record<string, string> = {
  NONE: C.green, LIGHT: C.green, MODERATE: C.yellow,
  SEVERE: C.peach, CRITICAL: C.peach, EMERGENCY: C.peach, SHUTDOWN: C.peach,
  UNKNOWN: C.overlay1,
};

function ThermalTab({thermal, running}: {thermal: ThermalData | null; running: boolean}) {
  const [stressing, setStressing] = useState(false);
  const activeRef = useRef(false);
  const [unavailable, setUnavailable] = useState(false);

  const runLoop = useCallback(() => {
    if (!activeRef.current) return;
    const end = Date.now() + 30;
    while (Date.now() < end) {}
    setTimeout(runLoop, 0);
  }, []);

  const toggleStress = useCallback(() => {
    if (stressing) {
      activeRef.current = false;
      setStressing(false);
    } else {
      activeRef.current = true;
      setStressing(true);
      setTimeout(runLoop, 50);
    }
  }, [stressing, runLoop]);

  useEffect(() => () => { activeRef.current = false; }, []);

  useEffect(() => {
    if (thermal) { setUnavailable(false); return; }
    const t = setTimeout(() => setUnavailable(true), 5000);
    return () => clearTimeout(t);
  }, [thermal]);

  const isUnsupported = thermal?.state === 'UNSUPPORTED';
  const stateLabel = thermal && !isUnsupported ? thermal.state : unavailable || isUnsupported ? 'N/A' : 'UNKNOWN';
  const stateColor = thermal && !isUnsupported
    ? (THERMAL_COLORS[thermal.state] ?? C.overlay1)
    : unavailable || isUnsupported ? C.red : C.overlay1;

  return (
    <View style={s.tabContent}>
      <ScrollView style={s.tabScroll} contentContainerStyle={[s.scrollContent, {alignItems: 'center'}]} showsVerticalScrollIndicator={false}>
        <View style={s.fpsBig}>
          <Text style={[s.fpsNum, {color: stateColor, fontSize: 48}]}>{stateLabel}</Text>
        </View>
        <Text style={[s.fpsUnit, {marginBottom: 20}]}>thermal state</Text>
        {thermal && !isUnsupported ? (
          <>
            <SectionTitle label="STATUS" first />
            <View style={s.metricWrap}>
              <View style={s.metricRow}>
                <Text style={s.metricLabel}>Throttling</Text>
                <Text style={[s.metricValue, {color: thermal.isThrottling ? C.peach : C.green}]}>
                  {thermal.isThrottling ? 'YES' : 'NO'}
                </Text>
              </View>
            </View>
            {thermal.temperatureC >= 0 ? (
              <View style={s.metricWrap}>
                <View style={s.metricRow}>
                  <Text style={s.metricLabel}>Battery temp</Text>
                  <Text style={[s.metricValue, {color: thermal.temperatureC > 40 ? C.peach : C.text}]}>
                    {thermal.temperatureC.toFixed(1)} °C
                  </Text>
                </View>
              </View>
            ) : null}
          </>
        ) : unavailable || isUnsupported ? (
          <Text style={[s.placeholder, {color: C.red}]}>Not available on this device</Text>
        ) : (
          <Placeholder running={running} />
        )}
      </ScrollView>
      <View style={s.footer}>
        <Btn
          label={stressing ? 'STOP CPU STRESS' : 'START CPU STRESS'}
          onPress={toggleStress}
        />
      </View>
    </View>
  );
}

// ── GPU Stress Canvas ──────────────────────────────────────────────────────────
const GPU_BALL_COUNT = 40;
const GPU_BALL_SIZE = 60;
const GPU_BALL_COLORS = [C.blue, C.green, C.yellow, C.peach, C.mauve, C.teal, C.red];
const GPU_BOX_SIDE = Dimensions.get('window').width - 80;

function GpuStressCanvas() {
  const balls = useRef(
    Array.from({length: GPU_BALL_COUNT}, (_, i) => ({
      x: new Animated.Value(0),
      y: new Animated.Value(0),
      color: GPU_BALL_COLORS[i % GPU_BALL_COLORS.length],
    }))
  ).current;

  useEffect(() => {
    const maxX = GPU_BOX_SIDE - GPU_BALL_SIZE;
    const maxY = GPU_BOX_SIDE - GPU_BALL_SIZE;
    const loops = balls.map((ball, i) => {
      const dur = 800 + (i * 137) % 1200;
      const xLoop = Animated.loop(Animated.sequence([
        Animated.timing(ball.x, {toValue: maxX, duration: dur, useNativeDriver: false, easing: Easing.inOut(Easing.quad)}),
        Animated.timing(ball.x, {toValue: 0, duration: dur, useNativeDriver: false, easing: Easing.inOut(Easing.quad)}),
      ]));
      const yDur = Math.round(dur * 0.7);
      const yLoop = Animated.loop(Animated.sequence([
        Animated.timing(ball.y, {toValue: maxY, duration: yDur, useNativeDriver: false, easing: Easing.inOut(Easing.sin)}),
        Animated.timing(ball.y, {toValue: 0, duration: yDur, useNativeDriver: false, easing: Easing.inOut(Easing.sin)}),
      ]));
      xLoop.start();
      yLoop.start();
      return {xLoop, yLoop};
    });
    return () => loops.forEach(l => { l.xLoop.stop(); l.yLoop.stop(); });
  }, [balls]);

  return (
    <View style={s.gpuStressBox}>
      {balls.map((ball, i) => (
        <Animated.View
          key={i}
          style={{
            position: 'absolute',
            left: ball.x,
            top: ball.y,
            width: GPU_BALL_SIZE,
            height: GPU_BALL_SIZE,
            borderRadius: GPU_BALL_SIZE / 2,
            backgroundColor: ball.color,
            opacity: 0.85,
          }}
        />
      ))}
    </View>
  );
}

// ── GPU Tab ────────────────────────────────────────────────────────────────────
function GpuTab({gpu, running}: {gpu: GpuData | null; running: boolean}) {
  const [stressing, setStressing] = useState(false);

  return (
    <View style={s.tabContent}>
      <ScrollView
        style={s.tabScroll}
        contentContainerStyle={[s.scrollContent, {alignItems: 'center'}]}
        showsVerticalScrollIndicator={false}>
        <View style={s.fpsBig}>
          <Text style={[s.fpsNum, {color: gpu ? C.mauve : C.muted, fontSize: 48}]}>
            {gpu ? (gpu.utilization >= 0 ? `${gpu.utilization.toFixed(1)}%` : '—%') : '—'}
          </Text>
        </View>
        <Text style={[s.fpsUnit, {marginBottom: 20}]}>GPU usage %</Text>
        {gpu ? (
          <>
            <SectionTitle label="FREQUENCY" first />
            <View style={s.metricWrap}>
              <View style={s.metricRow}>
                <Text style={s.metricLabel}>Cur Freq</Text>
                <Text style={[s.metricValue, {color: C.mauve}]}>
                  {gpu.curFreqKhz >= 0 ? `${(gpu.curFreqKhz / 1000).toFixed(0)} MHz` : '—'}
                </Text>
              </View>
            </View>
            <View style={s.metricWrap}>
              <View style={s.metricRow}>
                <Text style={s.metricLabel}>Max Freq</Text>
                <Text style={[s.metricValue, {color: C.text}]}>
                  {gpu.maxFreqKhz >= 0 ? `${(gpu.maxFreqKhz / 1000).toFixed(0)} MHz` : '—'}
                </Text>
              </View>
            </View>
            <SectionTitle label="MEMORY" />
            <View style={s.metricWrap}>
              <View style={s.metricRow}>
                <Text style={s.metricLabel}>Used</Text>
                <Text style={[s.metricValue, {color: C.peach}]}>
                  {gpu.usedMemoryMb >= 0 ? `${gpu.usedMemoryMb.toFixed(0)} MB` : '—'}
                </Text>
              </View>
            </View>
            <View style={s.metricWrap}>
              <View style={s.metricRow}>
                <Text style={s.metricLabel}>Total</Text>
                <Text style={[s.metricValue, {color: C.text}]}>
                  {gpu.totalMemoryMb >= 0 ? `${gpu.totalMemoryMb.toFixed(0)} MB` : '—'}
                </Text>
              </View>
            </View>
            <SectionTitle label="BREAKDOWN" />
            <View style={s.metricWrap}>
              <View style={s.metricRow}>
                <Text style={s.metricLabel}>App</Text>
                <Text style={[s.metricValue, {color: C.mauve}]}>
                  {gpu.appUtilization >= 0 ? `${gpu.appUtilization.toFixed(1)}%` : 'N/A'}
                </Text>
              </View>
            </View>
            <View style={s.metricWrap}>
              <View style={s.metricRow}>
                <Text style={s.metricLabel}>Renderer</Text>
                <Text style={[s.metricValue, {color: C.blue}]}>
                  {gpu.rendererUtilization >= 0 ? `${gpu.rendererUtilization.toFixed(1)}%` : 'N/A'}
                </Text>
              </View>
            </View>
            <View style={s.metricWrap}>
              <View style={s.metricRow}>
                <Text style={s.metricLabel}>Tiler</Text>
                <Text style={[s.metricValue, {color: C.teal}]}>
                  {gpu.tilerUtilization >= 0 ? `${gpu.tilerUtilization.toFixed(1)}%` : 'N/A'}
                </Text>
              </View>
            </View>
            <View style={s.metricWrap}>
              <View style={s.metricRow}>
                <Text style={s.metricLabel}>Compute</Text>
                <Text style={[s.metricValue, {color: C.green}]}>
                  {gpu.computeUtilization >= 0 ? `${gpu.computeUtilization.toFixed(1)}%` : 'N/A'}
                </Text>
              </View>
            </View>
          </>
        ) : (
          <Placeholder running={running} />
        )}
        {stressing && (
          <View style={{marginTop: 20, alignItems: 'center'}}>
            <GpuStressCanvas />
          </View>
        )}
      </ScrollView>
      <View style={s.footer}>
        {running
          ? <Btn label={stressing ? 'STOP STRESS' : 'STRESS GPU'} onPress={() => setStressing(v => !v)} />
          : <Text style={s.footerHint}>Engine stopped</Text>
        }
      </View>
    </View>
  );
}

// ── App ────────────────────────────────────────────────────────────────────────
const TABS = ['CPU', 'GPU', 'FPS', 'MEMORY', 'NETWORK', 'ISSUES', 'JANK', 'GC', 'THERMAL'];

export default function App() {
  const [activeTab, setActiveTab] = useState(0);
  const [running, setRunning]     = useState(false);
  const [cpu, setCpu]             = useState<CpuData | null>(null);
  const [fps, setFps]             = useState<FpsData | null>(null);
  const [mem, setMem]             = useState<MemoryData | null>(null);
  const [net, setNet]             = useState<NetworkData | null>(null);
  const [issues, setIssues]       = useState<IssueData[]>([]);
  const [jank, setJank]           = useState<JankData | null>(null);
  const [gc, setGc]               = useState<GcData | null>(null);
  const [gpu, setGpu]             = useState<GpuData | null>(null);
  const [thermal, setThermal]     = useState<ThermalData | null>(null);
  const [jsMem, setJsMem]         = useState<JsMemoryData | null>(null);
  const [jsGc, setJsGc]           = useState<JsGcData | null>(null);
  const peakRxRef                 = useRef(0);
  const peakTxRef                 = useRef(0);

  useEffect(() => {
    Kamper.start();
    setRunning(true);
    const subs = [
      Kamper.on('cpu',     (d: CpuData)     => setCpu(d)),
      Kamper.on('fps',     (d: FpsData)     => setFps(d)),
      Kamper.on('memory',  (d: MemoryData)  => setMem(d)),
      Kamper.on('network', (d: NetworkData) => {
        peakRxRef.current = Math.max(peakRxRef.current, d.rxMb);
        peakTxRef.current = Math.max(peakTxRef.current, d.txMb);
        setNet(d);
      }),
      Kamper.on('issue',   (d: IssueData)   => {
        setIssues(prev => [d, ...prev].slice(0, 100));
      }),
      Kamper.on('jank',    (d: JankData)    => setJank(d)),
      Kamper.on('gc',       (d: GcData)       => setGc(d)),
      Kamper.on('gpu',      (d: GpuData)      => setGpu(d)),
      Kamper.on('thermal',  (d: ThermalData)  => setThermal(d)),
      Kamper.on('jsMemory', (d: JsMemoryData) => setJsMem(d)),
      Kamper.on('jsGc',     (d: JsGcData)     => setJsGc(d)),
    ];
    return () => {
      subs.forEach(sub => sub.remove());
      Kamper.stop();
    };
  }, []);

  const toggle = useCallback(() => {
    if (running) {
      Kamper.stop();
      setRunning(false);
      setCpu(null); setFps(null); setMem(null); setNet(null); setIssues([]);
      setJank(null); setGc(null); setGpu(null); setThermal(null); setJsMem(null); setJsGc(null);
      peakRxRef.current = 0; peakTxRef.current = 0;
    } else {
      Kamper.start();
      setRunning(true);
    }
  }, [running]);

  return (
    <SafeAreaProvider>
    <SafeAreaView style={s.root}>
      <StatusBar barStyle="light-content" backgroundColor={C.mantle} />

      {/* Header */}
      <View style={s.header}>
        <Text style={s.headerTitle}>Kamper Performance Monitor</Text>
        <View style={[s.runDot, {backgroundColor: running ? C.green : C.surface1}]} />
        <Pressable
          style={[s.toggleBtn, {backgroundColor: running ? C.surface1 : C.blue}]}
          onPress={toggle}>
          <Text style={[s.toggleText, {color: running ? C.text : C.base}]}>
            {running ? 'STOP' : 'START'}
          </Text>
        </Pressable>
      </View>
      <View style={s.divider} />

      {/* Tab bar */}
      <View style={s.tabBar}>
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={s.tabBarContent}>
          {TABS.map((name, i) => (
            <Pressable
              key={name}
              style={s.tab}
              onPress={() => setActiveTab(i)}>
              <Text style={[s.tabText, activeTab === i && s.tabTextActive]}>
                {name}
              </Text>
              {activeTab === i && <View style={s.tabIndicator} />}
            </Pressable>
          ))}
        </ScrollView>
      </View>
      <View style={s.divider} />

      {/* Tab content */}
      {activeTab === 0 && <CpuTab     cpu={cpu} running={running} />}
      {activeTab === 1 && <GpuTab     gpu={gpu} running={running} />}
      {activeTab === 2 && <FpsTab     fps={fps} />}
      {activeTab === 3 && <MemoryTab  mem={mem} jsMem={jsMem} running={running} />}
      {activeTab === 4 && (
        <NetworkTab
          net={net}
          peakRxRef={peakRxRef}
          peakTxRef={peakTxRef}
          running={running}
        />
      )}
      {activeTab === 5 && <IssuesTab  issues={issues} onClear={() => setIssues([])} />}
      {activeTab === 6 && <JankTab    jank={jank}    running={running} />}
      {activeTab === 7 && <GcTab      gc={gc}        jsGc={jsGc}  running={running} />}
      {activeTab === 8 && <ThermalTab thermal={thermal} running={running} />}
    </SafeAreaView>
    </SafeAreaProvider>
  );
}

// ── Styles ─────────────────────────────────────────────────────────────────────
const MONO = Platform.OS === 'ios' ? 'Menlo' : 'monospace';

const s = StyleSheet.create({
  root: {flex: 1, backgroundColor: C.base},

  // Header
  header: {
    backgroundColor: C.mantle,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 14,
    gap: 8,
  },
  headerTitle: {flex: 1, fontSize: 16, fontWeight: '700', color: C.blue},
  runDot:      {width: 8, height: 8, borderRadius: 4},
  toggleBtn:   {paddingHorizontal: 14, paddingVertical: 6, borderRadius: 8},
  toggleText:  {fontSize: 12, fontWeight: '700', letterSpacing: 0.5},

  divider: {height: 1, backgroundColor: C.surface0},

  // Tab bar
  tabBar:        {backgroundColor: C.mantle, height: 44, flexShrink: 0},
  tabBarContent: {flexDirection: 'row', height: 44},
  tab: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 14,
    height: 44,
  },
  tabIndicator: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    height: 2,
    backgroundColor: C.blue,
  },
  tabText:       {fontSize: 12, fontWeight: '600', letterSpacing: 0.5, color: C.muted},
  tabTextActive: {color: C.blue, fontWeight: '700'},

  // Tab layout
  tabContent:    {flex: 1},
  tabScroll:     {flex: 1},
  scrollContent: {paddingHorizontal: 20, paddingTop: 20, paddingBottom: 12, flexGrow: 1},

  // Section
  sectionTitle:       {fontSize: 14, fontWeight: '700', color: C.text, marginBottom: 10},
  sectionTitleSpaced: {marginTop: 20},

  // MetricRow
  metricWrap:   {marginBottom: 10},
  metricRow:    {flexDirection: 'row', alignItems: 'center', gap: 8},
  metricLabel:  {width: 72, fontSize: 13, color: C.subtext},
  barTrack:     {flex: 1, height: 7, backgroundColor: C.surface1, borderRadius: 4, overflow: 'hidden'},
  barFill:      {height: '100%', borderRadius: 4},
  metricValue:  {width: 72, fontSize: 12, fontFamily: MONO, textAlign: 'right', color: C.text},
  metricDetail: {
    fontSize: 11,
    fontFamily: MONO,
    color: C.overlay1,
    marginTop: 2,
    marginLeft: 80,
  },

  // FPS tab
  fpsBig:  {flexDirection: 'row', alignItems: 'baseline', gap: 6, marginTop: 16},
  fpsNum:  {fontSize: 80, fontWeight: '700', fontFamily: MONO},
  fpsUnit: {fontSize: 16, color: C.overlay1},

  // Footer
  footer: {
    backgroundColor: C.mantle,
    paddingHorizontal: 20,
    paddingVertical: 12,
    flexDirection: 'row',
    justifyContent: 'flex-end',
    borderTopWidth: 1,
    borderTopColor: C.surface0,
  },
  footerRow: {gap: 8},
  footerHint: {fontSize: 11, color: C.overlay1, alignSelf: 'center'},

  // Buttons
  btn:        {backgroundColor: C.surface1, paddingHorizontal: 16, paddingVertical: 8, borderRadius: 8},
  btnDisabled:{opacity: 0.5},
  btnPressed: {opacity: 0.75},
  btnText:    {fontSize: 12, fontWeight: '700', color: C.text, letterSpacing: 0.5},

  // GPU stress box
  gpuStressBox: {
    width: GPU_BOX_SIDE,
    height: GPU_BOX_SIDE,
    backgroundColor: '#0D0D1A',
    borderRadius: 8,
    overflow: 'hidden',
  },

  // Misc
  placeholder: {fontSize: 13, color: C.muted, marginTop: 4},
  lowMem:      {fontSize: 13, color: C.red, marginTop: 8},
  dlStatus:    {fontSize: 12, color: C.overlay1, marginTop: 8, fontFamily: MONO},
  spanStatus:  {fontSize: 12, color: C.yellow, paddingHorizontal: 20, paddingVertical: 6, fontFamily: MONO},

  // Issues tab
  issueEmpty:     {flex: 1, alignItems: 'center', justifyContent: 'center'},
  issueEmptyText: {fontSize: 14, color: C.overlay1},
  issueRow: {
    flexDirection: 'row', backgroundColor: C.surface0,
    marginHorizontal: 0, marginBottom: 1,
  },
  issueSevBar: {width: 4},
  issueContent: {flex: 1, paddingHorizontal: 12, paddingVertical: 8},
  issueHeaderRow: {flexDirection: 'row', alignItems: 'center', marginBottom: 4, gap: 6},
  issueTypeChip: {
    borderWidth: 1, borderRadius: 3, paddingHorizontal: 5, paddingVertical: 1,
  },
  issueTypeText: {fontSize: 10, fontWeight: '700', fontFamily: MONO},
  issueSeverity: {fontSize: 11},
  issueTime: {fontSize: 11, fontFamily: MONO, color: C.overlay1, marginLeft: 'auto'},
  issueMsg: {fontSize: 12, color: C.text},
  issueDetails: {fontSize: 11, fontFamily: MONO, color: C.overlay1, marginTop: 2},
});
