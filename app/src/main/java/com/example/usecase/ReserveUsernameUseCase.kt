package com.example.usecase

import com.example.data.repository.UsernameRepository
import com.example.data.repository.UsernameReservationResult

class ReserveUsernameUseCase(
    private val usernameRepository: UsernameRepository
) {
    suspend fun execute(displayName: String, isOnline: Boolean): UsernameReservationResult {
        return usernameRepository.reserveUsername(displayName, isOnline)
    }
}
