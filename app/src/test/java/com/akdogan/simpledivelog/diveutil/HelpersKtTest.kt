package com.akdogan.simpledivelog.diveutil

import com.akdogan.simpledivelog.diveutil.Constants.PASSWORD_MIN_LENGTH
import com.akdogan.simpledivelog.diveutil.Constants.PASSWORD_PATTERN
import com.akdogan.simpledivelog.diveutil.Constants.USERNAME_MIN_LENGTH
import com.akdogan.simpledivelog.diveutil.Constants.USERNAME_PATTERN
import org.junit.Assert.assertEquals
import org.junit.Test

class HelpersMatchPassword {
    @Test
    fun matchPassword_allowLettersNumbers(){
        assertEquals(true, matchPattern("abcd1234", PASSWORD_PATTERN))
    }

    @Test
    fun matchPassword_allowSpecialCharacters(){
        assertEquals(true, matchPattern("abc+$!_*123", PASSWORD_PATTERN))
    }

    @Test
    fun matchPassword_allowSpecialCharactersBeginningEnd(){
        assertEquals(true, matchPattern("+abcde123*", PASSWORD_PATTERN))
    }

    @Test
    fun matchPassword_disallowOtherSpecialCharacters(){
        assertEquals(false, matchPattern("abc%/~123", PASSWORD_PATTERN))
    }

    @Test
    fun matchPassword_disallowWhiteSpace(){
        assertEquals(false, matchPattern("abc 123", PASSWORD_PATTERN))
    }

    @Test
    fun matchPassword_disallowOneLessThanMin(){
        val testString = StringBuilder()
        repeat(PASSWORD_MIN_LENGTH - 1){
            testString.append("a")
        }
        assertEquals(false, matchPattern(testString.toString(), PASSWORD_PATTERN))
    }

    @Test
    fun matchPassword_allowMin(){
        val testString = StringBuilder()
        repeat(PASSWORD_MIN_LENGTH){
            testString.append("a")
        }
        assertEquals(true, matchPattern(testString.toString(), PASSWORD_PATTERN))
    }

    @Test
    fun matchPassword_allowMoreThanMin(){
        val testString = StringBuilder()
        repeat(PASSWORD_MIN_LENGTH + 2){
            testString.append("a")
        }
        assertEquals(true, matchPattern(testString.toString(), PASSWORD_PATTERN))
    }
}


class HelpersMatchUsername {

    @Test
    fun matchUsername_allowLetterNumbers() {
        assertEquals(true, matchPattern("1abCdefgh123V456789", USERNAME_PATTERN))
    }

    @Test
    fun matchUsername_allowSpecialCharacters(){
        assertEquals(true, matchPattern("ab+-_ba", USERNAME_PATTERN))
    }

    @Test
    fun matchUsername_disallowSpecialCharactersBeginning(){
        assertEquals(false, matchPattern("-abc123", USERNAME_PATTERN))
    }

    @Test
    fun matchUsername_allowSpecialCharactersEnd(){
        assertEquals(true, matchPattern("abc123_", USERNAME_PATTERN))
    }

    @Test
    fun matchUsername_disallowWhiteSpace(){
        assertEquals(false, matchPattern("abc 123", USERNAME_PATTERN))
    }

    @Test
    fun matchUsername_disallowOtherSpecialCharacters(){
        assertEquals(false, matchPattern("abc%/*!$?1a", USERNAME_PATTERN))
    }

    @Test
    fun matchUsername_disallowOneLessThanMin(){
        val testString = StringBuilder()
        repeat(USERNAME_MIN_LENGTH - 1){
            testString.append("a")
        }
        assertEquals(false, matchPattern(testString.toString(), USERNAME_PATTERN))
    }

    @Test
    fun matchUsername_allowMin(){
        val testString = StringBuilder()
        repeat(USERNAME_MIN_LENGTH){
            testString.append("a")
        }
        assertEquals(true, matchPattern(testString.toString(), USERNAME_PATTERN))
    }

    @Test
    fun matchUsername_allowMoreThanMin(){
        val testString = StringBuilder()
        repeat(USERNAME_MIN_LENGTH + 2){
            testString.append("a")
        }
        assertEquals(true, matchPattern(testString.toString(), USERNAME_PATTERN))
    }



}