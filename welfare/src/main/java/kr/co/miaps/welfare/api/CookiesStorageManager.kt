package kr.co.miaps.welfare.api

import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage

object CookiesStorageManager {
    val cookiesStorage: CookiesStorage = AcceptAllCookiesStorage()
}