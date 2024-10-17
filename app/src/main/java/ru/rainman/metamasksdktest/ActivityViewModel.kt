package ru.rainman.metamasksdktest

import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.metamask.androidsdk.Ethereum
import io.metamask.androidsdk.EthereumFlowWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.rainman.metamasksdktest.network.dto.contract.NewChannel
import ru.rainman.metamasksdktest.repo.Repository
import ru.rainman.metamasksdktest.repo.model.ChannelMeta
import javax.inject.Inject


@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val repository: Repository,
    private val ethereum: EthereumFlowWrapper,
) : ViewModel() {

    val connection =
        ethereum.ethereumState.stateIn(CoroutineScope(Dispatchers.IO), SharingStarted.Eagerly, null)


    private val _channel = MutableStateFlow<String?>(null)
    val channel: StateFlow<String?> get() = _channel

    fun connect() {
        viewModelScope.launch { ethereum.connect() }
    }

    fun sendTransaction() {
        viewModelScope.launch {
            val result = try {
                repository.createChannel(NewChannel("Title", "Descritpion", false, 0))
            } catch (e: Exception) {
                Log.d("TAG", "ViewModel : sendTransaction: ${e.message}")
                null
            }
            _channel.emit(result)
        }
    }
}