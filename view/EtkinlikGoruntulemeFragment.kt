package com.omeraydin.etkinlikprojesi.view

import android.Manifest
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.facebook.AccessToken
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.AggregateField
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.omeraydin.etkinlikprojesi.R
import com.omeraydin.etkinlikprojesi.adapter.CommentsEventAdapter
import com.omeraydin.etkinlikprojesi.controller.NotificationWorker
import com.omeraydin.etkinlikprojesi.databinding.FragmentEtkinlikGoruntulemeBinding
import com.omeraydin.etkinlikprojesi.model.CommentEvent
import com.omeraydin.etkinlikprojesi.model.EventForMaps
import com.omeraydin.etkinlikprojesi.model.GeneratedEvent
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.concurrent.TimeUnit

class EtkinlikGoruntulemeFragment : Fragment() {
    private var _binding: FragmentEtkinlikGoruntulemeBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var generatedEvent: GeneratedEvent
    private  lateinit var eventForMap: EventForMaps

    private var adapter: CommentsEventAdapter ?= null
    val commentList: ArrayList<CommentEvent> = arrayListOf()

    private lateinit var permissionLauncher: ActivityResultLauncher<String>



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        db = Firebase.firestore
        registerLaunchers()
        set_notification_channel_timer()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEtkinlikGoruntulemeBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fBtnBack.setOnClickListener { back(it) }

