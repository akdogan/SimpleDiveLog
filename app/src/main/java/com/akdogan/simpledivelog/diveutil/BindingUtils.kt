package com.akdogan.simpledivelog.diveutil

import android.icu.text.DateFormat
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseMethod
import com.akdogan.simpledivelog.R
import com.akdogan.simpledivelog.datalayer.DiveLogEntry
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
        //this.text = "#${it.diveNumber}"
        this.text = resources.getString(R.string.detail_view_dive_number_content, it.diveNumber)

    }
}

@BindingAdapter("diveDurationFormatted")
fun TextView.diveDurationFormatted(item: DiveLogEntry?) {
    item?.let{
        //this.text = "${it.diveDuration} minutes"
        this.text = resources.getString(R.string.detail_view_duration_content, it.diveDuration)
    }
}


@BindingAdapter("maxDepthFormatted")
fun TextView.maxDepthFormatted(item: DiveLogEntry?) {
    item?.let{
        //this.text = "${it.maxDepth} meters"
        this.text = resources.getString(R.string.detail_view_maxdepth_content, it.maxDepth)
    }
}

@BindingAdapter("dateFormatted")
fun TextView.dateFormatted(item: DiveLogEntry?){
    item?.let{
        this.text = DateFormat.getDateInstance().format(item.diveDate)
    }
}






