package br.com.oiti.certiface.data.model.challenge

/**
 * Created by bzumpano on 16/11/17.
 */
class CaptchaResponse(val valid: Boolean,
                      val cause: String,
                      val hash: String,
                      val protocol: String)
