package com.algirm.arling.util

sealed class Resource<out T> {
    class Loading<out T> : Resource<T>()
    class Init<out T> : Resource<T>()
    data class Success<out T>(val data: T) : Resource<T>()
    data class Failure<T>(val throwable: Throwable?, val data: T?) : Resource<T>()
}
