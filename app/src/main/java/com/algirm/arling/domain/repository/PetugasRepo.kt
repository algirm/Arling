package com.algirm.arling.domain.repository

import android.location.Location
import com.algirm.arling.data.model.Petugas
import kotlinx.coroutines.flow.Flow

interface PetugasRepo {

    suspend fun getUserData(): Flow<Petugas>

    suspend fun getAllOnce(): Flow<List<Petugas>>

    suspend fun updateUserData(location: Location)

    suspend fun setPing(ping: Boolean)

}