package br.com.oiti.certiface.sample

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import br.com.oiti.certiface.challenge.ChallengeActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startChallenge(this@MainActivity)

        button_start.setOnClickListener { startChallenge(this@MainActivity) }
    }


    private fun startChallenge(context: Context) {
        val userInfoParam = "user,comercial.token,cpf,8136822824,nome,ALESSANDRO DE OLIVEIRA FARIA,nascimento,27/05/1972"

        val intent = Intent(context, ChallengeActivity::class.java).apply {
            putExtra(ChallengeActivity.USER_INFO_KEY, userInfoParam)
        }

        startActivityForResult(intent, CAPTCHA_RESULT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1) {
            if (requestCode == RESULT_OK) {
                val result = data?.getBooleanExtra(ChallengeActivity.ACTIVITY_RESULT_KEY, false)
                Toast.makeText(MainActivity@ this, "Sucesso: $result", Toast.LENGTH_LONG).show()
            }

            Toast.makeText(MainActivity@ this, "Ação cancelada", Toast.LENGTH_LONG).show()
        }
    }

    companion object {

        val CAPTCHA_RESULT = 1
        val ENDPOINT = ""
    }
}
