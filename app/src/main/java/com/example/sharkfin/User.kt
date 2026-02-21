package com.example.sharkfin

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    val uid: String = "",
    val email: String? = null,
    val displayName: String? = null,
    val accountType: String = "INDIVIDUAL", // INDIVIDUAL, JOINT, FAMILY, BUSINESS
    val primaryAccountId: String = "",
    @ServerTimestamp
    val createdAt: Date? = null
)
