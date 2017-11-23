package com.app.android.kotlin.facecaptcha.data.model.challenge

import android.graphics.Bitmap
import com.google.gson.annotations.Expose

/**
 * Created by bzumpano on 15/11/17.
 */
class ChallengeDataResponse(val mensagem: String,
                            val grayscale: Boolean,
                            val tempoEmSegundos: Int,
                            val tipoFace: TipoFaceResponse,
                            var icone: String? = "") {
    @Expose
    var decodedMessage: Bitmap? = null

    @Expose
    var decodedIcon: Bitmap? = null
}
