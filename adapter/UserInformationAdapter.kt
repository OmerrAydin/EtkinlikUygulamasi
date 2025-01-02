package com.omeraydin.etkinlikprojesi.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.omeraydin.etkinlikprojesi.adapter.ThemselvesGeneratedEventAdapter.ThemselvesGeneratedEventHolder
import com.omeraydin.etkinlikprojesi.databinding.FragmentParticipantsBinding
import com.omeraydin.etkinlikprojesi.databinding.RecyclerRowParticipantsBinding
import com.omeraydin.etkinlikprojesi.databinding.RecyclerRowThemselvesGeneratedEventBinding
import com.omeraydin.etkinlikprojesi.model.UserInformation

class UserInformationAdapter(val userInformationList: ArrayList<UserInformation>): RecyclerView.Adapter<UserInformationAdapter.UserInformationHolder>() {
    class UserInformationHolder(val binding: RecyclerRowParticipantsBinding): RecyclerView.ViewHolder(binding.root){

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserInformationHolder {
        val recyclerRowBinding = RecyclerRowParticipantsBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return UserInformationHolder(recyclerRowBinding)
    }

    override fun getItemCount(): Int {
        return userInformationList.size
    }

    override fun onBindViewHolder(holder: UserInformationHolder, position: Int) {
        holder.binding.txtParticipantName.text = userInformationList[position].name
        holder.binding.txtParticipantMail.text = userInformationList[position].mail
    }
}