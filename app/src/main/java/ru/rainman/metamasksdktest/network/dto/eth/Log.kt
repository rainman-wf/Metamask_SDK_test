package ru.rainman.metamasksdktest.network.dto.eth

import kotlinx.serialization.Serializable

@Serializable
data class Log(
    val address: String,
    val blockHash: String,
    val blockNumber: String,
    val data: String,
    val logIndex: String,
    val removed: Boolean,
    val topics: List<String>,
    val transactionHash: String,
    val transactionIndex: String
)