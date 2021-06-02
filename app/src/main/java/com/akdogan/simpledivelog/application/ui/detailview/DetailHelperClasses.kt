package com.akdogan.simpledivelog.application.ui.detailview

import android.content.res.Resources
import android.icu.text.DateFormat
import androidx.annotation.StringRes
import com.akdogan.simpledivelog.R
import com.akdogan.simpledivelog.datalayer.DiveLogEntry

internal fun DiveLogEntry.toDetailItemsList(res: Resources): List<DetailListItem> {
    return listOf(
        DetailListItem(
            res.getString(R.string.detail_view_dive_number_label),
            res.getString(R.string.detail_view_dive_number_content, this.diveNumber.toString())
        ),
        DetailListItem(
            res.getString(R.string.detail_view_date_label),
            DateFormat.getDateInstance().format(this.diveDate)
        ),
        DetailListItem(
            res.getString(R.string.detail_view_location_label),
            this.diveLocation
        ),
        DetailListItem(
            res.getString(R.string.detail_view_duration_label),
            res.getString(R.string.detail_view_duration_content, this.diveDuration.toString())
        ),
        DetailListItem(
            res.getString(R.string.detail_view_maxdepth_label),
            res.getString(R.string.detail_view_maxdepth_content, this.maxDepth.toString())
        ),
        DetailListItem(
            res.getString(R.string.detail_view_weight_label),
            this.weight.formatOrReplace(res, R.string.detail_view_weight_content)
        ),
        DetailListItem(
            res.getString(R.string.detail_view_air_in_label),
            this.airIn.formatOrReplace(res, R.string.detail_view_air_content)
        ),
        DetailListItem(
            res.getString(R.string.detail_view_air_out_label),
            this.airOut.formatOrReplace(res, R.string.detail_view_air_content)
        ),
        DetailListItem(
            res.getString(R.string.detail_view_notes_label),
            this.notes ?: res.getString(R.string.detail_view_placeholder_item_no_data)
        )
    )

}

private fun Int?.formatOrReplace(
    res: Resources,
    @StringRes resource: Int,
): String {
    return if (this == null){
        res.getString(R.string.detail_view_placeholder_item_no_data)
    } else {
        res.getString(resource, this.toString())
    }
}