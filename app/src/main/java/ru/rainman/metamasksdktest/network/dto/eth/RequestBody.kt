package ru.rainman.metamasksdktest.network.dto.eth
import kotlinx.serialization.Serializable

@Serializable
data class RequestBody(
    val method: String,
    val params: List<Params>,
    val id: Int,
    val jsonrpc: String,
)


