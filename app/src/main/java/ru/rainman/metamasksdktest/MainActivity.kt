package ru.rainman.metamasksdktest

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import ru.rainman.metamasksdktest.databinding.ActivityMainBinding

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: ActivityViewModel by viewModels()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.requestConnection.setOnClickListener {
            viewModel.connect()
        }

        viewModel.connection.observe(this) {
            this?.let {
                binding.connectionMeta.text =
"""
chainID = ${it.chainId}
adress = ${it.selectedAddress}
""".trimIndent()
            }
        }

        binding.sendTransaction.setOnClickListener {
            viewModel.sendTransaction()
        }

        viewModel.channel.observe(this) {
            this?.let {
                binding.result.text = it
            }
        }
    }

    private fun <T> Flow<T>.observe(viewLifecycleOwner: LifecycleOwner, collector: T.() -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                this@observe
                    .distinctUntilChanged { old, new -> new == old }
                    .collect { collector(it) }
            }
        }
    }
}
