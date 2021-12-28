package com.smellouk.kamper.api

interface Info {
    companion object {
        val INVALID = object : Info {}
    }
}

typealias InfoListener<I> = ((I) -> Unit)
