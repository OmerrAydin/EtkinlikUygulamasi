package com.omeraydin.etkinlikprojesi.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.Navigation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import com.omeraydin.etkinlikprojesi.databinding.FragmentKayitOlBinding

class KayitOlFragment : Fragment() {
    private var _binding: FragmentKayitOlBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        auth.setLanguageCode("tr")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentKayitOlBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnUserRegister.setOnClickListener { register_click(it) }

        binding.fBtnBack.setOnClickListener { back(it) }
    }

    fun back(view: View){
        val action = KayitOlFragmentDirections.actionKayitOlFragmentToGirisYapFragment()
        Navigation.findNavController(view).navigate(action)
    }

    fun register_click(view: View){
        val email = binding.etxtMail.text.toString()
        val password = binding.etxtRegisterPassword.text.toString()
        val passwordAgain = binding.etxtRegisterPasswordAgain.text.toString()
        val name = binding.etxtName.text.toString().trim()

        if(email != "" && password != "" && password == passwordAgain){
            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if(task.isSuccessful){
                    val profileUpdate = userProfileChangeRequest {
                        displayName = name
                    }
                    auth.currentUser?.updateProfile(profileUpdate)?.addOnCompleteListener {
                        auth.currentUser!!.sendEmailVerification().addOnCompleteListener {
                            if(it.isSuccessful){
                                Toast.makeText(requireContext(),"Kayıt Başarılı! Mailinizi Kontrol Ediniz!",Toast.LENGTH_SHORT).show()
                                val action = KayitOlFragmentDirections.actionKayitOlFragmentToGirisYapFragment()
                                Navigation.findNavController(view).navigate(action)
                            }
                        }
                    }
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(requireContext(),exception.localizedMessage,Toast.LENGTH_LONG).show()
            }
        }else{
            Toast.makeText(requireContext(),"Eksik veya Uyuşmayan Veri!",Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}