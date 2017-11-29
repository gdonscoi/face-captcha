package br.com.oiti.certiface.challenge

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

        val fragment = getFragment()
        fragment.arguments = buildBundle()

        startFragment(R.id.challenge_fragment_container, fragment)
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        super.onBackPressed()
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
        val PARAM_ACTIVITY_RESULT = "certiface_result"
    }

}
