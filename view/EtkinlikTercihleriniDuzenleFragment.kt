package com.omeraydin.etkinlikprojesi.view

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.navigation.Navigation
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.omeraydin.etkinlikprojesi.R
import com.omeraydin.etkinlikprojesi.controller.NotificationWorker
import com.omeraydin.etkinlikprojesi.databinding.FragmentEtkinlikTercihleriniDuzenleBinding
import com.omeraydin.etkinlikprojesi.databinding.FragmentKullaniciBilgileriBinding
import com.omeraydin.etkinlikprojesi.model.CommentEvent
import com.omeraydin.etkinlikprojesi.model.GeneratedEvent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class EtkinlikTercihleriniDuzenleFragment : Fragment() {
    private var _binding: FragmentEtkinlikTercihleriniDuzenleBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    val addedTypesList: ArrayList<String> = arrayListOf()

    val preferedTypesList : ArrayList<String> = arrayListOf()
    val myPreferenceEventsList : ArrayList<GeneratedEvent> = arrayListOf()

    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        db = Firebase.firestore
        set_notification_channel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEtkinlikTercihleriniDuzenleBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ){
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                //izin yok
                println("izin verilmemiş")
                binding.switchNotification.isChecked = false
            }else{
                //izin var
                println("izin verilmiş")
                binding.switchNotification.isChecked = true
            }
        }

        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (isNetworkEnabled || isGpsEnabled) {
            //izin var
            println("izin verilmemiş")
            binding.switchLocation.isChecked = true
        }else{
            //izin yok
            println("izin verilmiş")
            binding.switchLocation.isChecked = false
        }


        binding.switchNotification.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked){
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, "com.omeraydin.etkinlikprojesi")
                startActivity(intent)
            }else{
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, "com.omeraydin.etkinlikprojesi")
                startActivity(intent)
            }
        }

        binding.switchLocation.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked){
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, "com.omeraydin.etkinlikprojesi")
                startActivity(intent)
            }else{
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, "com.omeraydin.etkinlikprojesi")
                startActivity(intent)
            }
        }


        val spinner = binding.spinnerTypeCustomize
        val options = listOf("Tür Seç","Eglence", "Yemek", "Dans", "Spor", "Ogrenim", "Sanat", "Muzik" , "Oyun", "Gezi", "Drama", "Teknoloji")
        spinner_options(spinner,options)


        binding.btnClearType.setOnClickListener { clear_type(it) }
        binding.btnUpdate.setOnClickListener { update_click(it) }

        binding.fBtnBack.setOnClickListener { back(it) }
    }

    fun set_notification_channel() {
        val channelId = "hatirlatici_notification_channel"
        val channelName = "Hatırlatıcı Kanalı"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Hatırlatıcı bildirim kanalı"
            }

            val notificationManager = ContextCompat.getSystemService(
                requireContext(),
                NotificationManager::class.java
            )

            notificationManager?.createNotificationChannel(notificationChannel)
        }
    }

    fun set_alarm(view: View, context: Context, dateString: String) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val parsedDate = dateFormat.parse(dateString)
        val currentDate = Calendar.getInstance().time

        if(parsedDate != null){
            val delay = parsedDate.time - currentDate.time
            if (delay > 0) {
                val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .build()

                WorkManager.getInstance(requireContext()).enqueue(workRequest)
            } else {
                Toast.makeText(requireContext(),"Geçmiş Tarihe Alarm Kuramazsınız!",Toast.LENGTH_LONG).show()
            }
            Toast.makeText(requireContext(),"Alarm Kuruldu",Toast.LENGTH_SHORT).show()
        }
    }
