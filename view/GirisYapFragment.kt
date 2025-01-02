package com.omeraydin.etkinlikprojesi.view

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.navigation.Navigation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.omeraydin.etkinlikprojesi.databinding.FragmentGirisYapBinding
import com.facebook.FacebookSdk;
import com.facebook.FacebookSdk.getApplicationContext
import com.facebook.appevents.AppEventsLogger;
import com.omeraydin.etkinlikprojesi.controller.FacebookActivity
import com.omeraydin.etkinlikprojesi.controller.GoogleActivity


class GirisYapFragment : Fragment() {
    private var _binding: FragmentGirisYapBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        auth.setLanguageCode("tr")

        FacebookSdk.sdkInitialize(getApplicationContext())
        AppEventsLogger.activateApp(getApplicationContext() as Application)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentGirisYapBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val currentUser = auth.currentUser

        if(currentUser != null){
            println(currentUser.email.toString())
            if(auth.currentUser!!.isEmailVerified){
                val intent = Intent(requireActivity(), MainAnasayfaActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
            }
        }else{
            binding.btnLogin.setOnClickListener { login_click(it) }
            binding.btnRegister.setOnClickListener { register_click(it) }
            binding.btnForgotPassword.setOnClickListener { forgot_password_click(it) }
            binding.btnGoogleSignIn.setOnClickListener { signInWithGoogle() }
            binding.btnFacebookSignIn.setOnClickListener{ signInWithFace(it) }
/*
            // Geri tuşuna tıklanması durumunda yapılacak işlemi engelle
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                // Uygulamayı arka plana al
                requireActivity().moveTaskToBack(true)
            }*/
        }
    }

    fun signInWithFace(view: View){
        val intent = Intent(requireActivity(), FacebookActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(intent)
    }


    private fun signInWithGoogle() {
        val intent = Intent(requireActivity(), GoogleActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(intent)
    }

    fun login_click(view: View) {
        val email = binding.etxtEmail.text.toString()
        val password = binding.etxtPassword.text.toString()

        if (email.isNotEmpty() && password.isNotEmpty()) {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Kullanıcı başarıyla giriş yaptı
                        val user = auth.currentUser
                        if (user != null) {
                            // E-posta doğrulamasını kontrol et
                            if (user.isEmailVerified) {
                                Toast.makeText(requireContext(), "Hoşgeldin, ${user.displayName}", Toast.LENGTH_SHORT).show()
                                // Kullanıcı ana sayfaya yönlendirilir
                                val intent = Intent(requireActivity(), MainAnasayfaActivity::class.java)
                                startActivity(intent)
                                requireActivity().finish()
                            } else {
                                Toast.makeText(requireContext(), "Mailinizi Onaylayınız!", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        // Giriş başarısız oldu
                        Toast.makeText(requireContext(), "Giriş Başarısız! ${task.exception?.localizedMessage ?: "Bir hata oluştu"}", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { exception ->
                    // Hata durumunu yakala ve göster
                    Toast.makeText(requireContext(), "1.Hata: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(requireContext(), "Eksik Veri!", Toast.LENGTH_LONG).show()
        }
    }

    fun register_click(view: View){
        val action = GirisYapFragmentDirections.actionGirisYapFragmentToKayitOlFragment()
        Navigation.findNavController(view).navigate(action)
    }
    fun forgot_password_click(view: View){
        val action = GirisYapFragmentDirections.actionGirisYapFragmentToSifremiUnuttumFragment()
        Navigation.findNavController(view).navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}