        val nestedScrollView = binding.nestedScrollView
        binding.etxtComment.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                nestedScrollView.post {
                    nestedScrollView.scrollTo(0, binding.etxtComment.bottom)
                }
            }
        }

        arguments?.let {
            val eventID = EtkinlikGoruntulemeFragmentArgs.fromBundle(it).eventId
            println(eventID)
            val email = auth.currentUser?.email ?: "No email available"
            val name = auth.currentUser?.displayName ?: "No display name available"
            println(email)
            println(name)
            if(email != ""){
                count_comment_senderEmail_fireStore(email,eventID){ count ->
                    updateClickable(count)
                }
                getDocumentData(eventID)
                get_comment_eventID_fireStore(eventID)
                get_avarage_rate_to_eventID(eventID){ value ->
                    binding.ratingBar.rating = value.toFloat()
                }

                adapter = CommentsEventAdapter(commentList)
                binding.rViewComments.layoutManager = LinearLayoutManager(requireContext())
                binding.rViewComments.adapter = adapter
                /*
                            binding.ratingBar.setOnRatingBarChangeListener { ratingBar, rating, fromUser ->
                                val rate = ratingBar.rating.toDouble()
                            }*/

                favorite_check(eventID,email)
                binding.btnAddFavorites.setOnClickListener {
                    add_favorites(it,eventID,email,generatedEvent.head,generatedEvent.type,
                        generatedEvent.date,generatedEvent.clock,generatedEvent.location)
                }

                join_check(eventID,email)
                binding.btnJoin.setOnClickListener {
                    val head = generatedEvent.head
                    val type = generatedEvent.type
                    val date = generatedEvent.date
                    val clock = generatedEvent.clock
                    val location = generatedEvent.location
                    join_click(eventID,email,name,head,type, date,clock,location)
                }

                createNotificationChannel(requireContext())

                binding.btnSendComment.setOnClickListener { send_click(it,eventID) }

                binding.btnGetDirections.setOnClickListener {
                    eventForMap = EventForMaps(generatedEvent.eventID,generatedEvent.createrMail,generatedEvent.createrName,
                        generatedEvent.head,generatedEvent.explanation,generatedEvent.shortExplanation,generatedEvent.type,
                        generatedEvent.location,generatedEvent.latitude,generatedEvent.longitude,generatedEvent.date,
                        generatedEvent.clock,generatedEvent.rate)
                    val intent = Intent(requireContext(), MapsActivity::class.java)
                    intent.putExtra("event",eventForMap)
                    startActivity(intent)
                }
            }
        }
    }

    fun set_notification_channel_timer() {
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


    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "app_channel_id"
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

    fun updateClickableFavorite(count: Int){
        if(count>0){
            binding.btnAddFavorites.isEnabled = false
        }else{
            binding.btnAddFavorites.isEnabled = true
        }
    }

    fun favorite_check(eventID: String, joinerEmail: String){
        val query = db.collection("FavoritingEvents")
            .whereEqualTo("joiner_email",joinerEmail)
            .whereEqualTo("event_id",eventID)
            .count()
        val docRef = query.get(AggregateSource.SERVER).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Count fetched successfully
                val snapshot = task.result
                val count =  snapshot.count.toInt()
                updateClickableFavorite(count)
            } else {
                println("Count failed: "+ task.getException())
            }
        }
    }

    fun add_favorites(view: View, eventID: String, favoriteEmail: String, eventHead: String, eventType: String, eventDate: String, eventClock: String, eventLocate: String){
        if(eventID != null && favoriteEmail != "" && eventHead != "" && eventType != "" && eventDate != "" && eventClock != "" && eventLocate != ""){
            val favoritingMap = hashMapOf<String, Any>()
            favoritingMap.put("event_id",eventID)
            favoritingMap.put("joiner_email",favoriteEmail)
            favoritingMap.put("event_head",eventHead)
            favoritingMap.put("event_type",eventType)
            favoritingMap.put("event_date",eventDate)
            favoritingMap.put("event_clock",eventClock)
            favoritingMap.put("event_locate",eventLocate)
            favoritingMap.put("generated_date",Timestamp.now())

            db.collection("FavoritingEvents").add(favoritingMap).addOnSuccessListener { documentReference ->
                context?.let {
                    Toast.makeText(it,"Etkinlik Favorilerinize Eklendi!",Toast.LENGTH_SHORT).show()
                }
                favorite_check(eventID,favoriteEmail)
            }.addOnFailureListener { exception ->
                println(exception)
                context?.let {
                    Toast.makeText(it,exception.localizedMessage,Toast.LENGTH_LONG).show()
                }
            }

        }else{
            val alertDialog = AlertDialog.Builder(
                requireContext(),
                R.style.CustomAlertDialog // Özel tema
            )
                .setTitle("Uyarı")
                .setMessage("Kayıt İşlemi Gerçekleştirilemedi!")
                .setPositiveButton("Tamam") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
            alertDialog.show()
        }
    }

    fun updateClickableJoin(count: Int){
        if(count>0){
            binding.btnJoin.isEnabled = false
        }else{
            binding.btnJoin.isEnabled = true
        }
    }

    fun join_check(eventID: String, joinerEmail: String){
        val query = db.collection("JoiningEvents")
            .whereEqualTo("joiner_email",joinerEmail)
            .whereEqualTo("event_id",eventID)
            .count()
        val docRef = query.get(AggregateSource.SERVER).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Count fetched successfully
                val snapshot = task.result
                val count =  snapshot.count.toInt()
                updateClickableJoin(count)
            } else {
                println("Count failed: "+ task.getException())
            }
        }
    }

    fun join_click(eventID: String, joinerEmail: String, joinerName: String, eventHead: String, eventType: String, eventDate: String, eventClock: String, eventLocate: String){
        println("0")
        if(joinerEmail != "" && joinerName != "" && eventHead != "" && eventType != "" && eventDate != "" && eventClock != "" && eventLocate != ""){
            val joiningMap = hashMapOf<String, Any>()
            joiningMap.put("event_id",eventID)
            joiningMap.put("joiner_email",joinerEmail)
            joiningMap.put("joiner_name",joinerName)
            joiningMap.put("event_head",eventHead)
            joiningMap.put("event_type",eventType)
            joiningMap.put("event_date",eventDate)
            joiningMap.put("event_clock",eventClock)
            joiningMap.put("event_locate",eventLocate)
            joiningMap.put("generated_date",Timestamp.now())
            println("1")

            db.collection("JoiningEvents").add(joiningMap).addOnSuccessListener { documentReference ->
                context?.let {
                    Toast.makeText(requireContext(),"Kaydınız Başarılı!",Toast.LENGTH_SHORT).show()
                }
                println("2")
                join_check(eventID,joinerEmail)
            }.addOnFailureListener { exception ->
                Log.e("AppDebug", "Uygulama çökme detayı", exception)
                println("Hata!"+exception)
                context?.let {
                    Toast.makeText(requireContext(),exception.localizedMessage,Toast.LENGTH_LONG).show()
                }
            }

        }else{
            println("3")
            val alertDialog = AlertDialog.Builder(
                requireContext(),
                R.style.CustomAlertDialog // Özel tema
            )
                .setTitle("Uyarı")
                .setMessage("Kayıt İşlemi Gerçekleştirilemedi!")
                .setPositiveButton("Tamam") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
            alertDialog.show()
        }
    }

    fun send_click(view: View, eventID: String) = runBlocking{
        val senderName = auth.currentUser!!.displayName.toString()
        val senderEmail = auth.currentUser!!.email.toString()
        val comment = binding.etxtComment.text.toString()
        val rate = binding.ratingBar.rating.toDouble()

        create_comment_firestore(eventID,senderName,senderEmail,comment,rate){isCreated ->
            if(isCreated){
                avarage_rate_event(eventID){ rate ->
                    if(rate != null){
                        update_event_rate(eventID,rate){ success ->
                            if(success){
                                binding.etxtComment.setText("")
                                get_avarage_rate_to_eventID(eventID){ value ->
                                    if(value != null){
                                        binding.ratingBar.rating = value.toFloat()
                                        count_comment_senderEmail_fireStore(senderEmail,eventID){ count ->
                                            if(count != null){
                                                updateClickable(count)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }

    }

    fun get_avarage_rate_to_eventID(eventID: String, callback: (Double) -> Unit){
        val collectionName = "GeneratedEvents"  // Koleksiyon adı
        val docRef = db.collection(collectionName).document(eventID)

        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val rate = document.getDouble("event_rate") as Double
                    println(rate)
                    callback(rate)
                } else {
                    println("Belge bulunamadı!")
                    callback(0.0)
                }
            }
            .addOnFailureListener { exception ->
                // Hata oluştu
                println("Hata: $exception")
                callback(0.0)
            }
    }

    fun get_comment_eventID_fireStore(current_event_id: String){
        val query = db.collection("Comments_Events")
            .whereEqualTo("event_id",current_event_id)
            .orderBy("generated_date",Query.Direction.DESCENDING)

        val docRef = query.addSnapshotListener { value, error ->
            if (error != null) {
                Toast.makeText(requireContext(),"Comments listen failed. ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                println("Listen failed. ${error.localizedMessage}")
                //return@addSnapshotListener
            }else{
                if (value != null && !value.isEmpty){
                    commentList.clear()
                    val documents = value.documents
                    for (document in documents){
                        val eventID = document.get("event_id") as String
                        val senderName = document.get("comment_senderName") as String
                        val senderEmail = document.get("comment_senderEmail") as String
                        val comment = document.get("comment_message") as String
                        val rate = document.get("comment_rate") as Double
                        val generatedDate = document.getTimestamp("generated_date") as Timestamp
                        val commentEvent = CommentEvent(eventID,senderName,senderEmail,comment,rate,generatedDate)
                        commentList.add(commentEvent)
                    }
                    adapter?.notifyDataSetChanged()
                }
            }
        }
    }

    fun avarage_rate_event(eventID: String, callback: (Double) -> Unit){
        val query = db.collection("Comments_Events").whereEqualTo("event_id", eventID)
        val aggregateQuery = query.aggregate(AggregateField.average("comment_rate"))
        aggregateQuery.get(AggregateSource.SERVER).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Aggregate fetched successfully
                val snapshot = task.result
                var rate = snapshot.get(AggregateField.average("comment_rate"))
                if (rate != null){
                    rate = rate.toDouble()
                    println("2 - avarage rate")
                    callback(rate)
                }else{
                    callback(0.0)
                }
                println("Average: ${snapshot.get(AggregateField.average("comment_rate"))}")
            } else {
                println("Aggregate failed: "+ task.getException())
                callback(0.0)
            }
        }
    }

    fun update_event_rate(eventID: String, rate: Double?, callback: (Boolean) -> Unit){
        val userRef = db.collection("GeneratedEvents").document(eventID) // Belgeyi referans al
        userRef.update("event_rate", rate).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                println("3 - update")
                println("rate başarıyla güncellendi")
                callback(true)
            } else {
                println("Hata: ${task.exception}")
                callback(false)
            }
        }
    }


    fun count_comment_senderEmail_fireStore(current_senderMail: String, current_eventID: String, callback: (Int) -> Unit){
        val query = db.collection("Comments_Events")
            .whereEqualTo("comment_senderEmail",current_senderMail)
            .whereEqualTo("event_id",current_eventID)
            .count()
        val docRef = query.get(AggregateSource.SERVER).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Count fetched successfully
                val snapshot = task.result
                val count =  snapshot.count.toInt()
                callback(count)
            } else {
                println("Count failed: "+ task.getException())
                callback(0)
            }
        }
    }

    fun updateClickable(count: Int){
        if(count>0) {
            binding.btnSendComment.isEnabled = false
            binding.ratingBar.setIsIndicator(true)
            binding.etxtComment.isEnabled = false
        }else{
            binding.btnSendComment.isEnabled = true
            binding.ratingBar.setIsIndicator(false)
            binding.etxtComment.isEnabled = true
        }
    }

    fun create_comment_firestore(eventID: String, senderName: String, senderEmail: String, comment: String, rate: Double, callback: (Boolean) -> Unit){
        if(eventID != null && senderName != "" && senderEmail != "" && comment != "" && rate != null && rate != 0.0){
            val commentMap = hashMapOf<String, Any>()
            commentMap.put("event_id",eventID)
            commentMap.put("comment_senderName",senderName)
            commentMap.put("comment_senderEmail",senderEmail)
            commentMap.put("comment_message",comment)
            commentMap.put("comment_rate",rate)
            commentMap.put("generated_date",Timestamp.now())

            db.collection("Comments_Events").add(commentMap).addOnSuccessListener { documentReference ->
                get_comment_eventID_fireStore(eventID)
                Toast.makeText(requireContext(),"Yorumunuz Gönderildi!",Toast.LENGTH_SHORT).show()
                adapter?.notifyDataSetChanged()
                callback(true)
                println("1 - create")
            }.addOnFailureListener { exception ->
                Toast.makeText(requireContext(),exception.localizedMessage,Toast.LENGTH_LONG).show()
                callback(false)
            }

        }else{
            val alertDialog = AlertDialog.Builder(
                requireContext(),
                R.style.CustomAlertDialog // Özel tema
            )
                .setTitle("Uyarı")
                .setMessage("Boş Mesaj Gönderemezsiniz!\nYorum Göndermek İçin Etkinliğe Oy Vermeyi Unutmayın!")
                .setPositiveButton("Tamam") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
            alertDialog.show()
            callback(false)
        }
    }

    fun getDocumentData(documentId: String) {
        val collectionName = "GeneratedEvents"  // Koleksiyon adı
        val docRef = db.collection(collectionName).document(documentId)

        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val eventID = documentId
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
                    generatedEvent = GeneratedEvent(eventID,createrMail,createrName,head,explanation,shortExplanation,
                        type,location,latitude,longitude,date,clock,base64String,rate,generated_date)
                    update_UI(generatedEvent)
                } else {
                    println("Belge bulunamadı!")
                }
            }
            .addOnFailureListener { exception ->
                // Hata oluştu
                println("Hata: $exception")
            }
    }

    fun update_UI(generatedEvent: GeneratedEvent){
        val base64String = generatedEvent.base64String
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        val bitmapImage: Bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

        binding.txtHead.text = generatedEvent.head
        binding.txtCreator.text = "Oluşturan: "+generatedEvent.createrName
        binding.txtEventType.text = "Tür: "+generatedEvent.type
        binding.txtDate.text = "Tarih: "+generatedEvent.date
        binding.txtClock.text = "Saat: "+generatedEvent.clock
        binding.txtLocation.text = "Konum: "+generatedEvent.location
        binding.txtShortExplanation.text = "Kısa Açıklama: "+generatedEvent.shortExplanation
        binding.imageView.setImageBitmap(bitmapImage)
        binding.txtExplanation.text = "Açıklama: "+generatedEvent.explanation
    }

    fun back(view: View){
        val action = EtkinlikGoruntulemeFragmentDirections.actionEtkinlikGoruntulemeFragmentToAnaSayfaFragment()
        Navigation.findNavController(view).navigate(action)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}