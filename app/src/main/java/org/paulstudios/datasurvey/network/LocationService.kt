package org.paulstudios.datasurvey.network

import java.util.Random

fun generateUniqueFileName(): String {
    val random = Random()
    val uniqueNumber = (1..15).map { random.nextInt(10) }.joinToString("")
    return uniqueNumber
}


