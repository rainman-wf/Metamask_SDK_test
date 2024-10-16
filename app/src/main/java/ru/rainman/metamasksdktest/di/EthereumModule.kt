package tech.imbalanced.dependencycontainer

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.metamask.androidsdk.EthereumFlow
import io.metamask.androidsdk.EthereumFlowWrapper

@Module
@InstallIn(SingletonComponent::class)
interface EthereumModule {

    @Binds
    fun bindEthereum(impl: EthereumFlow) : EthereumFlowWrapper
}