package ru.rainman.metamasksdktest.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import ru.rainman.metamasksdktest.network.dto.base.JRPCResponse
import ru.rainman.metamasksdktest.network.dto.eth.RequestBody
import ru.rainman.metamasksdktest.network.dto.eth.TransactionReceipt


interface EthereumJRPCApi {
    @POST(".")
    suspend fun sendEthTransaction(@Body transaction: RequestBody) : Response<JRPCResponse<String>>

    @POST(".")
    suspend fun getTransactionReceipt(@Body transaction: RequestBody) : Response<JRPCResponse<TransactionReceipt>>

    @POST(".")
    suspend fun sendBatchCallTransaction(@Body list: List<RequestBody>) : Response<List<JRPCResponse<String>>>

    @POST(".")
    suspend fun ethCall(@Body transaction: RequestBody) : Response<JRPCResponse<String>>
}