package com.smellouk.konitor.rn

import com.smellouk.konitor.api.PerformanceModule

expect val JsMemoryModule: PerformanceModule<JsMemoryConfig, JsMemoryInfo>
expect val JsGcModule: PerformanceModule<JsGcConfig, JsGcInfo>
expect val JsIssueModule: PerformanceModule<JsIssueConfig, JsIssueInfo>
