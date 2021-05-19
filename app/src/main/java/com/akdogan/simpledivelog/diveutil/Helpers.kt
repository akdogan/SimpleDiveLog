package com.akdogan.simpledivelog.diveutil

import androidx.databinding.InverseMethod
import kotlin.math.round

fun matchPattern(stringToTest: String?, pattern: String): Boolean{
    return if (stringToTest.isNullOrBlank()) false
        else stringToTest matches pattern.toRegex()
}




object UnitConversion {

    @InverseMethod("stringToDate")
    @JvmStatic fun dateToString(
        value: Int?
    ): String? {

        return value?.let{
            round(value * 3.2808).toString()
        } ?: null

    }

    @JvmStatic fun stringToDate(
        value: String?
    ): Int? {
        var result = value?.toIntOrNull()
        if (result != null){
            result = round(result / 3.2808).toInt()
        }
        return result
    }

}