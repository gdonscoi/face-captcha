package br.com.oiti.certiface.data.model.challenge

/**
 * Created by bzumpano on 15/11/17.
 */
class ChallengeResponse(val chkey: String,
                        val totalTime: Int,
                        val snapFrequenceInMillis: Int,
                        val challenges: List<ChallengeDataResponse>)
