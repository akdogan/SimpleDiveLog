package com.akdogan.simpledivelog.diveutil

import kotlin.math.round


class UnitConverter(
    var metricDepth: Boolean,
    var metricPressure: Boolean
    ){

    companion object {
        const val METER_TO_FEET = 3.2808
        const val BAR_TO_PSI = 14.5038
    }

    fun depthToData(depth: Int): Int{
        return if (metricDepth){
            round(depth * METER_TO_FEET).toInt()
        } else {
            depth
        }
    }

    fun depthToDisplay(depth: Int): Int{
        return if (metricDepth){
            round(depth / METER_TO_FEET).toInt()
        } else {
            depth
        }
    }

    fun pressureToData(press: Int?): Int?{
        return if (press == null || !metricPressure){
            press
        } else {
            round(press * BAR_TO_PSI).toInt()
        }
    }

    fun pressureToDisplay(press: Int?): Int?{
        return if (press == null || !metricPressure){
            press
        } else {
            round(press / BAR_TO_PSI).toInt()
        }
    }
}
