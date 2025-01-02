package com.omeraydin.etkinlikprojesi.view

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.location.Address
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.omeraydin.etkinlikprojesi.R
import com.omeraydin.etkinlikprojesi.databinding.FragmentEtkinlikOlusturmaBinding
import java.io.ByteArrayOutputStream
import java.util.Calendar

class EtkinlikOlusturmaFragment : Fragment() {
    private var _binding: FragmentEtkinlikOlusturmaBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db : FirebaseFirestore

    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    var selectedPicture : Uri? = null
    var selectedBitmap : Bitmap? = null

    private var type = ""
    private var coordinatLat: Double? = 0.0
    private var coordinatLong: Double? = 0.0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
        db = Firebase.firestore
        registerLaunchers()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEtkinlikOlusturmaBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etxtEventLocate.isEnabled = false

        binding.btnCreateEvent.setOnClickListener { create_event_clicked(it) }

        binding.imageEvent.setOnClickListener { select_image_clicked(it) }

        binding.fBtnCreateBack.setOnClickListener { back(it) }

        binding.etxtEventDate.setOnClickListener { date_click(it) }

        binding.etxtEventClock.setOnClickListener { time_click(it) }

        binding.btnMap.setOnClickListener {
            val intent = Intent(requireActivity(), LocationMapsActivity::class.java)
            startActivityForResult(intent,100)
        }

        val spinner = binding.spinnerEventType

