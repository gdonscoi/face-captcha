package com.app.android.kotlin.facecaptcha.data.model.challenge

/**
 * Created by bzumpano on 15/11/17.
 */
class ChallengeDataResponse(val mensagem: String,
                            val grayscale: Boolean,
                            val tempoEmSegundos: Integer,
                            val tipoFace: TipoFaceResponse)
