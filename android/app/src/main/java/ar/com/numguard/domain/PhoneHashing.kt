package ar.com.numguard.domain

import java.security.MessageDigest

object PhoneHashing {

    fun sha256(e164: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(e164.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
