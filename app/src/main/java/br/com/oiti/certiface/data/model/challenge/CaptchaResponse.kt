package br.com.oiti.certiface.data.model.challenge


class CaptchaResponse(val valid: Boolean,
                      val cause: String,
                      val hash: String,
                      val protocol: String)
