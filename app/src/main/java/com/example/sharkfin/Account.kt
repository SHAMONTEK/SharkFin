package com.example.sharkfin

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Account(
    val accountId: String = "",
    val type: String = "INDIVIDUAL", // INDIVIDUAL, JOINT, FAMILY, BUSINESS
    val name: String = "",
    val createdByUid: String = "",
    @ServerTimestamp
    val createdAt: Date? = null
)
