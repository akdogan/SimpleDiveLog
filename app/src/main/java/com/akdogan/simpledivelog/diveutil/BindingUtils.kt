package com.akdogan.simpledivelog.diveutil

import android.icu.text.DateFormat
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.akdogan.simpledivelog.datalayer.repository.DiveLogEntry


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
        this.text = "${DateFormat.getDateInstance().format(item.diveDate)}"
    }
}




