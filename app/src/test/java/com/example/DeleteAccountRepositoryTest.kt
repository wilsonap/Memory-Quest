package com.example

import com.example.data.local.entity.PlayerEntity
import com.example.data.repository.DeleteAccountRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteAccountRepositoryTest {

    @Test
    fun resolveNormalizedName_prefersConfirmed() {
        val player = PlayerEntity(
            confirmedNormalizedName = "jogador1",
            pendingNormalizedName = "outro",
            name = "Terceiro"
        )
        assertEquals("jogador1", DeleteAccountRepository.resolveNormalizedNameForDeletion(player))
    }

    @Test
    fun resolveNormalizedName_usesPendingWhenConfirmedEmpty() {
        val player = PlayerEntity(
            pendingNormalizedName = "pendente",
            name = "Nome"
        )
        assertEquals("pendente", DeleteAccountRepository.resolveNormalizedNameForDeletion(player))
    }

    @Test
    fun resolveNormalizedName_emptyWhenNoData() {
        assertEquals("", DeleteAccountRepository.resolveNormalizedNameForDeletion(PlayerEntity()))
        assertEquals("", DeleteAccountRepository.resolveNormalizedNameForDeletion(null))
    }

    @Test
    fun resolveNormalizedName_emptyName_doesNotProduceInvalidPath() {
        val player = PlayerEntity(name = "   ")
        assertTrue(DeleteAccountRepository.resolveNormalizedNameForDeletion(player).isEmpty())
    }
}
