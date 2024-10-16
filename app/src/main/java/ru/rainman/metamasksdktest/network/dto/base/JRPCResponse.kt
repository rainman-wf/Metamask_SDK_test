package ru.rainman.metamasksdktest.network.dto.base

import kotlinx.serialization.Serializable

@Serializable
data class JRPCResponse<T>(
    val jsonrpc: String,
    val result: T? = null,
    val id: Int,
    val error: JRPCError? = null
)
