package com.omeraydin.etkinlikprojesi.view

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatActivity.LOCATION_SERVICE
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.facebook.AccessToken
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
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
import com.omeraydin.etkinlikprojesi.adapter.GeneratedEventAdapter
import com.omeraydin.etkinlikprojesi.controller.FacebookActivity
import com.omeraydin.etkinlikprojesi.databinding.FragmentAnaSayfaBinding
import com.omeraydin.etkinlikprojesi.model.AcceptedEvent
import com.omeraydin.etkinlikprojesi.model.EventForMaps
import com.omeraydin.etkinlikprojesi.model.GeneratedEvent
import java.io.Serializable
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

@Suppress("DEPRECATION")
class AnaSayfaFragment : Fragment(), PopupMenu.OnMenuItemClickListener {
    private var _binding: FragmentAnaSayfaBinding? = null
    private val binding get() = _binding!!

    private lateinit var popup : PopupMenu

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var spinner: Spinner

    private var adapter: GeneratedEventAdapter ?= null

    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var permissionLauncherLocation: ActivityResultLauncher<String>

    val generatedEventsList : ArrayList<GeneratedEvent> = arrayListOf()
    val acceptedEventsList : ArrayList<AcceptedEvent> = arrayListOf()

    val preferedTypesList : ArrayList<String> = arrayListOf()
    val myPreferenceEventsList : ArrayList<GeneratedEvent> = arrayListOf()

    var specialViewEnabled: Boolean = false

    private lateinit var locationListener: LocationListener
    private lateinit var locationManager: LocationManager
    private var currentLocation: LatLng ?= null

