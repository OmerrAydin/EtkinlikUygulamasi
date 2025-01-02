package com.omeraydin.etkinlikprojesi.model

import com.google.firebase.Timestamp

data class AcceptedEvent(val eventID: String, val joinerEmail: String, val eventHead: String, val eventType: String,
                         val eventDate:String, val eventClock: String, val eventLocate: String, val generatedDate: Timestamp)
