package com.algirm.arling.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algirm.arling.domain.repository.AuthRepo
import com.algirm.arling.util.Resource
import com.algirm.rondar.util.DispatcherProvider
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val dispatcher: DispatcherProvider,
    private val authRepo: AuthRepo
) : ViewModel() {

    private val _authResult = MutableStateFlow<Resource<FirebaseUser>>(Resource.Init())
    val authResult: StateFlow<Resource<FirebaseUser>> = _authResult

    private val _selectResult = MutableStateFlow<Resource<FirebaseUser>>(Resource.Init())
    val selectResult: StateFlow<Resource<FirebaseUser>> = _selectResult

    fun signIn(username: String, password: String, sektor: Int) = viewModelScope.launch {
        _authResult.value = Resource.Loading()
        try {
            withContext(dispatcher.io) {
                authRepo.signIn(username, password, sektor).collect { user ->
                    _authResult.value = Resource.Success(user!!)
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
            _authResult.value = Resource.Failure(e, null)
        }
    }

    fun pilihSektor(sektor: Int) = viewModelScope.launch {
        _selectResult.value = Resource.Loading()
        try {
            withContext(dispatcher.io) {
                authRepo.pilihSektor(sektor).collect { user ->
                    _selectResult.value = Resource.Success(user!!)
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
            _selectResult.value = Resource.Failure(e, null)
        }
    }

}