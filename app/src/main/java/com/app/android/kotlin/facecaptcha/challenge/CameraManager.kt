package com.app.android.kotlin.facecaptcha.challenge

import android.util.Log
import com.app.android.kotlin.facecaptcha.data.model.challenge.ChallengeResponse
import java.util.*


class CameraManager(private val challengeResponse: ChallengeResponse) {

    private var count = 1
    private val timer: Timer = Timer()

    fun start(totalTimeChallenges: Long, callback: CameraPresenter.ManagerCallback) {
        callback.onBeforeSend()

        val delayTotalChallenges = (challengeResponse.totalTime - totalTimeChallenges) * 1000
        var delayChallengeOverride: Long = 0

        challengeResponse.challenges.forEach {
            delayChallengeOverride += delayTotalChallenges
            Log.i("delayChallengeOverride" , delayChallengeOverride.toString())
            val delayChallenge = (it.tempoEmSegundos * 1000L)

            timer.schedule(executeChallenges(delayChallenge, it.mensagem, it.icone ?: "", callback), delayChallengeOverride, delayChallenge)
        }

        timer.schedule(startCounting(callback), 0, 1000L)
    }

    private fun executeChallenges(delayChallenge: Long, mensagem: String, icone: String, callback: CameraPresenter.ManagerCallback): TimerTask {
        return object : TimerTask() {
            override fun run() {
                Log.i("executeChallenges" , mensagem)

                callback.setMessage(mensagem)
                callback.loadIcon(icone)

                val delayFrameMillis = challengeResponse.snapFrequenceInMillis.toLong()
                val countIteration = (delayChallenge / challengeResponse.snapFrequenceInMillis.toLong()).toInt()

                callback.takePicture(countIteration, delayFrameMillis)
                cancel()
            }
        }
    }

    private fun startCounting(callback: CameraPresenter.ManagerCallback): TimerTask {
        return object : TimerTask() {
            override fun run() {
                if (count > challengeResponse.totalTime) {
                    callback.onComplete()
                    timer.cancel()
                    return
                }

                Log.i("startCounting" , count.toString())
                callback.setCount(count.toString())
                count++
            }
        }
    }
}
