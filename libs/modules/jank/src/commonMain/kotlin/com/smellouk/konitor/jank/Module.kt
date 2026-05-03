package com.smellouk.konitor.jank

import com.smellouk.konitor.api.PerformanceModule

expect val JankModule: PerformanceModule<JankConfig, JankInfo>
