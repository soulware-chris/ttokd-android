package kr.co.miaps.welfare.api

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout

import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Url
import io.ktor.http.isSuccess
import io.ktor.http.path

class OneId(private val saveCookies : Boolean = false) {
    private val httpClient by lazy {
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = 30000
                socketTimeoutMillis = 30000
                connectTimeoutMillis = 30000
            }
            if(saveCookies) {
                install(HttpCookies) {
                    storage = CookiesStorageManager.cookiesStorage
                }
            }
        }
    }

    suspend fun fetchGet(url : String, path : String?) = httpClient.use { client ->
        val response = client.get(url) {
            path?.let {
                url { this.path(path) }
            }
        }

        val result = response.bodyAsText()
        if(response.status.isSuccess()) {
            val cookies = CookiesStorageManager.cookiesStorage.get(Url(urlString = url))
            cookies.forEach {
                Log.d("OneId", "fetchGet():  cookie :  $it")
            }
            Log.d("OneId", "fetchGet():  $result")
            result
        } else {
            Log.e("OneId", "fetchGet(): ${response.status} , $result")
            null
        }
    }
}