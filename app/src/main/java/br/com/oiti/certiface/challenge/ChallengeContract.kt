package br.com.oiti.certiface.challenge

import android.graphics.Bitmap
import br.com.oiti.certiface.data.model.challenge.CaptchaResponse


interface ChallengeContract {
    interface View {

        fun initialView()

        fun startChallenge()

        fun buildTakePictureHandler(photos: HashMap<ByteArray, String>, afterTakePicture: (data: ByteArray) -> Unit): Any

        fun takePicture(callback: Any)

        fun loadIcon(icon: Bitmap?)

        fun setMessage(message: Bitmap?)

        fun setCounter(count: String)

        fun loadingView()

        fun animationFeedback(visibility: Int, message: String)

        fun finishChallenge(response: CaptchaResponse)
    }
}
