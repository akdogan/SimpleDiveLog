package com.akdogan.simpledivelog.diveutil

import android.icu.text.DateFormat
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseMethod
import com.akdogan.simpledivelog.R
import com.akdogan.simpledivelog.datalayer.DiveLogEntry
import kotlin.math.round




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






