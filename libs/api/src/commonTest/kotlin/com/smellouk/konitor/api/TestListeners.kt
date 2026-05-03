package com.smellouk.konitor.api

fun interface MockableListener<I : Info> : (I) -> Unit
