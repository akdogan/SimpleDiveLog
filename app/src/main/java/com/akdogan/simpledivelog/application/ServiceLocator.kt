package com.akdogan.simpledivelog.application

import android.content.Context
import com.akdogan.simpledivelog.datalayer.repository.DefaultDataRepository
import com.akdogan.simpledivelog.datalayer.repository.DataRepository

object ServiceLocator {

    lateinit var repo: DataRepository
        private set

    fun setupDefaultRepository(context: Context){
        repo = DefaultDataRepository.getDefaultRepository(context)
    }

    fun setupTestRepository(testRepo: DataRepository){
        repo = testRepo
    }

}