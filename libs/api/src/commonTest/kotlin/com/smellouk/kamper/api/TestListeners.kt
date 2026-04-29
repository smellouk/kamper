package com.smellouk.kamper.api

fun interface MockableListener<I : Info> : (I) -> Unit