/*
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.isNotificationPolicyAccessGranted) {
            // Bildirim izni verilmiş
        } else {
            // Bildirim izni verilmemiş
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            startActivity(intent)
        }
    }*/

    fun set_startly(userEmail: String){
        val getTypesQuery = db.collection("UsersPrefered")
            .whereEqualTo("user_email",userEmail)

        val typesDocRef = getTypesQuery.get().addOnSuccessListener { value ->
            if (value != null && !value.isEmpty){
                preferedTypesList.clear()
                val documents = value.documents
                val document=documents.get(0)
                val preferedType = document.get("prefered_types") as String
                val preferedDistance = document.getDouble("max_distance")?.toInt()
                val list = preferedType.split(", ")
                for(type in list){
                    preferedTypesList.add(type)
                }
                binding.txtAddingTypes.text = preferedTypesList.toString()
                binding.etxtDistancePrefered.setText(preferedDistance.toString())
            }else{
                binding.txtAddingTypes.text = "Eklenenler:"
            }
        }
    }

    fun compareDatesLegacy(dateString: String) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val parsedDate = dateFormat.parse(dateString)
        val currentDate = Calendar.getInstance().time

        if (parsedDate != null) {
            when {
                parsedDate.before(currentDate) -> {
                    println("Geçmiş tarih.")
                    val differenceMillis = currentDate.time - parsedDate.time
                    println(differenceMillis)
                }
                parsedDate.after(currentDate) -> {
                    println("Gelecek tarih.")
                    val differenceMillis = parsedDate.time - currentDate.time
                    println(differenceMillis)
                }
                else -> println("Bugün.")
            }
        } else {
            println("Tarih formatı hatalı.")
        }
    }

    fun get_prefered_types_and_update_count(userEmail: String){
        val dateTime = "27/12/2024 15:10"
        compareDatesLegacy(dateTime)



        val getTypesQuery = db.collection("UsersPrefered")
            .whereEqualTo("user_email",userEmail)

        val typesDocRef = getTypesQuery.get().addOnSuccessListener { value ->
            if (value != null && !value.isEmpty){
                preferedTypesList.clear()
                val documents = value.documents
                val document=documents.get(0)
                val preferedType = document.get("prefered_types") as String
                val list = preferedType.split(", ")
                for(type in list){
                    preferedTypesList.add(type)
                }
                println(preferedType)
                val query = db.collection("GeneratedEvents")
                    .whereIn("event_type",preferedTypesList)
                    .orderBy("generated_date",Query.Direction.DESCENDING)

                val docRef = query.addSnapshotListener { value, error ->
                    if (error != null) {
                        Toast.makeText(requireContext(),"Listen failed. ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                        println("Listen failed. ${error.localizedMessage}")
                        //return@addSnapshotListener
                    }else{
                        if (value != null && !value.isEmpty){
                            myPreferenceEventsList.clear()
                            val documents = value.documents
                            for (document in documents){
                                val eventID = document.id
                                val createrMail = document.get("event_createrMail") as String
                                val createrName = document.get("event_createrName") as String
                                val head = document.get("event_head") as String
                                val explanation = document.get("event_explanation") as String
                                val shortExplanation = document.get("event_shortExplanation") as String
                                val type = document.get("event_type") as String
                                val location = document.get("event_location") as String
                                val latitude = document.getDouble("event_location_lat")?.toDouble() ?:0.0
                                val longitude = document.getDouble("event_location_long")?.toDouble() ?:0.0
                                val date = document.get("event_date") as String
                                val clock = document.get("event_clock") as String
                                val base64String = document.get("event_image") as String
                                val rate = document.getDouble("event_rate")?.toDouble() ?: 0.0
                                val generated_date = document.getTimestamp("generated_date") as Timestamp
                                val generatedEvent = GeneratedEvent(eventID,createrMail,createrName,head,explanation,shortExplanation,
                                    type,location,latitude,longitude,date,clock,base64String,rate,generated_date)
                                myPreferenceEventsList.add(generatedEvent)
                                println(generatedEvent.head + " / " + generatedEvent.type)
                            }
                            val userEmail = auth.currentUser?.email
                            if(userEmail != null){
                                add_myPreferedEventsCount(userEmail)
                            }
                        }
                    }
                }
            }
        }
    }

    fun add_myPreferedEventsCount(userEmail: String){
        println(myPreferenceEventsList.size)
        val count = myPreferenceEventsList.size

        val query = db.collection("UsersPrefered")
            .whereEqualTo("user_email",userEmail)

        val docRef = query.get().addOnSuccessListener { value ->
            if (value != null && !value.isEmpty){
                val documents = value.documents
                val document=documents.get(0)
                val preferedID = document.id
                val userRef = db.collection("UsersPrefered").document(preferedID) // Belgeyi referans al
                userRef.update("prefered_event_count", count)
                    .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(),"Uygun Etkinlikler Arandı\n${count} Adet Size Uygun Etkinlik Bulunuyor!", Toast.LENGTH_SHORT).show()
                    } else {
                        println("Hata: ${task.exception}")
                    }
                }
            }
        }
    }

    /*
    fun deleteDocument(collectionName: String, documentId: String) {
        val db = Firebase.firestore

        db.collection(collectionName)
            .document(documentId)
            .delete()
            .addOnSuccessListener {
                println("Belge başarıyla silindi.")
            }
            .addOnFailureListener { e ->
                println("Belge silinirken hata oluştu: ${e.message}")
            }
    }
*/

    fun back(view: View){
        val action = EtkinlikTercihleriniDuzenleFragmentDirections.actionEtkinlikTercihleriniDuzenleFragmentToAnaSayfaFragment()
        Navigation.findNavController(requireView()).navigate(action)
    }

    fun update_click(view: View){
        var typesString = binding.txtAddingTypes.text.toString()
        val maxDistanceString = binding.etxtDistancePrefered.text.toString()
        try {
            val maxDistance = maxDistanceString.toInt()
            if(typesString.length>2 && typesString != "Tür Seç"){
                typesString = typesString.substring(1,typesString.length-1)
                val userEmail = auth.currentUser?.email
                if(userEmail != null && typesString != "" && maxDistanceString != ""){
                    check_and_add_prefered(userEmail,typesString, maxDistance)
                }
            }else{
                Toast.makeText(requireContext(),"Lütfen geçerli değerler giriniz!",Toast.LENGTH_LONG).show()
            }
        }catch (e: Exception){
            Toast.makeText(requireContext(),"Uzaklık Değeri Bir Tam Sayı Olmalıdır!",Toast.LENGTH_SHORT).show()
        }
    }

    fun check_and_add_prefered(userEmail: String, typesString: String, maxDistance: Int){
        val query = db.collection("UsersPrefered")
        .whereEqualTo("user_email",userEmail)
        .count()
        val docRef = query.get(AggregateSource.SERVER).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val snapshot = task.result
                val count =  snapshot.count.toInt()
                if(count>0){
                    find_perefered_id_and_update(userEmail,typesString, maxDistance)
                }else{
                    insert_perefered_firebase(userEmail,typesString, maxDistance)
                }
            } else {
                println("Count failed: "+ task.getException())
            }
        }
    }

    fun find_perefered_id_and_update(userEmail: String, typesString: String, maxDistance: Int){
        if(auth.currentUser != null && userEmail != "" && typesString != "" && maxDistance != 0){
            val query = db.collection("UsersPrefered")
                .whereEqualTo("user_email",userEmail)

            val docRef = query.get().addOnSuccessListener { value ->
                if (value != null && !value.isEmpty){
                    val documents = value.documents
                    val document=documents.get(0)
                    val preferedID = document.id
                    update_prefered_firebase(preferedID, typesString, maxDistance)
                }
            }
        }else{
            val alertDialog = AlertDialog.Builder(
                requireContext(),
                R.style.CustomAlertDialog // Özel tema
            )
                .setTitle("Uyarı")
                .setMessage("Eksik Veri!")
                .setPositiveButton("Tamam") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
            alertDialog.show()
        }
    }

    fun update_prefered_firebase(preferedID: String, typesString: String, maxDistance: Int){
        val userRef = db.collection("UsersPrefered").document(preferedID) // Belgeyi referans al
        userRef.update("prefered_types", typesString,"max_distance",maxDistance).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(requireContext(),"Tercihleriniz Güncellendi!", Toast.LENGTH_SHORT).show()
            } else {
                println("Hata: ${task.exception}")
            }
        }
        val userEmail = auth.currentUser?.email
        if(userEmail != null){
            get_prefered_types_and_update_count(userEmail)
        }
    }

    fun insert_perefered_firebase(userEmail: String, typesString: String, maxDistance:Int){
        if(auth.currentUser != null && userEmail != "" && typesString != "" && maxDistance != 0){
            val preferedMap = hashMapOf<String, Any>()
            preferedMap.put("user_email",userEmail)
            preferedMap.put("prefered_types",typesString)
            preferedMap.put("max_distance",maxDistance)
            preferedMap.put("prefered_event_count",0)

            db.collection("UsersPrefered").add(preferedMap).addOnSuccessListener { documentReference ->
                Toast.makeText(requireContext(),"Tercihleriniz Oluşturuldu!", Toast.LENGTH_SHORT).show()
                get_prefered_types_and_update_count(userEmail)
            }.addOnFailureListener { exception ->
                Toast.makeText(requireContext(),exception.localizedMessage, Toast.LENGTH_LONG).show()
            }

        }else {
            val alertDialog = AlertDialog.Builder(
                requireContext(),
                R.style.CustomAlertDialog // Özel tema
            )
                .setTitle("Uyarı")
                .setMessage("Eksik Veri!")
                .setPositiveButton("Tamam") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
            alertDialog.show()
        }
    }

    fun spinner_options(spinner: Spinner, options: List<String>){
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
                var control = true
                val type = options[position]
                for (x in addedTypesList){
                    if(x == type){
                        control = false
                        break
                    }
                }
                val userEMail = auth.currentUser?.email
                if(control){
                    addedTypesList.add(type)
                }
                if(type == "Tür Seç" && userEMail != null){
                    addedTypesList.clear()
                    set_startly(userEMail)
                    binding.txtAddingTypes.text = "Eklenenler:"
                }else{
                    binding.txtAddingTypes.text = addedTypesList.toString()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Hiçbir öğe seçilmediğinde yapılacak işlem
            }
        }
    }

    fun clear_type(view: View){
        addedTypesList.clear()
        val userEMail = auth.currentUser?.email
        if(userEMail != null){
            addedTypesList.clear()
            set_startly(userEMail)
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}