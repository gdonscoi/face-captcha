package com.app.android.kotlin.facecaptcha.remote

import com.app.android.kotlin.facecaptcha.model.ResponseChallenge
import retrofit2.http.Body
import retrofit2.http.Path

class MockedFaceCaptchaService {

    fun auth(@Path("appkey") appKey: String): String {
        Thread.sleep(2000)
        return "appkey"
    }

    fun challenge(@Path("appkey") appKey: String, @Path("p") infoPerson: String): ResponseChallenge {
        Thread.sleep(2000)
        return ResponseChallenge()
    }

    fun captcha(@Body params: FaceCaptchaService.CaptchaRequest): String {
        Thread.sleep(2000)
        return "ok"
    }

    fun server(@Body params: FaceCaptchaService.ServerRequest): String {
        Thread.sleep(2000)
        return "ok"
    }

    fun icon(@Body params: FaceCaptchaService.IconRequest, @Path("id") idIcon: String): String {
        Thread.sleep(2000)
        return "bytes"
    }
}