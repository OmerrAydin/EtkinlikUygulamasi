package com.omeraydin.etkinlikprojesi.model

import com.google.firebase.Timestamp

data class GeneratedEvent(val eventID: String, val createrMail: String, val createrName: String, val head: String,
                          val explanation: String, val shortExplanation: String, val type: String, val location: String,
                          val latitude: Double, val longitude: Double, val date: String, val clock: String,
                          val base64String: String, val rate: Double, val generated_date: Timestamp)