package com.app.android.kotlin.facecaptcha.challenge

import com.app.android.kotlin.facecaptcha.data.model.challenge.ChallengeResponse

class CameraManager(private val challengeResponse: ChallengeResponse) {

    fun start(totalTimeChallenges: Long, callback: CameraPresenter.ManagerCallback) {
        callback.onBeforeSend()

        val delayTotalChallenges = (challengeResponse.totalTime - totalTimeChallenges)*1000

        Thread({
            Thread.sleep(delayTotalChallenges)
            executeChallenges(callback)
        }).start()

        startCounting(callback)

        callback.onComplete()
    }

    private fun executeChallenges(callback: CameraPresenter.ManagerCallback) {
        challengeResponse.challenges.forEach {
            callback.setMessage(it.mensagem)
            callback.loadIcon(it.icone ?: "")

            val delayChallenge = it.tempoEmSegundos * 1000
            val delayFrameMillis = challengeResponse.snapFrequenceInMillis.toLong()
            val countIteration = (delayChallenge / challengeResponse.snapFrequenceInMillis.toLong()).toInt()

            callback.takePicture(countIteration, delayFrameMillis)

            Thread.sleep(delayChallenge.toLong())
        }
    }

    private fun startCounting(callback: CameraPresenter.ManagerCallback) {
        var count = 1
        (count..challengeResponse.totalTime).forEach {
            callback.setCount(count.toString())
            Thread.sleep(1000)
            count++
        }
    }
}