package com.akdogan.simpledivelog.diveutil

import com.akdogan.simpledivelog.datalayer.DiveLogEntry

fun getSampleData(amount: Int, latestDiveNumber: Int = 0): List<DiveLogEntry> {
    val list: MutableList<DiveLogEntry> = mutableListOf()
    var date = 1546706393L // 05. Jan 2019 as start date for the sample Data
    repeat(amount) {
        val i = it + 1 + latestDiveNumber
        val d = getRandomDateLaterThan(date)
        list.add(
            DiveLogEntry(
                dataBaseId = "",
                diveNumber = i,
                diveLocation = getRandomLocation(),
                maxDepth = (25..90).random(),
                diveDuration = (30..55).random(),
                diveDate = d,
                imgUrl = getRandomThumbnailUrl()
            )
        )
        date = d
    }
    return list
}


fun getRandomDateLaterThan(date: Long): Long {
    val today = System.currentTimeMillis()
    return (date..today).random()
}

fun getRandomThumbnailUrl(): String {
    val list = listOf(
        "https://res.cloudinary.com/dcftx5e2/image/upload/v1619690226/viudxy4hltx4acfgank2.jpg",
        "https://res.cloudinary.com/dcftx5e2/image/upload/v1617893727/amhfgk7cyy9ogfebth0l.jpg",
        "https://res.cloudinary.com/dcftx5e2/image/upload/v1617893707/mitjqfzlvbkbdvb0fvrc.jpg",
        "https://res.cloudinary.com/dcftx5e2/image/upload/v1617393013/fzk2cik0srcxnx8gfldd.jpg",
        "https://res.cloudinary.com/dcftx5e2/image/upload/v1611771108/cmcq1y6wo9tibidt7lez.jpg",
        "https://res.cloudinary.com/dcftx5e2/image/upload/v1611749383/gujq2ct9kfkgpm7yl4jd.jpg",
        "https://res.cloudinary.com/dcftx5e2/image/upload/v1611676774/uoatrj0qwkmqzee51edl.jpg",
        "https://res.cloudinary.com/dcftx5e2/image/upload/v1621076595/uv2mlyl3xpjqak2f1cnp.jpg",
        "https://res.cloudinary.com/dcftx5e2/image/upload/v1621076621/ihqh6yxh9s1wy1thb7vf.jpg",
        "https://res.cloudinary.com/dcftx5e2/image/upload/v1621076657/tx97qsae0yfzaeyr3zyv.jpg",
        "https://res.cloudinary.com/dcftx5e2/image/upload/v1621077059/ycp0smafnc5u8zz9e1uy.jpg",
        "https://res.cloudinary.com/dcftx5e2/image/upload/v1621077167/iqbewhjgzd9r1lndj2g3.jpg"
    )

    return list[(list.indices.random())]
}


fun getRandomLocation(): String {
    val list = listOf(
        "Egypt",
        "Plötzensee",
        "Bathtub",
        "Carribean",
        "Australia",
        "Maledives",
        "Thailand",
        "Malta",
        "Mexiko",
        "Belize",
        "Columbia",
        "Great Barrier Reef",
        "Honduras",
        "Eilat",
        "Mallorca",
        "Teneriffa",
        "Lanzarote",
        "Malaysia",
        "Iceland",
        "Galapagos"
    )

    return list[(list.indices).random()]
}