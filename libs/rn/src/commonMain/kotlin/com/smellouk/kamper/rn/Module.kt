package com.smellouk.kamper.rn

import com.smellouk.kamper.api.PerformanceModule

expect val JsMemoryModule: PerformanceModule<JsMemoryConfig, JsMemoryInfo>
expect val JsGcModule: PerformanceModule<JsGcConfig, JsGcInfo>
expect val JsIssueModule: PerformanceModule<JsIssueConfig, JsIssueInfo>
