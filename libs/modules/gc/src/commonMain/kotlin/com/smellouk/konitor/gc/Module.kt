package com.smellouk.konitor.gc

import com.smellouk.konitor.api.PerformanceModule

expect val GcModule: PerformanceModule<GcConfig, GcInfo>
