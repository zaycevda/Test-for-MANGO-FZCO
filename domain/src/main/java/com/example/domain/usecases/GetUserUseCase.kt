package com.example.domain.usecases

import com.example.domain.repository.ApiRepository

class GetUserUseCase(private val repository: ApiRepository) {
    suspend fun execute() = repository.getUser()
}