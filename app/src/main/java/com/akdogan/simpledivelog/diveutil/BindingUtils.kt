package com.akdogan.simpledivelog.diveutil

import android.icu.text.DateFormat
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseMethod
import com.akdogan.simpledivelog.datalayer.repository.DiveLogEntry
import kotlin.math.round


object UnitConversion {
    /*@InverseMethod("depthDisplayToDate")
    fun depthDataToDisplay(depth: Int, useMetric: Boolean): String? {
        var result = depth
        if (!useMetric){
            result = round(depth * 3.2808).toInt()
        }
        return result.toString()
    }

    fun depthDisplayToData(string: String, useMetric: Boolean): Int? {
        var result = string.toIntOrNull()
        if (result != null && !useMetric){
            result = round(result / 3.2808).toInt()
        }
        return result
    }*/
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

@BindingAdapter("diveNumberFormatted")
fun TextView.diveNumberFormatted(item: DiveLogEntry?) {
    item?.let{
        this.text = "#${it.diveNumber}"
    }
}

@BindingAdapter("diveDurationFormatted")
fun TextView.diveDurationFormatted(item: DiveLogEntry?) {
    item?.let{
        this.text = "${it.diveDuration} minutes"
    }
}


@BindingAdapter("maxDepthFormatted")
fun TextView.maxDepthFormatted(item: DiveLogEntry?) {
    item?.let{
        this.text = "${it.maxDepth} meters"
    }
}

@BindingAdapter("dateFormatted")
fun TextView.dateFormatted(item: DiveLogEntry?){
    item?.let{
        this.text = DateFormat.getDateInstance().format(item.diveDate)
    }
}






