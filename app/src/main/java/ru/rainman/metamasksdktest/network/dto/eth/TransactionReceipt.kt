package ru.rainman.metamasksdktest.network.dto.eth

import kotlinx.serialization.Serializable

@Serializable
data class TransactionReceipt(
    val blockHash: String,
    val blockNumber: String,
    val contractAddress: String?,
    val cumulativeGasUsed: String,
    val effectiveGasPrice: String,
    val from: String,
    val gasUsed: String,
    val logs: List<Log>,
    val logsBloom: String,
    val status: String,
    val to: String,
    val transactionHash: String,
    val transactionIndex: String,
    val type: String,
)