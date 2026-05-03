package com.smellouk.konitor.network

import com.smellouk.konitor.api.PerformanceModule

expect val NetworkModule: PerformanceModule<NetworkConfig, NetworkInfo>
