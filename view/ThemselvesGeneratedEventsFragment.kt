package com.omeraydin.etkinlikprojesi.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.omeraydin.etkinlikprojesi.R
import com.omeraydin.etkinlikprojesi.adapter.AcceptedEventAdapter
import com.omeraydin.etkinlikprojesi.adapter.GeneratedEventAdapter
import com.omeraydin.etkinlikprojesi.adapter.ThemselvesGeneratedEventAdapter
import com.omeraydin.etkinlikprojesi.databinding.FragmentEtkinlikTercihleriniDuzenleBinding
import com.omeraydin.etkinlikprojesi.databinding.FragmentThemselvesGeneratedEventsBinding
import com.omeraydin.etkinlikprojesi.model.GeneratedEvent

class ThemselvesGeneratedEventsFragment : Fragment() {
    private var _binding: FragmentThemselvesGeneratedEventsBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var adapter: ThemselvesGeneratedEventAdapter?= null

    val generatedEventsList: ArrayList<GeneratedEvent> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        db = Firebase.firestore
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentThemselvesGeneratedEventsBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fBtnBack.setOnClickListener { back(it) }

        val email = auth.currentUser?.email
        if(email != null){
            get_events_fireStore_by_mail(email)

            adapter = ThemselvesGeneratedEventAdapter(generatedEventsList)
            binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerView.adapter = adapter
        }


    }

    fun back(view: View){
        val action = ThemselvesGeneratedEventsFragmentDirections.actionThemselvesGeneratedEventsFragmentToAnaSayfaFragment()
        Navigation.findNavController(requireView()).navigate(action)
    }

    fun get_events_fireStore_by_mail(email: String){
        val query = db.collection("GeneratedEvents")
            .whereEqualTo("event_createrMail",email)
            .orderBy("generated_date", Query.Direction.DESCENDING)

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
                        generatedEventsList.add(generatedEvent)
                        //println(generatedEvent.createrMail+" "+generatedEvent.head+" "+generatedEvent.explanation)
                    }
                    adapter?.notifyDataSetChanged()
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}