package br.com.oiti.certiface.data.model.challenge

import android.graphics.Bitmap
import com.google.gson.annotations.Expose


class ChallengeDataResponse(val mensagem: String,
                            val grayscale: Boolean,
                            val tempoEmSegundos: Int,
                            val tipoFace: TipoFaceResponse) {
    @Expose
    var decodedMessage: Bitmap? = null

    @Expose
    var decodedIcon: Bitmap? = null
}
