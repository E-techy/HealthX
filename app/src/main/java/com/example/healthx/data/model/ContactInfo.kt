package com.example.healthx.data.model

data class ContactInfo(
    val phoneNumber: String,
    val alternatePhoneNumber: String? = null,
    val address: Address,
    val emergencyContact: EmergencyContact
)
data class Address(val street: String, val landmark: String?, val city: String, val state: String, val zipCode: String, val country: String)
data class EmergencyContact(val name: String, val relation: String, val phoneNumber: String, val alternatePhoneNumber: String?)