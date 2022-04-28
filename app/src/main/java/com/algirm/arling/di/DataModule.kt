package com.algirm.arling.di

import com.algirm.arling.data.repository.AuthRepoImpl
import com.algirm.arling.data.repository.PetugasRepoImpl
import com.algirm.arling.domain.repository.AuthRepo
import com.algirm.arling.domain.repository.PetugasRepo
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Singleton
    @Binds
    abstract fun bindPetugasRepo(petugasRepoImpl: PetugasRepoImpl): PetugasRepo

    @Singleton
    @Binds
    abstract fun bindAuthRepo(authRepoImpl: AuthRepoImpl): AuthRepo

}