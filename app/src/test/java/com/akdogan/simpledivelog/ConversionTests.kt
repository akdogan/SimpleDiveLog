package com.akdogan.simpledivelog

import com.akdogan.simpledivelog.diveutil.UnitConverter
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversionTests {
    @Test
    fun depthToDataConversionOn(){
        val depth = 10
        val converter = UnitConverter(metricDepth = true, metricPressure = true)
        val expected = 33
        val result = converter.depthToData(depth)
        assertEquals(expected, result)
    }

    @Test
    fun depthToDataConversionOff(){
        val depth = 10
        val converter = UnitConverter(metricDepth = false, metricPressure = true)
        val expected = 10
        val result = converter.depthToData(depth)
        assertEquals(expected, result)
    }

    @Test
    fun depthToDisplayConversionOn(){
        val depth = 10
        val converter = UnitConverter(metricDepth = true, metricPressure = true)
        val expected = 3
        val result = converter.depthToDisplay(depth)
        assertEquals(expected, result)
    }

    @Test
    fun depthToDisplayConversionOff(){
        val depth = 10
        val converter = UnitConverter(metricDepth = false, metricPressure = true)
        val expected = 10
        val result = converter.depthToDisplay(depth)
        assertEquals(expected, result)
    }

    @Test
    fun pressureToDataConversionOn(){
        val press = 200//3000
        val converter = UnitConverter(metricDepth = true, metricPressure = true)
        val expected = 2901//207
        val result = converter.pressureToData(press)
        assertEquals(expected, result)
    }

    @Test
    fun pressureToDataConversionOff(){
        val press = 200
        val converter = UnitConverter(metricDepth = true, metricPressure = false)
        val expected = 200
        val result = converter.pressureToData(press)
        assertEquals(expected, result)
    }

    @Test
    fun pressureToDisplayConversionOn(){
        val press = 3000
        val converter = UnitConverter(metricDepth = true, metricPressure = true)
        val expected = 207
        val result = converter.pressureToDisplay(press)
        assertEquals(expected, result)
    }

    @Test
    fun pressureToDisplayConversionOff(){
        val press = 200
        val converter = UnitConverter(metricDepth = true, metricPressure = false)
        val expected = 200
        val result = converter.pressureToDisplay(press)
        assertEquals(expected, result)
    }

}