@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.example.frontend.util

@JsFun("(key, value) => localStorage.setItem(key, value)")
private external fun jsSetItem(key: String, value: String)

@JsFun("key => localStorage.getItem(key)")
private external fun jsGetItem(key: String): String?

@JsFun("key => localStorage.removeItem(key)")
private external fun jsRemoveItem(key: String)

actual fun saveLocalStorage(key: String, value: String) {
    jsSetItem(key, value)
}

actual fun loadLocalStorage(key: String): String? {
    return jsGetItem(key)
}

actual fun removeLocalStorage(key: String) {
    jsRemoveItem(key)
}
