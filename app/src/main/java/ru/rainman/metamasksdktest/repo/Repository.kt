package ru.rainman.metamasksdktest.repo

import android.graphics.ColorSpace.match
import android.util.Log
import io.metamask.androidsdk.EthereumFlowWrapper
import io.metamask.androidsdk.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Bool
import org.web3j.abi.datatypes.DynamicBytes
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECDSASignature
import org.web3j.crypto.Hash
import org.web3j.crypto.Keys
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.Sign
import org.web3j.crypto.Sign.SignatureData
import org.web3j.crypto.TransactionEncoder
import ru.rainman.metamasksdktest.*
import ru.rainman.metamasksdktest.network.EthereumJRPCApi
import ru.rainman.metamasksdktest.network.dto.contract.NewChannel
import ru.rainman.metamasksdktest.network.dto.eth.Params
import ru.rainman.metamasksdktest.network.dto.eth.RequestBody
import ru.rainman.metamasksdktest.repo.model.ChannelMeta
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.Charsets.UTF_16
import kotlin.text.Charsets.UTF_32
import kotlin.text.Charsets.UTF_8


@Singleton
class Repository @Inject constructor(
    private val ethereum: EthereumFlowWrapper,
    private val ethereumApi: EthereumJRPCApi,
) {

    private val connection =
        ethereum.ethereumState.stateIn(CoroutineScope(Dispatchers.IO), SharingStarted.Eagerly, null)

    suspend fun createChannel(newChannel: NewChannel): String {

        val address = SUPER_ADMIN_ADRESS
        val chainId = TEST_CHAIN_ID // 1337
        val private = SUPER_ADMIN_PRIVATE

        val function = buildFunction(newChannel) // your function

        val encodedFunction = FunctionEncoder.encode(function)

        val transactionParams = requestTransactionParams(address, encodedFunction)

        val transaction = buildTransaction(
            transactionParams[1]!!,
            transactionParams[2]!!,
            transactionParams[3]!!,
            encodedFunction
        )

        val encoded = TransactionEncoder.encode(transaction, chainId)

        val vrs = arrayOf(
            Sign.signatureDataFromHex((ethereum.connectSign("0x" + encoded.toHex()) as? Result.Success.Item)?.value!!),
            Sign.signMessage(encoded, Credentials.create(private).ecKeyPair)
        )

        val metaSign = vrs[0].r.plus(vrs[0].s).plus(vrs[0].v).toHex()
        val web3sign = vrs[1].r.plus(vrs[1].s).plus(vrs[1].v).toHex()

        val txSign = signTransaction(transaction, chainId, vrs[1])

        Log.i("TAG", "createChannel: metaSign = $metaSign")
        Log.i("TAG", "createChannel: web3Sign = $web3sign")

        testRecoverAddressFromSignature(vrs[0], encoded.toHex())
        testRecoverAddressFromSignature(vrs[1], encoded.toHex())

        return sendTransactionAndGetHash(txSign)
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
        signedData: Sign.SignatureData,
    ): String {

        val eipSign = TransactionEncoder.createEip155SignatureData(
            signedData,
            chainId
        )

        val txSigned = TransactionEncoder.encode(transaction, eipSign)

        return txSigned.toHex();
    }

    private fun ByteArray.toHex(): String {
       return map {
            String.format("%02x", it)
        }.joinToString("")
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

    fun testRecoverAddressFromSignature(sd: SignatureData, message: String) {

        val PERSONAL_MESSAGE_PREFIX: String = "\u0019Ethereum Signed Message:\n"

        val prefix: String = PERSONAL_MESSAGE_PREFIX + message.length
        val msgHash = Hash.sha3((prefix + message).toByteArray())

        for (i in 0..3) {
            val publicKey =
                Sign.recoverFromSignature(
                    i,
                    ECDSASignature(BigInteger(1, sd.r), BigInteger(1, sd.s)),
                    msgHash
                )
            publicKey?.let {
                Log.d("TAG", "fetch adress: 0x${Keys.getAddress(it)}")
            }

        }
    }
}