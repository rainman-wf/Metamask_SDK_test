package ru.rainman.metamasksdktest.network.dto.base

import kotlinx.serialization.Serializable

@Serializable
data class JRPCError(
    val code: Int,
    val data: Data,
    val message: String,
) {

    @Serializable
    data class Data(
        val message: String,
        val data: String?,
        val txHash: String?
    )
}
