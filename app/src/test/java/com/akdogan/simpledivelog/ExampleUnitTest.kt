package com.akdogan.simpledivelog

import com.akdogan.simpledivelog.diveutil.getRandomDateLaterThan
import com.akdogan.simpledivelog.diveutil.getSampleData
import com.akdogan.simpledivelog.application.editview.toStringOrNull
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {


    @Test
    fun testRandomDate() {
        val start = 1546706393L
        val date: Long = getRandomDateLaterThan(start)
        assertTrue("Date lager than", date > start)

    }

    @Test
    fun testMockDataDate() {
        val list = getSampleData(2)
        assertTrue("Date larger than: ", list[0].diveDate < list[1].diveDate)
    }

    @Test
    fun testMockDataDateMultiple(){
        val list = getSampleData(10)
        var pass = true
        repeat(list.size-2){
            if (list[it].diveDate >= list[it+1].diveDate) {
                pass = false
            }
        }
        assertTrue("Dates larger than failed: ", pass)
    }


    @Test
    fun testToStringOrNullWithValue(){
        val value: Int? = 256
        val result = value.toStringOrNull()
        assertEquals(value.toString(), result)
    }

    @Test
    fun testToStringOrNullWithZero(){
        val value: Int? = 0
        val result = value.toStringOrNull()
        assertEquals(value.toString(), result)
    }

    @Test
    fun testToStringOrNullWithNull(){
        val value: Int? = null
        val result = value.toStringOrNull()
        assertEquals(null, result)
    }
}