package com.app.android.kotlin.facecaptcha.data.source.remote

import com.app.android.kotlin.facecaptcha.challenge.CameraPresenter
import com.app.android.kotlin.facecaptcha.data.model.challenge.ChallengeDataResponse
import com.app.android.kotlin.facecaptcha.data.model.challenge.ChallengeResponse
import com.app.android.kotlin.facecaptcha.data.model.challenge.TipoFaceResponse

class MockChallengeRemoteDataSource(private val appkey: String) {

    fun challenge(p: String, callback: CameraPresenter.ChallengeCallback): ChallengeResponse? {
        var challengeResponse: ChallengeResponse? = null

        try {
            callback.onBeforeSend()
            challengeResponse = mockChallengeResponse()
            Thread.sleep(2000)

            challengeResponse.challenges.forEach {
                it.icone = icon(it.tipoFace.imagem) // codigo
            }

        } catch (e: Exception) {
            callback.onError(e.message ?: "Erro")
        }finally {
            callback.onComplete()
        }

        return challengeResponse
    }

    fun icon(id: String): String {
        Thread.sleep(2000)
        return id
    }

    private fun mockChallengeResponse(): ChallengeResponse {
        val tipoFacaReponse1 = TipoFaceResponse("SORRIA.jpg", "=D")
        val tipoFacaReponse2 = TipoFaceResponse("PISQUE_OS_OLHOS.jpg", ";)")

        val challengeDataResponse1 = ChallengeDataResponse("Sorria", false, 3, tipoFacaReponse1, "")
        val challengeDataResponse2 = ChallengeDataResponse("Pisque os olhos", false, 3, tipoFacaReponse2, "")

        val challengeList: ArrayList<ChallengeDataResponse> = ArrayList()
        challengeList.add(challengeDataResponse1)
        challengeList.add(challengeDataResponse2)

        return ChallengeResponse("chkey", 9, 1490, challengeList)
    }
}
