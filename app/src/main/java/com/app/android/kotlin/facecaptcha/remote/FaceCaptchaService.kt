package com.app.android.kotlin.facecaptcha.remote

import com.app.android.kotlin.facecaptcha.model.ResponseChallenge
import org.json.JSONObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FaceCaptchaService {

    companion object {
        const val url: String = "/facecaptcha/service/captcha/"

    }

    @GET("${url}checkauth")
    fun auth(@Path("appkey") appKey: String): Response<JSONObject>

    @GET("${url}challenge")
    fun challenge(@Path("appkey") appKey: String, @Path("p") infoPerson: String): Response<ResponseChallenge>

    @POST(url)
    fun captcha(@Body params: CaptchaRequest): Response<JSONObject>
    data class CaptchaRequest(val appkey: String, val chkey: String, val images: MutableCollection<String>)

    @POST("${url}server")
    fun server(@Body params: ServerRequest): Response<JSONObject>
    data class ServerRequest(val serverkey: String)

    @POST("${url}icon/{id}")
    fun icon(@Body params: IconRequest, @Path("id") idIcon: String): Response<JSONObject>
    data class IconRequest(val appkey: String)
}