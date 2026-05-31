package org.example.frontend.util

expect fun saveLocalStorage(key: String, value: String)
expect fun loadLocalStorage(key: String): String?
expect fun removeLocalStorage(key: String)
