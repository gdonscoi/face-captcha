package br.com.oiti.certiface.challenge

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import br.com.oiti.certiface.R


class ChallengeActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    private val endpoint by lazy { intent.getStringExtra(PARAM_ENDPOINT) }
    private val appKey by lazy { intent.getStringExtra(PARAM_APP_KEY) }
    private val userParams by lazy { intent.getStringExtra(PARAM_USER_INFO) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_challenge)

        if(isInvalidBundleParams()){
            val data = Intent()
            data.putExtra(ChallengeActivity.PARAM_RESULT_ERROR, getString(R.string.invalid_activity_params_error))
            setResult(AppCompatActivity.RESULT_CANCELED, data)

            finish()
            return
        }

        val fragment = getFragment()
        fragment.arguments = buildBundle()

        startFragment(R.id.challenge_fragment_container, fragment)
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }

    private fun isInvalidBundleParams(): Boolean {
        return endpoint.isNullOrBlank() ||
                appKey.isNullOrBlank() ||
                userParams.isNullOrBlank()
    }

    private fun getFragment(): AbstractChallengeFragment {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Camera2Fragment()
        } else {
            CameraFragment()
        }
    }

    private fun buildBundle(): Bundle {
        val bundle = Bundle()

        bundle.putString(PARAM_ENDPOINT, endpoint)
        bundle.putString(PARAM_APP_KEY, appKey)
        bundle.putString(PARAM_USER_INFO, userParams)

        return bundle
    }

    private fun startFragment(containerViewId: Int, fragment: Fragment) {
        supportFragmentManager.beginTransaction()
                .add(containerViewId, fragment)
                .commit()
    }

    companion object {
        val PARAM_APP_KEY = "app_key"
        val PARAM_ENDPOINT = "endpoint"
        val PARAM_USER_INFO = "user_info"
        val PARAM_RESULT = "certiface_result"
        val PARAM_RESULT_HASH = "certiface_result_hash"
        val PARAM_RESULT_PROTOCOL = "certiface_result_protocol"
        val PARAM_RESULT_ERROR = "certiface_result_error"
    }

}
