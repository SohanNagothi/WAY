package com.example.way.di

import com.example.way.data.repository.AuthRepository
import com.example.way.data.repository.AuthRepositoryImpl
import com.example.way.data.repository.ContactsRepository
import com.example.way.data.repository.ContactsRepositoryImpl
import com.example.way.data.repository.LocationRepository
import com.example.way.data.repository.LocationRepositoryImpl
import com.example.way.data.repository.WalkSessionRepository
import com.example.way.data.repository.WalkSessionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds repository interfaces to their implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindContactsRepository(impl: ContactsRepositoryImpl): ContactsRepository

    @Binds
    @Singleton
    abstract fun bindWalkSessionRepository(impl: WalkSessionRepositoryImpl): WalkSessionRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository
}
