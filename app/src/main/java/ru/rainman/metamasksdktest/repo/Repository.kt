package ru.rainman.metamasksdktest.repo

import android.util.Log
import io.metamask.androidsdk.EthereumFlowWrapper
import io.metamask.androidsdk.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import org.web3j.abi.EventEncoder
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Bool
import org.web3j.abi.datatypes.DynamicBytes
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.Sign
import org.web3j.crypto.TransactionEncoder
import ru.rainman.metamasksdktest.CONTRACT_ADDRESS
import ru.rainman.metamasksdktest.VERIFIER
import ru.rainman.metamasksdktest.WALLET_PRIVATE
import ru.rainman.metamasksdktest.network.EthereumJRPCApi
import ru.rainman.metamasksdktest.network.dto.contract.NewChannel
import ru.rainman.metamasksdktest.network.dto.eth.Params
import ru.rainman.metamasksdktest.network.dto.eth.RequestBody
import ru.rainman.metamasksdktest.network.dto.eth.TransactionReceipt
import ru.rainman.metamasksdktest.repo.model.ChannelMeta
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Repository @Inject constructor(
    private val ethereum: EthereumFlowWrapper,
    private val ethereumApi: EthereumJRPCApi,
) {

    private val connection =
        ethereum.ethereumState.stateIn(CoroutineScope(Dispatchers.IO), SharingStarted.Eagerly, null)

    suspend fun createChannel(newChannel: NewChannel): ChannelMeta? {

        val metamask = true // switch sign otpions

        val state = connection.value ?: return null

        val chainId = state.chainId.removePrefix("0x").toLong(16)

        val address = state.selectedAddress

        if (address.isNullOrEmpty()) return null

        val function = buildFunction(newChannel) // your function

        val encodedFunction = FunctionEncoder.encode(function)

        val transactionParams = requestTransactionParams(address, encodedFunction)

        val transaction = buildTransaction(
            transactionParams[1]!!,
            transactionParams[2]!!,
            transactionParams[3]!!,
            encodedFunction
        )

        val txSign = signTransaction(transaction, chainId, metamask)

        val transactionHash = sendTransactionAndGetHash(txSign)

        val transactionReceipt = getTransactionReceipt(transactionHash)

        val channelAddress = fetchChannelAddress(transactionReceipt)

        val getChannelFunc = createGetChannelFunction(channelAddress)

        val getChannelResult = getChannelByAddress(getChannelFunc, address)

        return getChannelResult
    }


    private fun buildFunction(newChannel: NewChannel): Function {
        val _title = DynamicBytes(newChannel.title.toByteArray())
        val _description = DynamicBytes(newChannel.description.toByteArray())
        val _channelPrice = Uint256(0)
        val _isClosed = Bool(false)
        val _verifier = Address(VERIFIER)

        return Function(
            "bind",
            listOf<Type<*>>(_title, _description, _channelPrice, _isClosed, _verifier),
            listOf<TypeReference<*>>(object : TypeReference<ChannelMeta?>() {})
        )
    }

    private suspend fun requestTransactionParams(
        myAddress: String,
        encodedFunction: String,
    ): Map<Int, BigInteger> {
        return ethereumApi.sendBatchCallTransaction(
            listOf(
                RequestBody(
                    id = 1,
                    method = "eth_getTransactionCount",
                    params = listOf(Params.Text(myAddress), Params.Text("pending")),
                    jsonrpc = "2.0"
                ),
                RequestBody(
                    id = 2,
                    jsonrpc = "2.0",
                    params = listOf(),
                    method = "eth_gasPrice"
                ),
                RequestBody(
                    id = 3,
                    jsonrpc = "2.0",
                    params = listOf(
                        Params.Input(
                            from = myAddress,
                            to = CONTRACT_ADDRESS,
                            input = encodedFunction
                        ),
                        Params.Text("latest")
                    ),
                    method = "eth_estimateGas"
                )
            )
        )
            .body()!!
            .map { it.id to BigInteger(it.result!!.removePrefix("0x"), 16) }
            .toMap()
    }

    private fun buildTransaction(
        nonce: BigInteger,
        gasPrise: BigInteger,
        gasLimit: BigInteger,
        encodedFunction: String,
    ): RawTransaction {
        return RawTransaction.createTransaction(
            nonce,
            gasPrise,
            gasLimit,
            CONTRACT_ADDRESS,
            BigInteger.ZERO,
            encodedFunction
        )
    }

    private suspend fun signTransaction(
        transaction: RawTransaction,
        chainId: Long,
        metamask: Boolean,
    ): String {

        val encoded = TransactionEncoder.encode(transaction, chainId)

        val signedData = getSignData(encoded, metamask)

        val eipSign = TransactionEncoder.createEip155SignatureData(
            signedData,
            chainId
        )

        val txSigned = TransactionEncoder.encode(transaction, eipSign)

        return txSigned.toHex();
    }

    private suspend fun getSignData(
        encodedTransaction: ByteArray,
        metamask: Boolean,
    ): Sign.SignatureData {
        return if (metamask)
            Sign.signatureDataFromHex(
                (ethereum.connectSign(encodedTransaction.toHex()) as? Result.Success.Item)?.value!!
            )
        else Sign.signMessage(encodedTransaction, Credentials.create(WALLET_PRIVATE).ecKeyPair)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun ByteArray.toHex(): String {
        return "0x${toHexString(HexFormat.Default)}"
    }

    private suspend fun sendTransactionAndGetHash(transactionSign: String): String {
        return ethereumApi.sendEthTransaction(
            RequestBody(
                method = "eth_sendRawTransaction",
                params = listOf(Params.Text(transactionSign)),
                jsonrpc = "2.0",
                id = 1
            )
        ).let {
            it.errorBody()
                ?.let { throw RuntimeException(it.string()) }
                ?: it.body()
                ?: throw RuntimeException("Response body is null")
        }.result ?: throw RuntimeException("Result is null")
    }

    private suspend fun getTransactionReceipt(hash: String): TransactionReceipt {
        return ethereumApi.getTransactionReceipt(
            RequestBody(
                id = 1,
                jsonrpc = "2.0",
                method = "eth_getTransactionReceipt",
                params = listOf(
                    Params.Text(hash)
                )
            )
        ).let {
            it.errorBody()
                ?.let { throw RuntimeException(it.string()) }
                ?: it.body()
                ?: throw RuntimeException("Response body is null")
        }.result ?: throw RuntimeException("Result is null")
    }

    private fun fetchChannelAddress(transactionReceipt: TransactionReceipt): String {

        val eventHash = EventEncoder.buildEventSignature("channelCreated(address)")

        val log = transactionReceipt.logs
            .singleOrNull { it.topics[0] == eventHash }
            ?: throw RuntimeException("Target log is not exists")

        return log.topics[1]
    }

    private suspend fun createGetChannelFunction(channelAddress: String): Function {
        return Function(
            "getChannelByAddress",
            listOf<Type<*>>(Address(channelAddress)),
            listOf<TypeReference<*>>(object : TypeReference<ChannelMeta>() {})
        )
    }

    private suspend fun getChannelByAddress(function: Function, myAddress: String): ChannelMeta {
        val response = ethereumApi.ethCall(
            RequestBody(
                method = "eth_call",
                jsonrpc = "2.0",
                id = 1,
                params = listOf(
                    Params.Input(
                        from = myAddress,
                        to = CONTRACT_ADDRESS,
                        input = FunctionEncoder.encode(function)
                    )
                )
            )
        ).let {
            it.errorBody()?.let {
                throw RuntimeException(it.string())
            } ?: it.body() ?: throw RuntimeException("Response body is null")
        }.result ?: throw RuntimeException("Result is null")

        return FunctionReturnDecoder.decode(response, function.outputParameters)[0] as ChannelMeta
    }
}