        // Spinner için veri seti oluştur
        val options = listOf("Eglence", "Yemek", "Dans", "Spor", "Ogrenim", "Sanat", "Muzik" , "Oyun", "Gezi", "Drama", "Teknoloji")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, options)
        //adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Adapter'ı Spinner'a set et
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedItem = options[position]
                type = selectedItem

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Hiçbir öğe seçilmediğinde yapılacak işlem
            }
        }
    }

    fun back(view: View){
        val action = EtkinlikOlusturmaFragmentDirections.actionEtkinlikOlusturmaFragmentToAnaSayfaFragment()
        Navigation.findNavController(requireView()).navigate(action)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            val address = data?.getStringExtra("address")
            coordinatLat = data?.getDoubleExtra("coordinatLatitude",0.0)
            coordinatLong = data?.getDoubleExtra("coordinatLongitude",0.0)
            //println("lat:${coordinatLat}, long:${coordinatLong}")
            binding.etxtEventLocate.setText(address)
            binding.etxtEventLocate.isEnabled = true
        }
    }

    fun date_click(view: View){
        val calendar = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                // Seçilen tarihi işleyin
                val selectedDate = "$dayOfMonth/${month + 1}/$year"
                binding.etxtEventDate.setText(selectedDate)
            },
            calendar.get(Calendar.YEAR),  // Başlangıç yılı
            calendar.get(Calendar.MONTH), // Başlangıç ayı
            calendar.get(Calendar.DAY_OF_MONTH) // Başlangıç günü
        )

        datePickerDialog.show()
    }

    fun time_click(view: View){
        val timePickerDialog = TimePickerDialog(
            requireContext(), // Context, burada fragment içinde olduğunuz için requireContext()
            { _, hourOfDay, minute ->
                // Saat ve dakika seçildiğinde yapılacak işlem
                val selectedTime = String.format("%02d:%02d", hourOfDay, minute)
                // Örneğin, TextView'a yazdırma
                binding.etxtEventClock.setText(selectedTime)
            },
            12, // Başlangıç saati (12:00)
            0,  // Başlangıç dakikası (00)
            true // 24 saat formatı (true ise 24 saat formatı, false ise 12 saat formatı)
        )

        // Dialog'u göster
        timePickerDialog.show()
    }

    fun create_event_clicked(view: View){
        val createrMail = auth.currentUser!!.email.toString()
        val createrName = auth.currentUser!!.displayName.toString()
        val head = binding.etxtEventHead.text.toString()
        val shortExplanation = binding.etxtEventShortExplanation.text.toString()
        val explanation = binding.etxtEventExplanation.text.toString()
        val location = binding.etxtEventLocate.text.toString()
        val date = binding.etxtEventDate.text.toString()
        val clock = binding.etxtEventClock.text.toString()
        val rate = 0

        if(auth.currentUser != null && selectedBitmap != null && createrMail != "" && createrName != "" && head != "" && shortExplanation != "" && explanation != "" && type != "" && location != "" && date != "" && clock != "" && coordinatLat != 0.0 && coordinatLong != 0.0 && coordinatLat != null && coordinatLong != null){
            val lessBitmap = create_less_bitmap(selectedBitmap!!,300)
            val outputStream = ByteArrayOutputStream()
            lessBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteArray = outputStream.toByteArray()
            val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)

            val generatedEventMap = hashMapOf<String, Any>()
            generatedEventMap.put("event_createrMail",createrMail)
            generatedEventMap.put("event_createrName",createrName)
            generatedEventMap.put("event_head",head.uppercase())
            generatedEventMap.put("event_explanation",explanation)
            generatedEventMap.put("event_shortExplanation",shortExplanation)
            generatedEventMap.put("event_type",type)
            generatedEventMap.put("event_location",location)
            generatedEventMap.put("event_location_lat", coordinatLat!!)
            generatedEventMap.put("event_location_long",coordinatLong!!)
            generatedEventMap.put("event_date",date)
            generatedEventMap.put("event_clock",clock)
            generatedEventMap.put("event_image",base64String)
            generatedEventMap.put("event_rate",rate)
            generatedEventMap.put("generated_date",Timestamp.now())

            db.collection("GeneratedEvents").add(generatedEventMap).addOnSuccessListener { documentReference ->
                val alertDialog = AlertDialog.Builder(
                    requireContext(),
                    R.style.CustomAlertDialog // Özel tema
                )
                    .setTitle("Başarılı")
                    .setMessage("Etkinlik Başarıyla Oluşturuldu!")
                    .setPositiveButton("Tamam") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                alertDialog.show()
                val action = EtkinlikOlusturmaFragmentDirections.actionEtkinlikOlusturmaFragmentToAnaSayfaFragment()
                Navigation.findNavController(view).navigate(action)
            }.addOnFailureListener { exception ->
                Toast.makeText(requireContext(),exception.localizedMessage,Toast.LENGTH_LONG).show()
            }

        }else{
            val alertDialog = AlertDialog.Builder(
                requireContext(),
                R.style.CustomAlertDialog // Özel tema
            )
                .setTitle("Uyarı")
                .setMessage("Lütfen Bilgileri Eksiksiz Doldurunuz!")
                .setPositiveButton("Tamam") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
            alertDialog.show()
        }
    }

    fun select_image_clicked(view: View){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ){
            if(ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED){
                //İzin yok
                if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),Manifest.permission.READ_MEDIA_IMAGES)){
                    Snackbar.make(view,"Resim seçebilmeniz için galeriye erişim izni verilmesi gerekmektedir!",Snackbar.LENGTH_INDEFINITE).setAction("İzin Ver"
                        ,View.OnClickListener {
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                        }).show()
                }else{
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }else{
                //izin var
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
        }else{
            if(ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                //İzin yok
                if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),Manifest.permission.READ_EXTERNAL_STORAGE)){
                    Snackbar.make(view,"Resim seçebilmeniz için galeriye erişim izni verilmesi gerekmektedir!",Snackbar.LENGTH_INDEFINITE).setAction("İzin Ver"
                        ,View.OnClickListener {
                            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }).show()
                }else{
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }else{
                //izin var
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
        }
    }

    fun registerLaunchers(){
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            if (result.resultCode == RESULT_OK){
                val intentFromResult = result.data
                if(intentFromResult != null){
                    selectedPicture = intentFromResult.data
                    try {
                        if(Build.VERSION.SDK_INT >= 28){
                            val source = ImageDecoder.createSource(requireActivity().contentResolver,selectedPicture!!)
                            selectedBitmap = ImageDecoder.decodeBitmap(source)
                            binding.imageEvent.setImageBitmap(selectedBitmap)
                        }else{
                            selectedBitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver,selectedPicture)
                            binding.imageEvent.setImageBitmap(selectedBitmap)
                        }
                    }catch (e : Exception){
                        e.printStackTrace()
                    }
                }
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->
            if(result){
                //izin verildi
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }else{
                Toast.makeText(requireContext(),"Galeriye erişim iznini reddettiniz!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun create_less_bitmap(usersSelectedBitmap : Bitmap, maxSize : Int) : Bitmap{
        var width = usersSelectedBitmap.width
        var height = usersSelectedBitmap.height

        val bitmapRatio : Double = width.toDouble() / height.toDouble()

        if(bitmapRatio > 1){
            //Görsel yatay
            width = maxSize
            height = (width/bitmapRatio).toInt()
        }else{
            //Görsel dikey
            height = maxSize
            width = (height*bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(usersSelectedBitmap,width,height,true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}