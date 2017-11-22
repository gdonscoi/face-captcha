package com.app.android.kotlin.facecaptcha.data.source.remote.api

import com.app.android.kotlin.facecaptcha.data.model.challenge.CaptchaResponse
import com.app.android.kotlin.facecaptcha.data.model.challenge.ChallengeResponse
import retrofit2.Call
import retrofit2.http.*

interface ChallengeApi {

    /**
     *
     * Utilizado para validar a chave de aplicação
     *
     * type: 'GET'
     * url: '/facecaptcha/service/captcha/checkauth'
     *
     * @params:  appKey: chave de aplicação
     *
     */
    @GET("/facecaptcha/service/captcha/checkauth")
    fun checkauth(@Query("appkey") appKey: String): Call<String>

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
     * Utilizado para recuperar os ícones de cada desafio.
     *
     * type: 'POST'
     * url: '/facecaptcha/service/captcha//icon/{id}'
     *
     * @params:  appKey : chave de aplicação
     *           id     : tipoFace.codigo
     *
     */
    @FormUrlEncoded
    @POST("/facecaptcha/service/captcha/icon/{id}")
    fun icon(@Path("id") id: String, @Field("appkey") appKey: String): Call<ByteArray>

    /**
     *
     * Utilizado para validar o desafio
     *
     * type: 'POST'
     * url: '/facecaptcha/service/captcha'
     *
     * @params:  appKey : chave de aplicação
     *           chkey  : chave do desafio
     *           images : Imagens capturadas, cada imagem capturada segue este formato:
     *                          'data:image/jpeg;base64,' + 'type:' + tipoFace.codigo + ',' + imgB64Data;
     *
     */
    @POST("/facecaptcha/service/captcha")
    fun captcha(@Field("appkey") appKey: String, @Field("chkey") chkey: String, @Field("images") images: List<String>): Call<CaptchaResponse>
}
