package org.example.frontend.util

import kotlinx.browser.localStorage

actual fun saveLocalStorage(key: String, value: String) {
    localStorage.setItem(key, value)
}

actual fun loadLocalStorage(key: String): String? {
    return localStorage.getItem(key)
}

actual fun removeLocalStorage(key: String) {
    localStorage.removeItem(key)
}
