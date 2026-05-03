package com.smellouk.konitor.api

interface Info {
    companion object {
        val INVALID = object : Info {}
        val UNSUPPORTED = object : Info {}
    }
}

typealias InfoListener<I> = ((I) -> Unit)
