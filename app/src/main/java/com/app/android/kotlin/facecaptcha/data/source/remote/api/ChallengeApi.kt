package com.app.android.kotlin.facecaptcha.data.source.remote.api

import com.app.android.kotlin.facecaptcha.data.model.challenge.CaptchaResponse
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ChallengeApi {

    /**
     *
     * Utilizado para obter o desafio (olhe para cima, sorria...)
     *
     * type: 'GET'
     * url: '/facecaptcha/service/captcha/checkauth'
     *
     * @params:  appKey : chave de aplicação
     *           p      : AES.encrypt(dados sensíveis do usuário)
     *
     */
    @FormUrlEncoded
    @POST("/facecaptcha/service/captcha/challenge")
    fun challenge(@Field("appkey") appKey: String, @Field("p") p: String): Call<String>

    /**
     *
     * Utilizado para validar o desafio
     *
     * type: 'POST'
     * url: '/facecaptcha/service/captcha'
     *
     * @params:  appKey : chave de aplicação
     *           chkey  : chave do desafio
     *           images : Imagens capturadas e criptografadas, cada imagem capturada segue este formato:
     *                          'data:image/jpeg;base64,' + 'type:' + tipoFace.codigo + ',' + imgB64Data;
     *
     */
    @FormUrlEncoded
    @POST("/facecaptcha/service/captcha")
    fun captcha(@Field("appkey") appKey: String, @Field("chkey") chkey: String, @Field("images") images: String): Call<CaptchaResponse>
}
