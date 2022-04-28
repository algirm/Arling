package com.algirm.arling.domain.repository

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface AuthRepo {

    suspend fun signIn(username: String, password: String, sektor: Int): Flow<FirebaseUser?>

    suspend fun pilihSektor(sektor: Int): Flow<FirebaseUser?>

}