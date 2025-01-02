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
import com.google.firebase.ktx.Firebase
import com.omeraydin.etkinlikprojesi.databinding.FragmentSifremiUnuttumBinding

class SifremiUnuttumFragment : Fragment() {
    private var _binding: FragmentSifremiUnuttumBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth : FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        auth.setLanguageCode("tr")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSifremiUnuttumBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnResetRequest.setOnClickListener { reset_password(it) }
        binding.fBtnBack.setOnClickListener { back(it) }
    }

    fun reset_password(view: View){
        val email = binding.etxtResetMail.text.toString()
        if(email != ""){
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        println("Doğrulama Maili Gönderildi!")
                        Toast.makeText(requireContext(),"Doğrulama Maili Gönderildi!",Toast.LENGTH_SHORT).show()
                        val action = SifremiUnuttumFragmentDirections.actionSifremiUnuttumFragmentToGirisYapFragment()
                        Navigation.findNavController(view).navigate(action)
                    }
                }.addOnFailureListener {
                    Toast.makeText(requireContext(),"Mail Bulunamadı!",Toast.LENGTH_SHORT).show()
                }
        }else{
            Toast.makeText(requireContext(),"Lütfen Mail Alanını Boş Bırakmayınız!",Toast.LENGTH_SHORT).show()
        }
    }

    fun back(view: View){
        val action = SifremiUnuttumFragmentDirections.actionSifremiUnuttumFragmentToGirisYapFragment()
        Navigation.findNavController(view).navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}