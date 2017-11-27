package br.com.oiti.certiface.sample

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import br.com.oiti.certiface.challenge.ChallengeActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        addDevelopmentStrictModes()

        startChallenge(this@MainActivity)

        button_start.setOnClickListener { startChallenge(this@MainActivity) }
    }


    private fun startChallenge(context: Context) {
        val intent = Intent(context, ChallengeActivity::class.java).apply {
            putExtra(ChallengeActivity.PARAM_ENDPOINT, ENDPOINT)
            putExtra(ChallengeActivity.PARAM_APP_KEY, APP_KEY)
            putExtra(ChallengeActivity.PARAM_USER_INFO, USER_INFO)
        }

        startActivityForResult(intent, CAPTCHA_RESULT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1) {
            if (requestCode == RESULT_OK) {
                val result = data?.getBooleanExtra(ChallengeActivity.PARAM_ACTIVITY_RESULT, false)
                Toast.makeText(MainActivity@ this, "Sucesso: $result", Toast.LENGTH_LONG).show()
            }

            Toast.makeText(MainActivity@ this, "Ação cancelada", Toast.LENGTH_LONG).show()
        }
    }


    private fun addDevelopmentStrictModes() {

        if (BuildConfig.DEBUG) {
            Thread {
                addStrictModeThreadPolicy()
                addStrictModeVmPolicy()
            }.start()
        }

    }

    private fun addStrictModeThreadPolicy() {
        StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                        .detectDiskReads()
                        .detectDiskWrites()
                        .detectNetwork()
                        .penaltyLog()
                        .build())
    }

    private fun addStrictModeVmPolicy() {
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build())
    }

    companion object {

        val CAPTCHA_RESULT = 1

        // Mock
        private val ENDPOINT = "https://comercial.certiface.com.br:8443"
        private val APP_KEY = "oKIZ1jjpRyXCDDNiR--_OPNGiNmraDZIuGE1rlUyZwOGJJzDtJR7BahJ4MqnobwetlmjXFsYFeze0eBRAGS2KWmjFUp08HYsv6pyI3KZklISJVmKJDSgfmkRmPaBR9ZJP3wtVWFDwNR9kS_vecameg"
        private val USER_INFO = "user,comercial.token,cpf,8136822824,nome,ALESSANDRO DE OLIVEIRA FARIA,nascimento,27/05/1972"
    }
}
