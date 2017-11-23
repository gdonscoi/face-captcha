package com.app.android.kotlin.facecaptcha.challenge

import android.hardware.Camera
import com.app.android.kotlin.facecaptcha.data.model.challenge.ChallengeResponse
import java.util.*
import kotlin.collections.ArrayList


class CameraManager(private val challengeResponse: ChallengeResponse) {

    private var count = 1

    fun start(callback: CameraPresenter.ManagerCallback) {
        callback.onBeforeSend()

        val timer = Timer()
        timer.schedule(startCounting(callback), 0, 1000L)

        challengeResponse.challenges.forEach {
            val photos: MutableCollection<ByteArray> = ArrayList()
            val pictureCallback = Camera.PictureCallback { data, camera ->
                photos.add(data)
//                camera?.stopPreview()
//                camera?.startPreview()
            }

            val delayChallenge = (it.tempoEmSegundos * 1000L)
            executeChallenges(delayChallenge, it.mensagem, it.tipoFace.imagem, callback, pictureCallback)
            callback.sendPhotos(photos)
        }

        timer.cancel()
        callback.onComplete()
    }

    private fun executeChallenges(delayChallenge: Long, message: String, icone: String, callback: CameraPresenter.ManagerCallback, pictureCallback: Camera.PictureCallback) {
        callback.loadMessage(message)
        callback.loadIcon(icone)

        val delayFrameMillis = challengeResponse.snapFrequenceInMillis.toLong()
        val countIteration = (delayChallenge / challengeResponse.snapFrequenceInMillis.toLong()).toInt()

        Thread({
            (1..countIteration).forEach {
                callback.takePicture(pictureCallback)
                Thread.sleep(delayFrameMillis)
            }
        }).start()

        Thread.sleep(delayChallenge)
    }

    private fun startCounting(callback: CameraPresenter.ManagerCallback): TimerTask {
        return object : TimerTask() {
            override fun run() {
                callback.setCount(count.toString())
                count++
            }
        }
    }
}
