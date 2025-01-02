package com.omeraydin.etkinlikprojesi.controller

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.omeraydin.etkinlikprojesi.view.MainActivity

class FacebookActivity : AppCompatActivity() {
    private lateinit var callbackManager: CallbackManager
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
        auth.setLanguageCode("tr") // Firebase dil kodu Türkçe'ye ayarlandı

        // CallbackManager oluşturma
        callbackManager = CallbackManager.Factory.create()

        // Facebook LoginManager başlatma
        LoginManager.getInstance().logInWithReadPermissions(
            this,
            listOf("email", "public_profile")
        )

        // Callback ekleme
        LoginManager.getInstance().registerCallback(callbackManager, object :
            FacebookCallback<LoginResult> {
            override fun onCancel() {
                Toast.makeText(this@FacebookActivity, "Giriş iptal edildi.", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onError(error: FacebookException) {
                Toast.makeText(
                    this@FacebookActivity,
                    "Facebook giriş hatası: ${error.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }

            override fun onSuccess(result: LoginResult) {
                // Facebook AccessToken işlemi
                handleFacebookAccessToken(result.accessToken)
            }
        })
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user?.isEmailVerified == true) {
                        Toast.makeText(
                            this,
                            "Facebook ile giriş yapıldı! Hoşgeldin ${user.displayName}",
                            Toast.LENGTH_LONG
                        ).show()
                        updateUI()
                    } else {
                        user?.sendEmailVerification()?.addOnSuccessListener {
                            Toast.makeText(
                                this,
                                "Kayıt başarılı! Mailinizi kontrol edin.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Giriş başarısız: ${task.exception?.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun updateUI() {
        // Giriş başarılı olduğunda ana ekrana yönlendirme
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // FacebookActivity'i sonlandırma
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // CallbackManager sonucu işleme
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }
}
