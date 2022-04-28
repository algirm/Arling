package com.algirm.arling.data.repository

import android.location.Location
import com.algirm.arling.data.model.Petugas
import com.algirm.arling.domain.repository.PetugasRepo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PetugasRepoImpl @Inject constructor(
    private val firebaseDb: FirebaseDatabase,
    private val firebaseAuth: FirebaseAuth
) : PetugasRepo {

    private fun getSectorRef(): String {
//        return firebaseUser!!.displayName!!.split("?")[1]
        val firebaseUser = firebaseAuth.currentUser
        val sektor = firebaseUser!!.displayName!!.split("?")[1].toInt()
        return if (sektor < 1) {
            (firebaseUser.displayName!!.filter { it.isDigit() }[0]).toString()
        } else {
            (sektor + 10).toString()
        }
    }

    override suspend fun getAllOnce() = flow {
        val firebaseUser = firebaseAuth.currentUser
        val result = ArrayList<Petugas>()
        firebaseDb.getReference(getSectorRef()).get().await().children.forEach {
            if (it.key != firebaseUser!!.uid) {
                val petugas = it.getValue(Petugas::class.java)!!
                petugas.uid = it.key!!
                result.add(petugas)
            }
        }
        emit(result)
    }

    override suspend fun getUserData() = flow {
        val firebaseUser = firebaseAuth.currentUser
        val result =
            firebaseDb.getReference(getSectorRef()).child(firebaseUser!!.uid).get().await()
        val userData = result.getValue(Petugas::class.java)!!
        emit(userData)
    }

    override suspend fun updateUserData(location: Location) {
        val firebaseUser = firebaseAuth.currentUser
        val updateLocMap = HashMap<String, Any>()
        updateLocMap["lat"] = location.latitude
        updateLocMap["lon"] = location.longitude
        firebaseDb.getReference(getSectorRef()).child(firebaseUser!!.uid).updateChildren(updateLocMap).await()
    }

    override suspend fun setPing(ping: Boolean) {
        val firebaseUser = firebaseAuth.currentUser
        val updateLocMap = HashMap<String, Any>()
        updateLocMap["ping"] = ping
        firebaseDb.getReference(getSectorRef()).child(firebaseUser!!.uid).updateChildren(updateLocMap).await()
    }

}