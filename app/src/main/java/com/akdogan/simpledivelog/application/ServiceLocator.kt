package com.akdogan.simpledivelog.application

import android.content.Context
import com.akdogan.simpledivelog.datalayer.repository.DefaultRepository
import com.akdogan.simpledivelog.datalayer.repository.Repository

object ServiceLocator {

    lateinit var repo: Repository
        private set

    fun setupDefaultRepository(context: Context){
        repo = DefaultRepository.getDefaultRepository(context)
    }

    fun setupTestRepository(testRepo: Repository){
        repo = testRepo
    }

}