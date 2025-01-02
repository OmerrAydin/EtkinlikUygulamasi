package com.omeraydin.etkinlikprojesi.view

import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.os.Binder
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
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
import com.omeraydin.etkinlikprojesi.controller.NotificationWorker
import com.omeraydin.etkinlikprojesi.databinding.FragmentAcceptedEventsBinding
import com.omeraydin.etkinlikprojesi.databinding.FragmentAnaSayfaBinding
import com.omeraydin.etkinlikprojesi.model.AcceptedEvent
import com.omeraydin.etkinlikprojesi.model.GeneratedEvent
import java.text.SimpleDateFormat
import java.time.Year
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class AcceptedEventsFragment : Fragment() {
    private var _binding: FragmentAcceptedEventsBinding?= null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var adapter: AcceptedEventAdapter?= null

    val acceptedEventsList : ArrayList<AcceptedEvent> = arrayListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        db = Firebase.firestore
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAcceptedEventsBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        get_accepted_events_fireStore()
        adapter = AcceptedEventAdapter(acceptedEventsList)
        binding.rViewEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rViewEvents.adapter = adapter

        binding.fBtnBack.setOnClickListener { back(it) }

    }

    fun back(view: View){
        val action = AcceptedEventsFragmentDirections.actionAcceptedEventsFragmentToAnaSayfaFragment()
        Navigation.findNavController(requireView()).navigate(action)
    }


    fun get_accepted_events_fireStore(){
        val query = db.collection("JoiningEvents")
            .whereEqualTo("joiner_email",auth.currentUser?.email)
            .orderBy("generated_date", Query.Direction.DESCENDING)

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
                        val eventID = document.get("event_id") as String
                        val joinerEmail = document.get("joiner_email") as String
                        val eventHead = document.get("event_head") as String
                        val eventType = document.get("event_type") as String
                        val eventDate = document.get("event_date") as String
                        val eventClock = document.get("event_clock") as String
                        val eventLocate = document.get("event_locate") as String
                        val generated_date = document.getTimestamp("generated_date") as Timestamp
                        val acceptedEvent = AcceptedEvent(eventID,joinerEmail,eventHead,eventType,eventDate,eventClock,
                            eventLocate,generated_date)
                        acceptedEventsList.add(acceptedEvent)
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