package com.algirm.arling.data.repository

import com.algirm.arling.domain.repository.AuthRepo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepoImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseDatabase: FirebaseDatabase
) : AuthRepo {

    private fun getSectorRef(): String {
        val firebaseUser = firebaseAuth.currentUser

        return if (firebaseUser != null) {
            val sektor = firebaseUser.displayName!!.split("?")[1].toInt()
            if (sektor < 1) {
                (firebaseUser.displayName!!.filter { it.isDigit() }[0]).toString()
            } else {
                (sektor + 10).toString()
            }
        } else {
            "9"
        }
    }

    override suspend fun pilihSektor(sektor: Int) = flow {
        val user = firebaseAuth.currentUser
        val username = user!!.displayName.toString() // ex: arl_401?0
        val nickname = username.split("?")[0] // -> arl_401
        val pos = if (sektor < 1) {
            nickname.filter { it.isDigit() }[0]
        } else {
            sektor + 10
        }

        // update user
        user.updateProfile(
            UserProfileChangeRequest.Builder()
                .setDisplayName("$nickname?$sektor")
                .build()
        ).await()

        // create child on db
        firebaseDatabase.getReference(pos.toString())
            .child(user.uid)
            .updateChildren(
                HashMap<String, Any>().apply {
                    val displayName = user.displayName.toString()
                    this["name"] = displayName.split("?")[0]
                    this["ping"] = false
                }
            ).await()

        emit(user)
    }

    override suspend fun signIn(username: String, password: String, sektor: Int) = flow {
        firebaseAuth.signInWithEmailAndPassword(username, password).await()
        val user = firebaseAuth.currentUser
        val nickname = username.split("@")[0]

        if (user != null) {
            // update display name
            user.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName("$nickname?$sektor")
                    .build()
            ).await()

            emit(user)
        } else {
            emit(null)
        }
    }
}