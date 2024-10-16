package ru.rainman.metamasksdktest.network.dto.contract

data class NewChannel(
    val title: String,
    val description: String,
    val isPrivate: Boolean,
    val price: Long,
)