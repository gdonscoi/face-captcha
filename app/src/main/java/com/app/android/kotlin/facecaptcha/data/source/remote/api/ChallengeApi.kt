package com.app.android.kotlin.facecaptcha.data.source.remote.api

import com.app.android.kotlin.facecaptcha.data.model.challenge.ChallengeResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ChallengeApi {

    /**
     *
     * type: 'GET'
     * url: '/facecaptcha/service/captcha/checkauth'
     *
     * @params:  'appkey': chave de aplicação
     *
     */
    @GET("/facecaptcha/service/captcha/checkauth")
    fun checkauth(@Query("appkey") appkey: String): Call<Unit>

    /**
     *
     * type: 'GET'
     * url: '/facecaptcha/service/captcha/checkauth'
     *
     * @params:  'appkey': chave de aplicação
     *           'p'     : AES.encrypt(dados sensíveis do usuário)
     *
     */
    @GET("/facecaptcha/service/captcha/challenge")
    fun challenge(@Query("appkey") appkey: String, @Query("p") p: String): Call<ChallengeResponse>
}
