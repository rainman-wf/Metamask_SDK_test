package tech.imbalanced.dependencycontainer

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.metamask.androidsdk.DappMetadata
import io.metamask.androidsdk.Ethereum
import io.metamask.androidsdk.EthereumFlow
import io.metamask.androidsdk.SDKOptions
import ru.rainman.metamasksdktest.INFURA_API_KEY
import ru.rainman.metamasksdktest.TEST_CHAIN_ID
import ru.rainman.metamasksdktest.TEST_CHEIN_ID_HEX
import ru.rainman.metamasksdktest.TEST_NET_NAME
import ru.rainman.metamasksdktest.TEST_NET_PROVIDER

@Module
@InstallIn(SingletonComponent::class)
object EthereumConfigModule {

    @Provides
    fun provideDappMetadata(): DappMetadata {
        return DappMetadata(TEST_NET_NAME, TEST_NET_PROVIDER)
    }

    @Provides
    fun provideEthereum(@ApplicationContext context: Context, dappMetadata: DappMetadata): Ethereum {
        // CHAIN_ID_HEX = "0x539" // 1337
        return Ethereum(context, dappMetadata, SDKOptions(INFURA_API_KEY, mapOf(TEST_CHEIN_ID_HEX to TEST_NET_PROVIDER)))
    }

    @Provides
    fun provideEthereumFlow(ethereum: Ethereum): EthereumFlow {
        return EthereumFlow(ethereum)
    }
}