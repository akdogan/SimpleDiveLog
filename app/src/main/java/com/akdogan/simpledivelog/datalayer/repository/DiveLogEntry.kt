/*
 * Copyright 2019, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.akdogan.simpledivelog.datalayer.repository

import com.squareup.moshi.Json

data class DiveLogEntry(
        @Json(name = "id") var dataBaseId: String,
        val diveNumber: Int,
        val diveDuration: Int, // stored in minutes
        val maxDepth: Int, // stored in feet
        @Json(name = "location")val diveLocation: String,
        var diveDate: Long,
        val weight: Int? = null, // stored in Pound
        val airIn: Int? = null, // stored in PSI
        val airOut: Int? = null, // stored in PSI
        val notes: String? = null
)

