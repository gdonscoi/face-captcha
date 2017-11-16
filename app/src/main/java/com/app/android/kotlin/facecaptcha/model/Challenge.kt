package com.app.android.kotlin.facecaptcha.model

import com.google.gson.annotations.SerializedName

class Challenge {

    @SerializedName("mensagem")
    var message: String = ""

    @SerializedName("grayscale")
    var grayScale: Boolean = false

    @SerializedName("tempoEmSegundos")
    var duration: Int = 0

    @SerializedName("tipoFace")
    var typeFace: TypeFace? = null

    data class TypeFace(val codigo: String, val imagem: String)
}