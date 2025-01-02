package com.omeraydin.etkinlikprojesi.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.omeraydin.etkinlikprojesi.R
import com.omeraydin.etkinlikprojesi.adapter.ThemselvesGeneratedEventAdapter
import com.omeraydin.etkinlikprojesi.adapter.UserInformationAdapter
import com.omeraydin.etkinlikprojesi.databinding.FragmentParticipantsBinding
import com.omeraydin.etkinlikprojesi.databinding.FragmentThemselvesGeneratedEventsBinding
import com.omeraydin.etkinlikprojesi.model.GeneratedEvent
import com.omeraydin.etkinlikprojesi.model.UserInformation

class ParticipantsFragment : Fragment() {
    private var _binding: FragmentParticipantsBinding? = null
    private val binding get() = _binding!!

    val userInformationList: ArrayList<UserInformation> = arrayListOf()
    private var adapter: UserInformationAdapter?=null

    private lateinit var db: FirebaseFirestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Firebase.firestore
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentParticipantsBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            val eventID = ParticipantsFragmentArgs.fromBundle(it).eventId
            println(eventID)

            get_participant_event_in_firebase_with_eventID(eventID)

            adapter = UserInformationAdapter(userInformationList)
            binding.recyclerViewParticipants.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerViewParticipants.adapter = adapter
        }
    }

    fun get_participant_event_in_firebase_with_eventID(eventID: String){
        val query = db.collection("JoiningEvents")
            .whereEqualTo("event_id",eventID)
            .orderBy("generated_date", Query.Direction.DESCENDING)

        val docRef = query.addSnapshotListener { value, error ->
            if (error != null) {
                Toast.makeText(requireContext(),"Listen failed. ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                println("Listen failed. ${error.localizedMessage}")
                //return@addSnapshotListener
            }else{
                if (value != null && !value.isEmpty){
                    userInformationList.clear()
                    val documents = value.documents
                    for (document in documents){
                        val mail = document.get("joiner_email") as String
                        val name = document.get("joiner_name") as String
                        val userInfo = UserInformation(mail,name)
                        userInformationList.add(userInfo)
                        //println(generatedEvent.createrMail+" "+generatedEvent.head+" "+generatedEvent.explanation)
                    }
                    adapter?.notifyDataSetChanged()
                    binding.txtParticipantsSize.text = "Katılımcı Sayısı: ${userInformationList.size}"
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}