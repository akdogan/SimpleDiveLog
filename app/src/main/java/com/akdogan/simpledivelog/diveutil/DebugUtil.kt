package com.akdogan.simpledivelog.diveutil


import com.akdogan.simpledivelog.datalayer.repository.DiveLogEntry



fun getSampleData(amount: Int, latestDiveNumber: Int = 0): List<DiveLogEntry>{
    val list : MutableList<DiveLogEntry> = mutableListOf()
    var date = 1546706393L // 05. Jan 2019 as start date for the sample Data
    repeat(amount){
        val i = it + 1 + latestDiveNumber
        val d = getRandomDateLaterThan(date)
        list.add(
            DiveLogEntry(
                dataBaseId = "",
                diveNumber = i,
                diveLocation = getRandomLocation(),
                maxDepth = (25..90).random(),
                diveDuration = (30..55).random(),
                diveDate = d
            )
        )
        date = d
    }
    return list
}


fun getRandomDateLaterThan(date: Long): Long{
    val today = System.currentTimeMillis()
    return (date..today).random()
}

fun getRandomLocation(): String{
    val list = listOf<String>(
        "Ägypten",
        "Plötze",
        "Badewanne",
        "Karibik",
        "Australien",
        "Malediven",
        "Thailand",
        "Malta",
        "Mexiko",
        "Belize",
        "Kolumbien",
        "Great Barrier Reef",
        "Honduras",
        "Eilat",
        "Mallorca",
        "Teneriffa",
        "Lanzarote",
        "Malaysien",
        "Island",
        "Düsterer Tümpel"
    )

    return list[(list.indices).random()]
}