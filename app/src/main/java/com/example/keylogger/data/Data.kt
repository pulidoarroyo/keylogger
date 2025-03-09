package com.example.keylogger.data

data class Data(
    val type: TYPE,
    val message: String? = null,
    val geo: Geo? = null
)

enum class TYPE(type: Int) {
    MESSAGE(1),
    GEO(2)
}