    var maxDistance = 5000
    var preferedMaxDistance = maxDistance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        db = Firebase.firestore
        registerLaunchers()
        registerLaunchersLocation()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAnaSayfaBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        //Popup menu
        popup = PopupMenu(requireContext(),binding.fBtnMenu)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.popup_menu_for_button,popup.menu)

        binding.fBtnMenu.setOnClickListener { menuButtonClicked(it) }
        popup.setOnMenuItemClickListener(this)

        //Toolbar
        toolbar = view.findViewById(R.id.toolbar)
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        setHasOptionsMenu(true)

        binding.toolbar.setOnMenuItemClickListener { onToolbarMenuClick(it) }

        // Geri tuşuna tıklanması durumunda yapılacak işlemi engelle
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            // Uygulamayı arka plana al
            requireActivity().moveTaskToBack(true)
        }


        //Recycler ve veri alma işlemleri
        get_events_fireStore()
        adapter = GeneratedEventAdapter(generatedEventsList)
        binding.rViewEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rViewEvents.adapter = adapter


        createNotificationChannel(requireContext())
        //check_permission_and_send_notification()
        val userEmail = auth.currentUser?.email
        if(userEmail != null){
            get_prefered_types_and_update_count(userEmail)
            get_accepted_events_and_date_compare(userEmail)
        }

        locationManager = requireContext().getSystemService(LOCATION_SERVICE) as LocationManager


        locationListener = object : LocationListener{
            override fun onLocationChanged(location: Location) {
                currentLocation = LatLng(location.latitude,location.longitude)
                println("${location.latitude}, ${location.longitude}")
                get_events_fireStore()
            }

        }

        if(ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),Manifest.permission.ACCESS_FINE_LOCATION)){
                Snackbar.make(binding.root,"Konumunuza Erişebilmemiz İçin İzin Vermeniz Gerekiyor",Snackbar.LENGTH_INDEFINITE).setAction("İzin Ver"){
                    //izin iste
                    permissionLauncherLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }.show()
            }else{
                //izin isteyeceğiz
                permissionLauncherLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }else{
            //izin verilmiş
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,60000,1000f,locationListener)
            val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if(lastLocation != null){
                currentLocation = LatLng(lastLocation.latitude,lastLocation.longitude)
                println("${currentLocation?.latitude}, ${currentLocation?.longitude}")
            }
        }
    }

    fun get_accepted_events_and_date_compare(userEmail: String){
        val query = db.collection("JoiningEvents")
            .whereEqualTo("joiner_email",userEmail)

        val docRef = query.addSnapshotListener { value, error ->
            if (error != null) {
                Toast.makeText(requireContext(),"Listen failed. ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                println("Listen failed. ${error.localizedMessage}")
                //return@addSnapshotListener
            }else{
                if (value != null && !value.isEmpty){
                    acceptedEventsList.clear()
                    val documents = value.documents
                    for (document in documents){
                        val eventID = document.id
                        val joinerEmail = document.get("joiner_email") as String
                        val eventHead = document.get("event_head") as String
                        val eventType = document.get("event_type") as String
                        val eventDate = document.get("event_date") as String
                        val eventClock = document.get("event_clock") as String
                        val eventLocate = document.get("event_locate") as String
                        val generatedDate = document.getTimestamp("generated_date") as Timestamp
                        val acceptedEvent = AcceptedEvent(eventID,joinerEmail,eventHead,eventType,eventDate,eventClock,
                            eventLocate,generatedDate)
                        acceptedEventsList.add(acceptedEvent)
                    }

                    for (x in acceptedEventsList){
                        val datetime = x.eventDate + " " +x.eventClock
                        compare_dates_legacy(requireContext(),datetime)
                    }
                }
            }
        }
    }

    fun compare_dates_legacy(context: Context, dateString: String) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val parsedDate = dateFormat.parse(dateString)
        val currentDate = Calendar.getInstance().time

        val userEmail = auth.currentUser?.email

        val eventKey = "eventID_$userEmail"
        val sharedPreferences = context.getSharedPreferences("EventNotifications", Context.MODE_PRIVATE)

        if (parsedDate != null) {
            when {
                parsedDate.before(currentDate) -> {
                    //println("Gecmis tarih.")
                    sharedPreferences.edit().putBoolean(eventKey, false).apply() // Bildirim durumunu kaydet
                }
                parsedDate.after(currentDate) -> {
                    //println("Gelecek tarih.")
                    val differenceMillis = parsedDate.time - currentDate.time

                    if (differenceMillis <= 90000000) { // Yaklaşık 1 gün

                        val alreadyNotified = sharedPreferences.getBoolean(eventKey, false)
                        if (!alreadyNotified) {
                            check_permission_and_send_notification_for_closed_event()
                            sharedPreferences.edit().putBoolean(eventKey, true).apply() // Bildirim durumunu kaydet
                        } else {
                            //println("Bildirim daha önce gönderildi.")
                        }
                    }
                }
                else -> println("Bugün.")
            }
        } else {
            println("Tarih formatı hatalı.")
        }
    }

    fun get_prefered_types_and_update_count(userEmail: String){
        val getTypesQuery = db.collection("UsersPrefered")
            .whereEqualTo("user_email",userEmail)

        val typesDocRef = getTypesQuery.get().addOnSuccessListener { value ->
            if (value != null && !value.isEmpty){
                preferedTypesList.clear()
                val documents = value.documents
                val document=documents.get(0)
                val preferedType = document.get("prefered_types") as String
                val maxDistanceFirebase = document.getDouble("max_distance")?.toInt()
                val count = document.getDouble("prefered_event_count")?.toInt()
                val list = preferedType.split(", ")
                if (maxDistanceFirebase != null) {
                    preferedMaxDistance = maxDistanceFirebase
                }
                for(type in list){
                    preferedTypesList.add(type)
                }
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
                                val countFinal = myPreferenceEventsList.size
                                if(count != null){
                                    if(countFinal>count){
                                        //İzin iste
                                        check_permission_and_send_notification()
                                    }
                                }
                                //println(generatedEvent.head + " / " + generatedEvent.type)
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
        val count = myPreferenceEventsList.size

        val query = db.collection("UsersPrefered")
            .whereEqualTo("user_email",userEmail)

        val docRef = query.get().addOnSuccessListener { value ->
            if (value != null && !value.isEmpty){
                val documents = value.documents
                val document=documents.get(0)
                val preferedID = document.id
                val userRef = db.collection("UsersPrefered").document(preferedID) // Belgeyi referans al
                userRef.update("prefered_event_count", count).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        //Toast.makeText(requireContext(),"Uygun Etkinlikler Arandı\n${count} Adet Size Uygun Etkinlik Bulunuyor!", Toast.LENGTH_SHORT).show()
                    } else {
                        println("Hata: ${task.exception}")
                    }
                }
            }
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "event_channel_id"
            val channelName = "Channel"
            val channelDescription = "Message"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }

            // NotificationManager ile kanalı kaydedin
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun check_permission_and_send_notification_for_closed_event() {
        val channelId = "event_channel_id"
        val notificationId = 3

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ){
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // İzin verilmemişse kullanıcıdan izin iste
                if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),Manifest.permission.POST_NOTIFICATIONS)){
                    Snackbar.make(requireView(),"Etkinliklerinizi hatırlatabilmemiz için bildirimlere izin vermeniz gerekiyor!",
                        Snackbar.LENGTH_INDEFINITE).setAction("İzin Ver"
                        ,View.OnClickListener {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }).show()
                }else{
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }else{
                //izin var
                val notification = update_closed_event_notification(channelId)
                with(NotificationManagerCompat.from(requireContext())) {
                    notify(notificationId, notification)
                }
            }
        }
    }

    fun update_closed_event_notification(channelId: String) : Notification {
        val notification = NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Etkinlik")
            .setContentText("Etkinliğinize 1 günden az kaldı. Kontrol etmeyi unutmayın.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        return  notification
    }

    fun check_permission_and_send_notification() {
        val channelId = "event_channel_id"
        val notificationId = 2

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ){
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // İzin verilmemişse kullanıcıdan izin iste
                if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),Manifest.permission.POST_NOTIFICATIONS)){
                    Snackbar.make(requireView(),"Etkinliklerinizi hatırlatabilmemiz için bildirimlere izin vermeniz gerekiyor!",
                        Snackbar.LENGTH_INDEFINITE).setAction("İzin Ver"
                        ,View.OnClickListener {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }).show()
                }else{
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }else{
                //izin var
                val notification = update_added_event_notification(channelId)
                with(NotificationManagerCompat.from(requireContext())) {
                    notify(notificationId, notification)
                }
            }
        }
    }

    fun update_added_event_notification(channelId: String) : Notification {
        val notification = NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Etkinlik")
            .setContentText("Tercihlerinize Uygun Yeni Etkinlikler Oluşturuldu. Özelleştirilmiş Görünüme Alıp Kontrol Etmeyi Unutmayın.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        return  notification
    }

    fun update_request_confirm_notification(channelId: String) : Notification{
        val notification = NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("Bildirimler")
            .setContentText("Bildirimler Açık Olarak Ayarlandı. Yaklaşan Etkinlikerinizi Size Hatırlatmaktan Mutluluk Duyacağız :)")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        return  notification
    }

    fun registerLaunchers(){
        val channelId = "app_channel_id"
        val notificationId = 1
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->
            if(result){
                //izin verildi
                val notification = update_request_confirm_notification(channelId)
                with(NotificationManagerCompat.from(requireContext())) {
                    if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED){
                        notify(notificationId, notification)
                    }
                }
            }else{
                Toast.makeText(requireContext(),"Bildirim iznini reddettiniz!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun registerLaunchersLocation(){
        permissionLauncherLocation = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->
            if(result){
                if(ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,5f,locationListener)
                    val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if(lastLocation != null){
                        currentLocation = LatLng(lastLocation.latitude,lastLocation.longitude)
                    }
                }
            }else{
                Toast.makeText(requireContext(),"Konum İznini Reddettiniz!",Toast.LENGTH_SHORT).show()
            }
        }
    }


    fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371e3 // Dünya'nın yarıçapı (metre)
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
                Math.cos(phi1) * Math.cos(phi2) *
                Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return R * c // Mesafe (metre cinsinden)
    }

    fun get_events_fireStore(){
        println(specialViewEnabled)
        println(currentLocation)
        if(specialViewEnabled == false || preferedTypesList.isEmpty()){
            val query = db.collection("GeneratedEvents")
                //.whereIn("event_type",preferedTypesList)
                .orderBy("generated_date",Query.Direction.DESCENDING)

            val docRef = query.addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(requireContext(),"Listen failed. ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                    println("Listen failed. ${error.localizedMessage}")
                    //return@addSnapshotListener
                }else{
                    if (value != null && !value.isEmpty){
                        generatedEventsList.clear()
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
                            val check = check_out_of_date_event(generatedEvent)
                            if(currentLocation != null){
                                val distance = haversine(currentLocation!!.latitude, currentLocation!!.longitude,latitude,longitude)
                                if(check && distance<maxDistance){
                                    //println(generatedEvent.head)
                                    generatedEventsList.add(generatedEvent)
                                }
                            }else{
                                if(check){
                                    //println(generatedEvent.head)
                                    generatedEventsList.add(generatedEvent)
                                }
                            }
                        }
                        adapter?.notifyDataSetChanged()
                    }
                }
            }
        }else{
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
                        generatedEventsList.clear()
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
                            val check = check_out_of_date_event(generatedEvent)
                            if(currentLocation != null){
                                val distance = haversine(currentLocation!!.latitude,currentLocation!!.longitude,latitude,longitude)
                                if(check && distance<preferedMaxDistance){
                                    //println(generatedEvent.head)
                                    generatedEventsList.add(generatedEvent)
                                }
                            }else{
                                if(check){
                                    //println(generatedEvent.head)
                                    generatedEventsList.add(generatedEvent)
                                }
                            }
                        }
                        adapter?.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    fun get_events_fireStore_by_search(search: String){
        if(specialViewEnabled == false){
            val query = db.collection("GeneratedEvents")
                .whereGreaterThanOrEqualTo("event_head", search.uppercase())
                .whereLessThan("event_head", search.uppercase() + "\uf8ff")
                .orderBy("generated_date",Query.Direction.DESCENDING)

            val docRef = query.addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(requireContext(),"Listen failed. ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                    println("Listen failed. ${error.localizedMessage}")
                    //return@addSnapshotListener
                }else{
                    if (value != null && !value.isEmpty){
                        generatedEventsList.clear()
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
                            val check = check_out_of_date_event(generatedEvent)
                            if(currentLocation != null){
                                val distance = haversine(currentLocation!!.latitude,currentLocation!!.longitude,latitude,longitude)
                                if(check && distance<maxDistance){
                                    //println(generatedEvent.head)
                                    generatedEventsList.add(generatedEvent)
                                }
                            }else{
                                if(check){
                                    //println(generatedEvent.head)
                                    generatedEventsList.add(generatedEvent)
                                }
                            }
                        }
                        adapter?.notifyDataSetChanged()
                    }
                }
            }
        }else{
            val query = db.collection("GeneratedEvents")
                .whereIn("event_type",preferedTypesList)
                .whereGreaterThanOrEqualTo("event_head", search.uppercase())
                .whereLessThan("event_head", search.uppercase() + "\uf8ff")
                .orderBy("generated_date",Query.Direction.DESCENDING)

            val docRef = query.addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(requireContext(),"Listen failed. ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                    println("Listen failed. ${error.localizedMessage}")
                    //return@addSnapshotListener
                }else{
                    if (value != null && !value.isEmpty){
                        generatedEventsList.clear()
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
                            val check = check_out_of_date_event(generatedEvent)
                            if(currentLocation != null){
                                val distance = haversine(currentLocation!!.latitude,currentLocation!!.longitude,latitude,longitude)
                                if(check && distance<preferedMaxDistance){
                                    //println(generatedEvent.head)
                                    generatedEventsList.add(generatedEvent)
                                }
                            }else{
                                if(check){
                                    //println(generatedEvent.head)
                                    generatedEventsList.add(generatedEvent)
                                }
                            }
                        }
                        adapter?.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    fun get_events_fireStore_by_type(type: String){
        if(specialViewEnabled == false){
            val query = db.collection("GeneratedEvents")
                .whereGreaterThanOrEqualTo("event_type", type)
                .whereLessThan("event_type", type + "\uf8ff")
                .orderBy("generated_date",Query.Direction.DESCENDING)

            generatedEventsList.clear()
            adapter?.notifyDataSetChanged()

            val docRef = query.addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(requireContext(),"Listen failed. ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                    println("Listen failed. ${error.localizedMessage}")
                    //return@addSnapshotListener
                }else{
                    if (value != null && !value.isEmpty){
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
                            val check = check_out_of_date_event(generatedEvent)
                            if(currentLocation != null){
                                val distance = haversine(currentLocation!!.latitude,currentLocation!!.longitude,latitude,longitude)
                                if(check && distance<maxDistance){
                                    //println(generatedEvent.head)
                                    generatedEventsList.add(generatedEvent)
                                }
                            }else{
                                if(check){
                                    //println(generatedEvent.head)
                                    generatedEventsList.add(generatedEvent)
                                }
                            }
                        }
                        adapter?.notifyDataSetChanged()
                    }
                }
            }
        }else{
            val query = db.collection("GeneratedEvents")
                .whereIn("event_type",preferedTypesList)
                .whereGreaterThanOrEqualTo("event_type", type)
                .whereLessThan("event_type", type + "\uf8ff")
                .orderBy("generated_date",Query.Direction.DESCENDING)

            generatedEventsList.clear()
            adapter?.notifyDataSetChanged()

            val docRef = query.addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(requireContext(),"Listen failed. ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                    println("Listen failed. ${error.localizedMessage}")
                    //return@addSnapshotListener
                }else{
                    if (value != null && !value.isEmpty){
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
                            val check = check_out_of_date_event(generatedEvent)
                            if(currentLocation != null){
                                val distance = haversine(currentLocation!!.latitude,currentLocation!!.longitude,latitude,longitude)
                                if(check && distance<preferedMaxDistance){
                                    //println(generatedEvent.head)
                                    generatedEventsList.add(generatedEvent)
                                }
                            }else{
                                if(check){
                                    //println(generatedEvent.head)
                                    generatedEventsList.add(generatedEvent)
                                }
                            }
                        }
                        adapter?.notifyDataSetChanged()
                    }
                }
            }
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.popup_menu,menu)

        val spinnerItem = menu.findItem(R.id.spinner)
        spinner = spinnerItem.actionView as Spinner

        // Spinner için veri seti oluştur
        val options = listOf("Tümü", "Eglence", "Yemek", "Dans", "Spor", "Ogrenim", "Sanat", "Muzik" , "Oyun", "Gezi", "Drama", "Teknoloji")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, options)

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
                // Seçilen öğeyi işleyin
                if(selectedItem != "Tümü"){
                    get_events_fireStore_by_type(selectedItem)
                }else{
                    get_events_fireStore()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Hiçbir öğe seçilmediğinde yapılacak işlem
            }
        }


        val searchItem = toolbar.menu.findItem(R.id.search)
        val searchView = searchItem.actionView as SearchView


        // SearchView dinleyicisini ekleyin
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if(query != ""){
                    query?.let {
                        get_events_fireStore_by_search(it)
                    }
                }

                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    if (it.isEmpty()) {
                        get_events_fireStore()
                        println("Search Çalıştı")
                    }
                }
                return true
            }
        })
    }



    fun menuButtonClicked(view: View){
        popup.show()
    }

    private fun onToolbarMenuClick(item: MenuItem?) : Boolean{
        if(item?.itemId == R.id.profileInfoItem){
            val action = AnaSayfaFragmentDirections.actionAnaSayfaFragmentToKullaniciBilgileriFragment()
            Navigation.findNavController(requireView()).navigate(action)
        }
        else if(item?.itemId == R.id.viewAcceptEvent){
            val action = AnaSayfaFragmentDirections.actionAnaSayfaFragmentToAcceptedEventsFragment()
            Navigation.findNavController(requireView()).navigate(action)
        }
        else if(item?.itemId == R.id.viewFavorite){
            val action = AnaSayfaFragmentDirections.actionAnaSayfaFragmentToFavoritesFragment()
            Navigation.findNavController(requireView()).navigate(action)
        }
        else if(item?.itemId == R.id.viewPreferenceEvent){
            val action = AnaSayfaFragmentDirections.actionAnaSayfaFragmentToEtkinlikTercihleriniDuzenleFragment()
            Navigation.findNavController(requireView()).navigate(action)
        }
        else if(item?.itemId == R.id.logoutItem){
            sign_out()
        }
        return true
    }

    //Tarihi Geçmiş Etkinlik Konrolü
    fun check_out_of_date_event(generatedEvent: GeneratedEvent): Boolean{
        val date = generatedEvent.date + " " + generatedEvent.clock
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val parsedDate = dateFormat.parse(date)
        val currentDate = Calendar.getInstance().time

        if(parsedDate != null){
            val delay = parsedDate.time - currentDate.time
            if(delay<0){
                delete_accepted_event(generatedEvent)
                delete_favorited_event(generatedEvent)
                delete_comments_event(generatedEvent)
                delete_event(generatedEvent)
                return false
            }
        }
        return true
    }

    //Yorumları Sil
    fun delete_comments_event(generatedEvent: GeneratedEvent){
        val eventID = generatedEvent.eventID
        db.collection("Comments_Events")
            .whereEqualTo("event_id", eventID)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    for (document in querySnapshot.documents) {
                        document.reference.delete()
                            .addOnSuccessListener {
                                println("Belge başarıyla silindi: ${document.id}")
                            }
                            .addOnFailureListener { e ->
                                println("Belge silme hatası: ${e.message}")
                            }
                    }
                } else {
                    println("Bu mail bilgisine sahip bir kullanıcı bulunamadı.")
                }
            }
            .addOnFailureListener { e ->
                println("Sorgu sırasında bir hata oluştu: ${e.message}")
            }
    }


    //Favoriye Alınmış Etkinlikleri Sil
    fun delete_favorited_event(generatedEvent: GeneratedEvent){
        val eventID = generatedEvent.eventID
        db.collection("FavoritingEvents")
            .whereEqualTo("event_id", eventID)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    for (document in querySnapshot.documents) {
                        document.reference.delete()
                            .addOnSuccessListener {
                                println("Belge başarıyla silindi: ${document.id}")
                            }
                            .addOnFailureListener { e ->
                                println("Belge silme hatası: ${e.message}")
                            }
                    }
                } else {
                    println("Bu mail bilgisine sahip bir kullanıcı bulunamadı.")
                }
            }
            .addOnFailureListener { e ->
                println("Sorgu sırasında bir hata oluştu: ${e.message}")
            }
    }


    //Katılınmış Etkinlikleri Sil
    fun delete_accepted_event(generatedEvent: GeneratedEvent){
        val eventID = generatedEvent.eventID
        db.collection("JoiningEvents")
            .whereEqualTo("event_id", eventID)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    for (document in querySnapshot.documents) {
                        document.reference.delete()
                            .addOnSuccessListener {
                                println("Belge başarıyla silindi: ${document.id}")
                            }
                            .addOnFailureListener { e ->
                                println("Belge silme hatası: ${e.message}")
                            }
                    }
                } else {
                    println("Bu mail bilgisine sahip bir kullanıcı bulunamadı.")
                }
            }
            .addOnFailureListener { e ->
                println("Sorgu sırasında bir hata oluştu: ${e.message}")
            }
    }

    //Etkinliği Sil
    fun delete_event(generatedEvent: GeneratedEvent){
        val eventID = generatedEvent.eventID
        db.collection("GeneratedEvents").document(eventID)
            .delete()
            .addOnSuccessListener {
                println("Belge başarıyla silindi.")
            }
            .addOnFailureListener { e ->
                println("Belge silinirken hata oluştu: ${e.message}")
            }
    }




    private fun sign_out() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            // Google çıkışı
            GoogleSignIn.getClient(requireActivity(), gso).signOut()
            // Facebook çıkışı
            LoginManager.getInstance().logOut()
            // Firebase çıkışı
            FirebaseAuth.getInstance().signOut()

            // Oturum sonlandırıldıktan sonra veritabanına yeni veri eklemeye çalışın
            val intent = Intent(requireActivity(), MainActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        if(item?.itemId == R.id.createItem){
            val action = AnaSayfaFragmentDirections.actionAnaSayfaFragmentToEtkinlikOlusturmaFragment()
            Navigation.findNavController(requireView()).navigate(action)
        }
        else if(item?.itemId == R.id.mapViewItem){/*
            val action = AnaSayfaFragmentDirections.actionAnaSayfaFragmentToHaritaFragment()
            Navigation.findNavController(requireView()).navigate(action)*/
            try {
                val eventForMapList: ArrayList<EventForMaps> = arrayListOf()

                for(x in generatedEventsList){
                    val eventForMaps = EventForMaps(x.eventID,x.createrMail,x.createrName,x.head,x.explanation,x.shortExplanation,x.type,x.location,x.latitude,x.longitude,x.date,x.clock,x.rate)
                    eventForMapList.add(eventForMaps)
                }

                val intent = Intent(requireContext(), EventsMapsActivity::class.java)
                intent.putExtra("generatedList",ArrayList(eventForMapList))
                startActivity(intent)
            }catch (e: Exception){
                println(e.localizedMessage)
            }
        }
        else if(item?.itemId == R.id.seenMyEventsItem){
            val action = AnaSayfaFragmentDirections.actionAnaSayfaFragmentToThemselvesGeneratedEventsFragment()
            Navigation.findNavController(requireView()).navigate(action)
        }
        else if(item?.itemId == R.id.specialViewItem){
            generatedEventsList.clear()
            if(item.title == "Özelleştirilmiş Görünüm"){
                item.title = "Normal Görünüm"
                spinner.setSelection(0)
                specialViewEnabled = true
                get_events_fireStore()
            }else{
                item.title = "Özelleştirilmiş Görünüm"
                spinner.setSelection(0)
                specialViewEnabled = false
                get_events_fireStore()
            }
            adapter?.notifyDataSetChanged()
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}