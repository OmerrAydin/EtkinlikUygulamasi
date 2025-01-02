package com.omeraydin.etkinlikprojesi.model

import java.io.Serializable


data class EventForMaps (val eventID: String, val createrMail: String, val createrName: String, val head: String,
                   val explanation: String, val shortExplanation: String, val type: String, val location: String,
                   val latitude: Double, val longitude: Double, val date: String, val clock: String, val rate: Double): Serializable