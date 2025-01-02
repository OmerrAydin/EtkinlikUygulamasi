package com.omeraydin.etkinlikprojesi.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.omeraydin.etkinlikprojesi.databinding.RecyclerRowBinding
import com.omeraydin.etkinlikprojesi.databinding.RecyclerRowThemselvesGeneratedEventBinding
import com.omeraydin.etkinlikprojesi.model.GeneratedEvent
import com.omeraydin.etkinlikprojesi.view.AnaSayfaFragmentDirections
import com.omeraydin.etkinlikprojesi.view.ThemselvesGeneratedEventsFragmentDirections
import okhttp3.Callback

class ThemselvesGeneratedEventAdapter(val generatedEventsList: ArrayList<GeneratedEvent>): RecyclerView.Adapter<ThemselvesGeneratedEventAdapter.ThemselvesGeneratedEventHolder>(){
    class ThemselvesGeneratedEventHolder(val binding : RecyclerRowThemselvesGeneratedEventBinding) : RecyclerView.ViewHolder(binding.root){

    }
    private lateinit var db: FirebaseFirestore

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemselvesGeneratedEventHolder {
        val recyclerRowBinding = RecyclerRowThemselvesGeneratedEventBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ThemselvesGeneratedEventHolder(recyclerRowBinding)
    }

    override fun getItemCount(): Int {
        return generatedEventsList.size
    }

    override fun onBindViewHolder(holder: ThemselvesGeneratedEventHolder, position: Int) {
        db = Firebase.firestore
        holder.binding.txtHead.text = generatedEventsList[position].head
        holder.binding.txtEventType.text = generatedEventsList[position].type
        holder.binding.txtDate.text = generatedEventsList[position].date
        holder.binding.txtLocation.text = generatedEventsList[position].location
        holder.binding.txtShortExplanation.text = generatedEventsList[position].shortExplanation
        holder.binding.txtExplanationMy.text = generatedEventsList[position].explanation

        holder.binding.ratingBarMy.rating = generatedEventsList[position].rate.toFloat()

        holder.binding.btnDetails.setOnClickListener {
            val eventID = generatedEventsList[position].eventID
            val action = ThemselvesGeneratedEventsFragmentDirections.actionThemselvesGeneratedEventsFragmentToEtkinlikGoruntulemeFragment(eventID)
            Navigation.findNavController(it).navigate(action)
        }

        holder.binding.btnDeleteEvent.setOnClickListener {
            val event = generatedEventsList[position]
            generatedEventsList.clear()
            delete_event_with_id(event){ success ->
                if(success){
                    generatedEventsList.remove(event)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, generatedEventsList.size)
                }
            }
        }

        holder.binding.btnParticipants.setOnClickListener {
            val eventID = generatedEventsList[position].eventID
            val action = ThemselvesGeneratedEventsFragmentDirections.actionThemselvesGeneratedEventsFragmentToParticipantsFragment(eventID)
            Navigation.findNavController(it).navigate(action)
        }

    }

    private fun delete_event_with_id(event: GeneratedEvent, callback: (Boolean) -> Unit){
        delete_comments_event(event){ successComment ->
            if(successComment){
                delete_accepted_event(event){ successAccepted ->
                    if(successAccepted){
                        delete_favorited_event(event){ successFavorited ->
                            if(successFavorited){
                                delete_event(event){ success ->
                                    if(success){
                                        callback(true)
                                    }else{
                                        callback(false)
                                    }
                                }
                            }else{
                                callback(false)
                            }
                        }
                    }else{
                        callback(false)
                    }

                }
            }else{
                callback(false)
            }
        }
    }


    //Yorumları Sil
    fun delete_comments_event(generatedEvent: GeneratedEvent, callback: (Boolean) -> Unit){
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
                                callback(true)
                            }
                            .addOnFailureListener { e ->
                                println("Belge silme hatası: ${e.message}")
                                callback(false)
                            }
                    }
                } else {
                    println("Bu etkinlik idsine sahip bir etkinlik bulunamadı.")
                    callback(true)

                }
            }
            .addOnFailureListener { e ->
                println("Sorgu sırasında bir hata oluştu: ${e.message}")
                callback(false)
            }
    }


    //Favoriye Alınmış Etkinlikleri Sil
    fun delete_favorited_event(generatedEvent: GeneratedEvent, callback: (Boolean) -> Unit){
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
                                callback(true)
                            }
                            .addOnFailureListener { e ->
                                println("Belge silme hatası: ${e.message}")
                                callback(false)
                            }
                    }
                } else {
                    println("Bu mail bilgisine sahip bir kullanıcı bulunamadı.")
                    callback(true)
                }
            }
            .addOnFailureListener { e ->
                println("Sorgu sırasında bir hata oluştu: ${e.message}")
                callback(false)
            }
    }


    //Katılınmış Etkinlikleri Sil
    fun delete_accepted_event(generatedEvent: GeneratedEvent, callback: (Boolean) -> Unit){
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
                                callback(true)
                            }
                            .addOnFailureListener { e ->
                                println("Belge silme hatası: ${e.message}")
                                callback(false)
                            }
                    }
                } else {
                    println("Bu mail bilgisine sahip bir kullanıcı bulunamadı.")
                    callback(true)
                }
            }
            .addOnFailureListener { e ->
                println("Sorgu sırasında bir hata oluştu: ${e.message}")
                callback(false)
            }
    }

    //Etkinliği Sil
    fun delete_event(generatedEvent: GeneratedEvent, callback: (Boolean) -> Unit){
        val eventID = generatedEvent.eventID
        db.collection("GeneratedEvents").document(eventID)
            .delete()
            .addOnSuccessListener {
                println("Belge başarıyla silindi.")
                callback(true)
            }
            .addOnFailureListener { e ->
                println("Belge silinirken hata oluştu: ${e.message}")
                callback(false)
            }
    }
}