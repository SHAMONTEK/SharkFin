package com.example.sharkfin // Ensure this matches your package name at the top of MainActivity

data class User(
    val uid: String = "",
    val email: String = "",
    val accountType: String = "Individual",
    val role: String = "user" // Options: "user" or "admin"
)