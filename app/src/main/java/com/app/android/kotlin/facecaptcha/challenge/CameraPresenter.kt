package com.app.android.kotlin.facecaptcha.challenge

import android.graphics.Bitmap
import android.hardware.Camera
import android.os.Handler
import android.os.SystemClock
import com.app.android.kotlin.facecaptcha.data.model.challenge.ChallengeDataResponse
import com.app.android.kotlin.facecaptcha.data.model.challenge.ChallengeResponse
import com.app.android.kotlin.facecaptcha.data.source.ChallengeRepository
import java.util.*


class CameraPresenter(private val view: CameraContract.View) {

    private val handler = Handler()
    private val repository = ChallengeRepository(BASE_URL, APP_KEY)

    fun start(params: String) {
        repository.challenge(params, { challengeResponse -> startChallenges(challengeResponse) })
    }

    fun destroy() {
        handler.removeCallbacksAndMessages(null)
    }

    private fun startChallenges(apiResponse: ChallengeResponse) {
        val totalChallengesDurationInSeconds = apiResponse.totalTime
        val snapFrequenceInMillis = apiResponse.snapFrequenceInMillis
        var startChallengeAtInMillis = 0L

        setupCounter(totalChallengesDurationInSeconds)

        apiResponse.challenges.forEach { challenge ->
            val challengeDurationInMillis = challenge.tempoEmSegundos * 1000

            scheduleChallenge(challenge, startChallengeAtInMillis, snapFrequenceInMillis)

            // IMPORTANTE: Não podemos deixar de incremetar o tempo de cada desafio no final
            startChallengeAtInMillis += challengeDurationInMillis
        }
    }

    private fun setupCounter(durationInSeconds: Int) {

        view.setCounter(durationInSeconds.toString())

        (0..(durationInSeconds-1)).reversed().forEachIndexed { index, it ->
            val delay = (index + 1) * 1000L
            handler.postAtTime({
                view.setCounter(it.toString())
            }, getPostAtTime(delay))
        }
    }

    private fun scheduleChallenge(challenge: ChallengeDataResponse, startAt: Long, snapFrequenceInMillis: Int) {
        val challengeDurationInMillis = challenge.tempoEmSegundos * 1000
        val numberOfPictures = challengeDurationInMillis / snapFrequenceInMillis

        val photos = ArrayList<ByteArray>()
        val takePictureCallback = Camera.PictureCallback { data, camera ->
            photos.add(data)
            camera.startPreview()

            // envia fotos ao finalizar o desafio
            if (numberOfPictures == photos.size) {
                sendPhotos(photos)
            }
        }

        // Agenda para alterar icone e imagem do desafio
        handler.postAtTime({ loadChallenge(challenge) }, getPostAtTime(startAt))

        // Agenda para capturar imagens do desafio
        (1..numberOfPictures).forEach {
            val delay = startAt + (snapFrequenceInMillis * it)
            handler.postAtTime({ takePicture(takePictureCallback) }, getPostAtTime(delay))
        }
    }

    private fun getPostAtTime(delay: Long): Long = SystemClock.uptimeMillis() + delay

    private fun loadChallenge(challenge: ChallengeDataResponse) {
        loadMessage(challenge.decodedMessage)
        loadIcon(challenge.decodedIcon)
    }

    private fun loadMessage(message: Bitmap?) {
        view.setMessage(message)
    }

    private fun loadIcon(icon: Bitmap?) {
        view.loadIcon(icon)
    }

    private fun takePicture(pictureCallback: Camera.PictureCallback) {
        view.takePicture(pictureCallback)
    }

    private fun sendPhotos(photos: MutableCollection<ByteArray>) {
//        dataSource?.sendPhotos(photos)
    }


    companion object {
        // Mock
        private val BASE_URL = "https://comercial.certiface.com.br:8443"
        private val APP_KEY = "oKIZ1jjpRyXCDDNiR--_OPNGiNmraDZIuGE1rlUyZwOGJJzDtJR7BahJ4MqnobwetlmjXFsYFeze0eBRAGS2KWmjFUp08HYsv6pyI3KZklISJVmKJDSgfmkRmPaBR9ZJP3wtVWFDwNR9kS_vecameg"
    }
}
