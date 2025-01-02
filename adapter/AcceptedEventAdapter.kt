package com.omeraydin.etkinlikprojesi.adapter

import android.Manifest
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodSession.EventCallback
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.omeraydin.etkinlikprojesi.controller.NotificationWorker
import com.omeraydin.etkinlikprojesi.databinding.RecyclerRowConfirmedBinding
import com.omeraydin.etkinlikprojesi.model.AcceptedEvent
import com.omeraydin.etkinlikprojesi.view.AcceptedEventsFragmentDirections
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

class AcceptedEventAdapter(val acceptedEventsList: ArrayList<AcceptedEvent>): RecyclerView.Adapter<AcceptedEventAdapter.AcceptedEventHolder>() {
    class AcceptedEventHolder(val binding: RecyclerRowConfirmedBinding): RecyclerView.ViewHolder(binding.root){

    }

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    var datetime = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AcceptedEventHolder {
        val recyclerRowConfirmedBinding = RecyclerRowConfirmedBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return AcceptedEventHolder(recyclerRowConfirmedBinding)
    }

    override fun getItemCount(): Int {
        return acceptedEventsList.size
    }

    override fun onBindViewHolder(holder: AcceptedEventHolder, position: Int) {
        auth = Firebase.auth
        db = Firebase.firestore
        val context = holder.itemView.context
        val view = holder.itemView.rootView
        set_notification_channel(context)

        holder.binding.txtHead.text = acceptedEventsList[position].eventHead
        holder.binding.txtEventType.text = acceptedEventsList[position].eventType
        holder.binding.txtDate.text = acceptedEventsList[position].eventDate
        holder.binding.txtClock.text = acceptedEventsList[position].eventClock
        holder.binding.txtLocation.text = acceptedEventsList[position].eventLocate

        holder.binding.btnDetails.setOnClickListener {
            val eventID = acceptedEventsList[position].eventID
            val action = AcceptedEventsFragmentDirections.actionAcceptedEventsFragmentToEtkinlikGoruntulemeFragment(eventID)
            Navigation.findNavController(it).navigate(action)
        }

        val eventKey = "notificationUserID_${acceptedEventsList[position].joinerEmail}${acceptedEventsList[position].eventID}"
        val dateKey = "dateUserID_${acceptedEventsList[position].joinerEmail}${acceptedEventsList[position].eventID}"
        val sharedPreferences = context.getSharedPreferences("alarmNotification", Context.MODE_PRIVATE)

        val dateUser = sharedPreferences.getString(dateKey, "")

        if(dateUser != null && dateUser != ""){
            val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val datePar = format.parse(dateUser)
            val current = Calendar.getInstance().time

            if(datePar != null){
                val delay = datePar.time - current.time
                if (delay <= 0) {
                    holder.binding.btnReminder.text = "Hatırlatıcı Ekle"
                    sharedPreferences.edit().putBoolean(eventKey, false).apply()
                    sharedPreferences.edit().putString(dateKey, "").apply()
                }
            }
        }


        val alreadyNotified = sharedPreferences.getBoolean(eventKey, false)

        if(!alreadyNotified){
            holder.binding.btnReminder.text = "Hatırlatıcı Ekle"
        }else{
            holder.binding.btnReminder.text = "Hatırlatıcıyı Kapat"
        }

        val title = "Yaklaşan Bir Etkinliğin Var!"
        val message = "${acceptedEventsList[position].eventHead} isimli etkinliğin yaklaştı.\n${acceptedEventsList[position].eventDate}-${acceptedEventsList[position].eventClock}"

        holder.binding.btnReminder.setOnClickListener {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ){
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    //izin yok
                    Snackbar.make(view,"Etkinliklerinizi hatırlatabilmemiz için bildirimlere izin vermeniz gerekiyor!",
                        Snackbar.LENGTH_INDEFINITE).setAction("İzin Ver"
                        ,View.OnClickListener {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            intent.putExtra(Settings.EXTRA_APP_PACKAGE, "com.omeraydin.etkinlikprojesi")
                            context.startActivity(intent)
                        }).show()
                }else{
                    //izin var
                    if(holder.binding.btnReminder.text == "Hatırlatıcı Ekle"){
                        get_date(context, acceptedEventsList[position].joinerEmail, acceptedEventsList[position].eventID, sharedPreferences, holder, eventKey, dateKey, title, message)
                    }else{
                        sharedPreferences.edit().putBoolean(eventKey, false).apply()
                        WorkManager.getInstance(context).cancelAllWorkByTag("alarm_${acceptedEventsList[position].joinerEmail}${acceptedEventsList[position].eventID}")
                        Toast.makeText(context,"Alarm İptal Edildi!",Toast.LENGTH_SHORT).show()
                        holder.binding.btnReminder.text = "Hatırlatıcı Ekle"
                    }
                }
            }else{
                if(holder.binding.btnReminder.text == "Hatırlatıcı Ekle"){
                    get_date(context, acceptedEventsList[position].joinerEmail, acceptedEventsList[position].eventID, sharedPreferences, holder, eventKey, dateKey, title, message)
                }else{
                    sharedPreferences.edit().putBoolean(eventKey, false).apply()
                    WorkManager.getInstance(context).cancelAllWorkByTag("alarm_${acceptedEventsList[position].joinerEmail}${acceptedEventsList[position].eventID}")
                    Toast.makeText(context,"Alarm İptal Edildi!",Toast.LENGTH_SHORT).show()
                    holder.binding.btnReminder.text = "Hatırlatıcı Ekle"
                }
            }
        }

        holder.binding.btnDelete.setOnClickListener {
            sharedPreferences.edit().putBoolean(eventKey, false).apply()
            WorkManager.getInstance(context).cancelAllWorkByTag("alarm_${acceptedEventsList[position].joinerEmail}${acceptedEventsList[position].eventID}")
            holder.binding.btnReminder.text = "Hatırlatıcı Ekle"

            val eventHolder = acceptedEventsList[position]
            acceptedEventsList.clear()
            delete_accepted_event(eventHolder,context){
                acceptedEventsList.remove(eventHolder)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, acceptedEventsList.size)
            }
        }
    }

    fun delete_accepted_event(acceptedEvent: AcceptedEvent, contex: Context, callback: (Boolean) -> Unit){
        val mail = acceptedEvent.joinerEmail
        val eventID = acceptedEvent.eventID
        db.collection("JoiningEvents")
            .whereEqualTo("joiner_email", mail)
            .whereEqualTo("event_id", eventID)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    for (document in querySnapshot.documents) {
                        document.reference.delete()
                            .addOnSuccessListener {
                                println("Belge başarıyla silindi: ${document.id}")
                                Toast.makeText(contex,"Etkinlik İptal Edildi!",Toast.LENGTH_SHORT).show()
                                callback(true)
                            }
                            .addOnFailureListener { e ->
                                println("Belge silme hatası: ${e.message}")
                                Toast.makeText(contex,"Etkinliği Silerken Bir Hata Oluştu!",Toast.LENGTH_SHORT).show()
                                callback(false)
                            }
                    }
                } else {
                    println("Bu mail bilgisine sahip bir kullanıcı bulunamadı.")
                    callback(false)
                }
            }
            .addOnFailureListener { e ->
                println("Sorgu sırasında bir hata oluştu: ${e.message}")
                callback(false)
            }
    }

    fun get_date(contex: Context , mail: String, eventID: String, sharedPreferences: SharedPreferences, holder: AcceptedEventHolder, eventKey: String, dateKey: String, title: String, message: String){
        val calendar = Calendar.getInstance()
        datetime = ""

        val datePickerDialog = DatePickerDialog(
            contex,
            { _, year, month, dayOfMonth ->
                // Seçilen tarihi işleyin
                val selectedDate = "$dayOfMonth/${month + 1}/$year"
                datetime += selectedDate
                get_time(contex,mail,eventID,sharedPreferences,holder,eventKey, dateKey, title, message)
            },
            calendar.get(Calendar.YEAR),  // Başlangıç yılı
            calendar.get(Calendar.MONTH), // Başlangıç ayı
            calendar.get(Calendar.DAY_OF_MONTH) // Başlangıç günü
        )
        datePickerDialog.show()
    }

    fun get_time(contex: Context, mail: String, eventID: String, sharedPreferences: SharedPreferences, holder: AcceptedEventHolder, eventKey: String, dateKey: String, title: String, message: String){
        val timePickerDialog = TimePickerDialog(
            contex, // Context, burada fragment içinde olduğunuz için requireContext()
            { _, hourOfDay, minute ->
                // Saat ve dakika seçildiğinde yapılacak işlem
                val selectedTime = String.format("%02d:%02d", hourOfDay, minute)
                datetime += " "+selectedTime
                //Çıktı
                println(datetime)
                set_alarm(contex,datetime, mail, eventID, sharedPreferences, holder, eventKey, dateKey, title, message)

            },
            12, // Başlangıç saati (12:00)
            0,  // Başlangıç dakikası (00)
            true // 24 saat formatı (true ise 24 saat formatı, false ise 12 saat formatı)
        )
        timePickerDialog.show()
    }

    fun set_notification_channel(context: Context) {
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
                context,
                NotificationManager::class.java
            )
            notificationManager?.createNotificationChannel(notificationChannel)
        }
    }

    fun set_alarm(context: Context, dateString: String, mail: String, eventID: String, sharedPreferences: SharedPreferences, holder: AcceptedEventHolder, eventKey: String, dateKey: String, title: String, message: String) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val parsedDate = dateFormat.parse(dateString)
        val currentDate = Calendar.getInstance().time


        if(parsedDate != null){
            val delay = parsedDate.time - currentDate.time
            if (delay > 0) {
                val data = androidx.work.Data.Builder()
                    .putString("title", title)
                    .putString("message", message)
                    .build()

                val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .addTag("alarm_${mail}${eventID}")
                    .setInputData(data)
                    .build()

                WorkManager.getInstance(context).enqueue(workRequest)

                sharedPreferences.edit().putBoolean(eventKey, true).apply()
                holder.binding.btnReminder.text = "Hatırlatıcıyı Kapat"
                sharedPreferences.edit().putString(dateKey, datetime).apply()
            } else {
                Toast.makeText(context,"Geçmiş Tarihe Alarm Kuramazsınız!", Toast.LENGTH_LONG).show()
            }
            Toast.makeText(context,"Alarm Kuruldu", Toast.LENGTH_SHORT).show()
        }
    }
}