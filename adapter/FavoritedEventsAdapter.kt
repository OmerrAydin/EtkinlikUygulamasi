package com.omeraydin.etkinlikprojesi.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.omeraydin.etkinlikprojesi.adapter.AcceptedEventAdapter.AcceptedEventHolder
import com.omeraydin.etkinlikprojesi.databinding.RecyclerRowConfirmedBinding
import com.omeraydin.etkinlikprojesi.model.AcceptedEvent
import com.omeraydin.etkinlikprojesi.model.FavoritedEvent
import com.omeraydin.etkinlikprojesi.view.AcceptedEventsFragmentDirections
import com.omeraydin.etkinlikprojesi.view.FavoritesFragmentDirections

class FavoritedEventsAdapter (val favoritedEventsList: ArrayList<FavoritedEvent>): RecyclerView.Adapter<FavoritedEventsAdapter.FavoritedEventHolder>() {
    class FavoritedEventHolder(val binding: RecyclerRowConfirmedBinding): RecyclerView.ViewHolder(binding.root){

    }

    private lateinit var db: FirebaseFirestore


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoritedEventHolder {
        val recyclerRowConfirmedBinding = RecyclerRowConfirmedBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return FavoritedEventHolder(recyclerRowConfirmedBinding)
    }

    override fun getItemCount(): Int {
        return favoritedEventsList.size
    }

    override fun onBindViewHolder(holder: FavoritedEventHolder, position: Int) {
        db = Firebase.firestore
        val context = holder.itemView.context
        val event = favoritedEventsList[position]

        holder.binding.txtHead.text = event.eventHead
        holder.binding.txtEventType.text = event.eventType
        holder.binding.txtDate.text = event.eventDate
        holder.binding.txtClock.text = event.eventClock
        holder.binding.txtLocation.text = event.eventLocate

        holder.binding.btnReminder.isEnabled = false

        holder.binding.btnDetails.setOnClickListener {
            val eventID = favoritedEventsList[position].eventID
            val action = FavoritesFragmentDirections.actionFavoritesFragmentToEtkinlikGoruntulemeFragment(eventID)
            Navigation.findNavController(it).navigate(action)
        }

        holder.binding.btnDelete.setOnClickListener {
            val eventHolder = favoritedEventsList[position]
            favoritedEventsList.clear()
            delete_favorited_event(eventHolder,context){ success ->
                if(success){
                    favoritedEventsList.remove(eventHolder)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, favoritedEventsList.size)
                }
            }

        }
    }

    fun delete_favorited_event(favoritedEvent: FavoritedEvent, contex: Context, callback: (Boolean) -> Unit){
        val mail = favoritedEvent.favoriteEmail
        val eventID = favoritedEvent.eventID
        db.collection("FavoritingEvents")
            .whereEqualTo("joiner_email", mail)
            .whereEqualTo("event_id", eventID)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    for (document in querySnapshot.documents) {
                        document.reference.delete()
                            .addOnSuccessListener {
                                println("Belge başarıyla silindi: ${document.id}")
                                Toast.makeText(contex,"Favori Etkinlik Silindisil!", Toast.LENGTH_SHORT).show()
                                callback(true)
                            }
                            .addOnFailureListener { e ->
                                println("Belge silme hatası: ${e.message}")
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
            }
    }
}