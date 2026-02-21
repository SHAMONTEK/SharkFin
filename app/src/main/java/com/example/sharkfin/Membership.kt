package com.example.sharkfin

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Membership(
    val uid: String = "",
    val role: String = "MEMBER", // OWNER, ADMIN, ORGANIZER, MEMBER, EMPLOYEE
    val status: String = "ACTIVE", // ACTIVE, INVITED
    @ServerTimestamp
    val joinedAt: Date? = null
)
