import React, {useCallback, useEffect, useRef, useState} from 'react';
import {
  Animated,
  Easing,
  NativeEventEmitter,
  NativeModules,
  Platform,
  Pressable,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import {SafeAreaProvider, SafeAreaView} from 'react-native-safe-area-context';

const {KamperModule} = NativeModules;
const emitter = new NativeEventEmitter(KamperModule);

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
interface CpuData {
  totalUseRatio: number;
  appRatio: number;
  userRatio: number;
  systemRatio: number;
  ioWaitRatio: number;
}
interface FpsData {fps: number}
interface MemoryData {
  heapAllocatedMb: number;
  heapMaxMb: number;
  ramUsedMb: number;
  ramTotalMb: number;
  isLowMemory: boolean;
}
interface NetworkData {rxMb: number; txMb: number}
interface IssueData {
  id: string; type: string; severity: string; message: string;
  timestamp: number; durationMs?: number; threadName?: string;
}

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
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const toggleStress = useCallback(() => {
    if (stressing) {
      if (timerRef.current) clearInterval(timerRef.current);
      setStressing(false);
    } else {
      setStressing(true);
      timerRef.current = setInterval(() => {
        const end = Date.now() + 40;
        while (Date.now() < end) {}
      }, 60);
    }
  }, [stressing]);

  useEffect(() => () => {if (timerRef.current) clearInterval(timerRef.current);}, []);

  return (
    <View style={s.tabContent}>
      <ScrollView
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
function MemoryTab({mem, running}: {mem: MemoryData | null; running: boolean}) {
  return (
    <View style={s.tabContent}>
      <ScrollView
        contentContainerStyle={s.scrollContent}
        showsVerticalScrollIndicator={false}>
        {mem ? (
          <>
            <SectionTitle label="HEAP MEMORY" first />
            <MetricRow
              label="Heap"
              ratio={mem.heapMaxMb > 0 ? mem.heapAllocatedMb / mem.heapMaxMb : 0}
              value={mb1(mem.heapAllocatedMb)}
              color={C.green}
              detail={`${mb1(mem.heapAllocatedMb)} / ${mb1(mem.heapMaxMb)} max`}
            />
            <SectionTitle label="SYSTEM RAM" />
            <MetricRow
              label="RAM"
              ratio={mem.ramTotalMb > 0 ? mem.ramUsedMb / mem.ramTotalMb : 0}
              value={mb1(mem.ramUsedMb)}
              color={C.blue}
              detail={`${mb1(mem.ramUsedMb)} / ${mb1(mem.ramTotalMb)} total`}
            />
            {mem.isLowMemory ? (
              <Text style={s.lowMem}>⚠ Low Memory</Text>
            ) : null}
          </>
        ) : (
          <Placeholder running={running} />
        )}
      </ScrollView>
      <View style={[s.footer, s.footerRow]}>
        <Btn label="ALLOC 32 MB" onPress={() => {}} />
        <Btn label="FORCE GC"    onPress={() => {}} />
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
  return (
    <View style={s.tabContent}>
      {issues.length === 0
        ? <View style={s.issueEmpty}><Text style={s.issueEmptyText}>No issues detected</Text></View>
        : <ScrollView showsVerticalScrollIndicator={false}>
            {issues.map(issue => <IssueRow key={issue.id} issue={issue} />)}
          </ScrollView>
      }
      <View style={[s.footer, {justifyContent: 'flex-start', gap: 8}]}>
        <Btn label="SLOW SPAN" onPress={() => {
          const start = Date.now(); while(Date.now() - start < 800) {}
        }} />
        <Btn label="CLEAR" onPress={onClear} />
      </View>
    </View>
  );
}

// ── App ────────────────────────────────────────────────────────────────────────
const TABS = ['CPU', 'FPS', 'MEMORY', 'NETWORK', 'ISSUES'];

export default function App() {
  const [activeTab, setActiveTab] = useState(0);
  const [running, setRunning]     = useState(false);
  const [cpu, setCpu]             = useState<CpuData | null>(null);
  const [fps, setFps]             = useState<FpsData | null>(null);
  const [mem, setMem]             = useState<MemoryData | null>(null);
  const [net, setNet]             = useState<NetworkData | null>(null);
  const [issues, setIssues]       = useState<IssueData[]>([]);
  const peakRxRef                 = useRef(0);
  const peakTxRef                 = useRef(0);

  useEffect(() => {
    const subs = [
      emitter.addListener('kamper_cpu',     d => setCpu(d)),
      emitter.addListener('kamper_fps',     d => setFps(d)),
      emitter.addListener('kamper_memory',  d => setMem(d)),
      emitter.addListener('kamper_network', d => {
        peakRxRef.current = Math.max(peakRxRef.current, d.rxMb);
        peakTxRef.current = Math.max(peakTxRef.current, d.txMb);
        setNet(d);
      }),
      emitter.addListener('kamper_issues', (d: IssueData) => {
        setIssues(prev => [d, ...prev].slice(0, 100));
      }),
    ];
    return () => subs.forEach(sub => sub.remove());
  }, []);

  const toggle = useCallback(() => {
    if (running) {
      KamperModule.stop();
      setRunning(false);
      setCpu(null); setFps(null); setMem(null); setNet(null); setIssues([]);
      peakRxRef.current = 0; peakTxRef.current = 0;
    } else {
      KamperModule.start();
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
        {TABS.map((name, i) => (
          <Pressable
            key={name}
            style={[s.tab, activeTab === i && s.tabActive]}
            onPress={() => setActiveTab(i)}>
            <Text style={[s.tabText, activeTab === i && s.tabTextActive]}>
              {name}
            </Text>
          </Pressable>
        ))}
      </View>
      <View style={s.divider} />

      {/* Tab content */}
      {activeTab === 0 && <CpuTab     cpu={cpu} running={running} />}
      {activeTab === 1 && <FpsTab     fps={fps} />}
      {activeTab === 2 && <MemoryTab  mem={mem} running={running} />}
      {activeTab === 3 && (
        <NetworkTab
          net={net}
          peakRxRef={peakRxRef}
          peakTxRef={peakTxRef}
          running={running}
        />
      )}
      {activeTab === 4 && <IssuesTab issues={issues} onClear={() => setIssues([])} />}
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
  tabBar: {backgroundColor: C.mantle, flexDirection: 'row'},
  tab: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: 12,
    borderBottomWidth: 2,
    borderBottomColor: 'transparent',
  },
  tabActive:     {borderBottomColor: C.blue},
  tabText:       {fontSize: 12, fontWeight: '600', letterSpacing: 0.5, color: C.muted},
  tabTextActive: {color: C.blue, fontWeight: '700'},

  // Tab layout
  tabContent:    {flex: 1},
  scrollContent: {paddingHorizontal: 20, paddingTop: 20, paddingBottom: 12},

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

  // Misc
  placeholder: {fontSize: 13, color: C.muted, marginTop: 4},
  lowMem:      {fontSize: 13, color: C.red, marginTop: 8},
  dlStatus:    {fontSize: 12, color: C.overlay1, marginTop: 8, fontFamily: MONO},

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
