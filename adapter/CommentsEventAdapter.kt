package com.omeraydin.etkinlikprojesi.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.omeraydin.etkinlikprojesi.adapter.GeneratedEventAdapter.GeneratedEventHolder
import com.omeraydin.etkinlikprojesi.databinding.RecyclerRowBinding
import com.omeraydin.etkinlikprojesi.databinding.RecyclerRowCommentBinding
import com.omeraydin.etkinlikprojesi.model.CommentEvent
import com.omeraydin.etkinlikprojesi.model.GeneratedEvent
import com.omeraydin.etkinlikprojesi.view.AnaSayfaFragmentDirections

class CommentsEventAdapter(val commentsEventsList: ArrayList<CommentEvent>): RecyclerView.Adapter<CommentsEventAdapter.CommentsEventHolder>() {
    class CommentsEventHolder(val binding : RecyclerRowCommentBinding) : RecyclerView.ViewHolder(binding.root){

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentsEventHolder {
        val recyclerRowCommentBinding = RecyclerRowCommentBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return CommentsEventHolder(recyclerRowCommentBinding)
    }

    override fun getItemCount(): Int {
        return commentsEventsList.size
    }

    override fun onBindViewHolder(holder: CommentsEventHolder, position: Int) {
        holder.binding.txtSenderName.text = "İsim:: "+commentsEventsList[position].senderName
        holder.binding.txtSenderEmailComment.text = "Email: "+commentsEventsList[position].senderEmail
        holder.binding.txtComment.text = commentsEventsList[position].comment
        holder.binding.ratingBarComment.rating = commentsEventsList[position].rate.toFloat()
    }
}