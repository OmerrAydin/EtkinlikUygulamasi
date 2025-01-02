package com.omeraydin.etkinlikprojesi.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import com.omeraydin.etkinlikprojesi.databinding.FragmentKullaniciBilgileriBinding


class KullaniciBilgileriFragment : Fragment() {
    private var _binding: FragmentKullaniciBilgileriBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentKullaniciBilgileriBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentUser = auth.currentUser!!
        val name = currentUser.displayName
        val email = currentUser.email


        binding.etxtSettingsName.setText(name)
        binding.etxtSettingsEmail.setText(email)
        binding.btnSettingsSave.setOnClickListener { save_click(it) }
        binding.fBtnBack.setOnClickListener { back(it) }
        binding.btnPasswordChanged.setOnClickListener { change_password(it) }

        binding.btnPermissionManagement.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", "com.omeraydin.etkinlikprojesi", null)
            intent.data = uri
            startActivity(intent)
        }
    }

    fun back(view: View){
        val action = KullaniciBilgileriFragmentDirections.actionKullaniciBilgileriFragmentToAnaSayfaFragment()
        Navigation.findNavController(view).navigate(action)
    }

    fun change_password(view: View){
        val email = auth.currentUser?.email ?: ""
        if(email != ""){
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(),"Parola Değişikliği Talebiniz İçin Mail Gönderildi!",Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    fun save_click(view: View){
        val name = binding.etxtSettingsName.text.toString()
        val email = binding.etxtSettingsEmail.text.toString()
        val user = auth.currentUser!!

        if(name != "" && name != user.displayName){
            val profileUpdate = userProfileChangeRequest {
                displayName = name
            }
            user.updateProfile(profileUpdate).addOnCompleteListener { task ->
                if (task.isSuccessful){
                    binding.etxtSettingsName.setText(user.displayName)
                    Toast.makeText(requireContext(), "İsim Güncellendi", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        }else{
            if(name != user.displayName){
                Toast.makeText(requireContext(), "HATALI İSİM!", Toast.LENGTH_SHORT).show()
                binding.etxtSettingsName.setText(user.displayName)
            }
        }

        if(email != "" && email != user.email){
            user.verifyBeforeUpdateEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        binding.etxtSettingsEmail.setText(user.email)
                        Toast.makeText(requireContext(), "E-Mail'in Güncellenmesi İçin Mailinizi Kontrol Ediniz!", Toast.LENGTH_SHORT).show()
                        auth.signOut()
                        val intent = Intent(requireActivity(), MainActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    }
                }.addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_SHORT).show()
                    println(exception)
                }
        }else{
            if(email != user.email){
                binding.etxtSettingsEmail.setText(user.email)
                Toast.makeText(requireContext(), "HATA EMAİL!", Toast.LENGTH_SHORT).show()
            }
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}