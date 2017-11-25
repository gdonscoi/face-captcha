package br.com.oiti.certiface.data.challenge

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Camera
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import android.view.View
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.collections.HashMap


class CameraPresenter(private val view: CameraContract.View) {

    private val handlerThread = HandlerThread(this::javaClass.name)
    private var handler: Handler

    private val repository = ChallengeRepository(BASE_URL, APP_KEY)

    private val photos = HashMap<ByteArray, String>()


    init {
        handlerThread.start()
        handler = Handler(handlerThread.looper)
    }

    fun start(params: String) {
        photos.clear()
        view.startChallenge()
        repository.challenge(params, { challengeResponse -> startChallenges(challengeResponse) })
    }

    fun destroy() {
        repository.destroy()

        handler.removeCallbacksAndMessages(null)
        photos.clear()
    }

    private fun startChallenges(apiResponse: ChallengeResponse) {
        val chKey = apiResponse.chkey
        val totalChallengesDurationInSeconds = apiResponse.totalTime
        val snapFrequenceInMillis = apiResponse.snapFrequenceInMillis
        var startChallengeAtInMillis = 0L

        val totalChallengePictures = countTotalChallengePictures(apiResponse)

        setupCounter(totalChallengesDurationInSeconds)

        apiResponse.challenges.forEach { challenge ->
            val challengeDurationInMillis = challenge.tempoEmSegundos * 1000

            scheduleChallenge(
                    challenge,
                    startChallengeAtInMillis,
                    snapFrequenceInMillis,
                    {
                        if (totalChallengePictures == photos.size) {
                            sendChallenge(chKey, photos)
                        }
                    })

            // IMPORTANTE: Não podemos deixar de incremetar o tempo de cada desafio no final
            startChallengeAtInMillis += challengeDurationInMillis
        }
    }

    private fun countTotalChallengePictures(apiResponse: ChallengeResponse): Int {
        val snapFrequenceInMillis = apiResponse.snapFrequenceInMillis
        var totalOfPictures = 0

        apiResponse.challenges.forEach { challenge ->
            val challengeDurationInMillis = challenge.tempoEmSegundos * 1000
            val numberOfPictures = challengeDurationInMillis / snapFrequenceInMillis

            totalOfPictures += numberOfPictures
        }

        return totalOfPictures
    }

    private fun setupCounter(durationInSeconds: Int) {

        view.setCounter(durationInSeconds.toString())

        (0..(durationInSeconds - 1)).reversed().forEachIndexed { index, it ->
            val delay = (index + 1) * 1000L
            handler.postAtTime({
                view.setCounter(it.toString())
            }, getPostAtTime(delay))
        }
    }

    private fun scheduleChallenge(challenge: ChallengeDataResponse, startAt: Long, snapFrequenceInMillis: Int, afterTakePicture: () -> Unit) {
        val challengeDurationInMillis = challenge.tempoEmSegundos * 1000
        val numberOfPictures = challengeDurationInMillis / snapFrequenceInMillis
        val tipoFaceCodigo = challenge.tipoFace.codigo

        val callback = Camera.PictureCallback { data, camera ->
            handler.post({
                photos.put(reduceImage(data), tipoFaceCodigo)
                afterTakePicture()
            })
            camera.startPreview()
        }

        // Agenda para alterar icone e imagem do desafio
        handler.postAtTime({ loadChallenge(challenge) }, getPostAtTime(startAt))

        // Agenda para capturar imagens do desafio
        (1..numberOfPictures).forEach {
            val delay = startAt + (snapFrequenceInMillis * it)
            handler.postAtTime({ takePicture(callback) }, getPostAtTime(delay))
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

    private fun sendChallenge(chKey: String, images: Map<ByteArray, String>) {
        view.loadingView()

        Log.d(this::class.java.name, "Starting send captcha at " + Date())
        repository.captcha(chKey, images, { captchaResponse ->
            Log.d(this::class.java.name, "Finish send captcha at " + Date() + ". Valid: " + captchaResponse.valid)

            val messageAnimation: String

            if (captchaResponse.valid) {
                messageAnimation = "Sucesso na autenticação"
                handler.post({
                    Thread.sleep(2500)
                    view.finishChallenge(captchaResponse.valid)
                })
            } else {
                messageAnimation = "Erro na autenticação"
                handler.post({
                    Thread.sleep(2500)
                    view.initialView()
                })
            }

            view.animationFeedback(View.VISIBLE, messageAnimation)
        })
    }

    private fun reduceImage(byteImage: ByteArray): ByteArray {
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ALPHA_8
        options.inTargetDensity = 72
        options.inDensity = 72

        val image = BitmapFactory.decodeByteArray(byteImage, 0, byteImage.size, options)
        val bos = ByteArrayOutputStream()

        image.compress(Bitmap.CompressFormat.JPEG, 100, bos)

        return bos.toByteArray()
    }


    companion object {
        // Mock
        private val BASE_URL = "https://comercial.certiface.com.br:8443"
        private val APP_KEY = "oKIZ1jjpRyXCDDNiR--_OPNGiNmraDZIuGE1rlUyZwOGJJzDtJR7BahJ4MqnobwetlmjXFsYFeze0eBRAGS2KWmjFUp08HYsv6pyI3KZklISJVmKJDSgfmkRmPaBR9ZJP3wtVWFDwNR9kS_vecameg"
    }
}
