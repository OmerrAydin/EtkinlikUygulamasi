package com.omeraydin.etkinlikprojesi.adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.omeraydin.etkinlikprojesi.databinding.RecyclerRowBinding
import com.omeraydin.etkinlikprojesi.model.GeneratedEvent
import com.omeraydin.etkinlikprojesi.view.AnaSayfaFragmentDirections

class GeneratedEventAdapter (val generatedEventsList: ArrayList<GeneratedEvent>): RecyclerView.Adapter<GeneratedEventAdapter.GeneratedEventHolder>() {
    class GeneratedEventHolder(val binding : RecyclerRowBinding) : RecyclerView.ViewHolder(binding.root){

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GeneratedEventHolder {
        val recyclerRowBinding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return GeneratedEventHolder(recyclerRowBinding)
    }

    override fun getItemCount(): Int {
        return generatedEventsList.size
    }

    override fun onBindViewHolder(holder: GeneratedEventHolder, position: Int) {
        holder.binding.txtHead.text = generatedEventsList[position].head
        holder.binding.txtEventType.text = "Tür: " + generatedEventsList[position].type
        holder.binding.txtDate.text = "Tarih: " + generatedEventsList[position].date
        holder.binding.txtCreator.text = "Oluşturan: " + generatedEventsList[position].createrName
        holder.binding.txtLocation.text = "Konum: " + generatedEventsList[position].location
        holder.binding.txtShortExplanation.text = "Kısa Açıklama: " + generatedEventsList[position].shortExplanation

        val base64String = generatedEventsList[position].base64String
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        val bitmapImage: Bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        holder.binding.imageView.setImageBitmap(bitmapImage)

        holder.binding.btnDetails.setOnClickListener {
            val eventID = generatedEventsList[position].eventID
            val action = AnaSayfaFragmentDirections.actionAnaSayfaFragmentToEtkinlikGoruntulemeFragment(eventID)
            Navigation.findNavController(it).navigate(action)
        }
    }
}