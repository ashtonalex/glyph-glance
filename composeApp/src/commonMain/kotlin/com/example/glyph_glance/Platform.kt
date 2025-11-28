package com.example.glyph_glance

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform