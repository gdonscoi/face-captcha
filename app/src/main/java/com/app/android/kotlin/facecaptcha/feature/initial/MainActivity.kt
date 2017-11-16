package com.app.android.kotlin.facecaptcha.feature.initial

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.app.android.kotlin.facecaptcha.R
import com.app.android.kotlin.facecaptcha.remote.MockedFaceCaptchaService

class MainActivity : AppCompatActivity(), MainContract.View {

    companion object {
        const val ARG_APP_KEY = "app_key"
    }

    private var presenter: MainPresenter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val appKey = intent.getStringExtra(ARG_APP_KEY)

        presenter = MainPresenter(this, MockedFaceCaptchaService())

    }

    override fun onBefore() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onSuccess() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onError(message: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onComplete() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
