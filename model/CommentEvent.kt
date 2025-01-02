package com.omeraydin.etkinlikprojesi.model

import com.google.firebase.Timestamp

data class CommentEvent(val eventID: String, val senderName: String, val senderEmail: String,
                        val comment: String, val rate: Double, val generatedDate: Timestamp)
