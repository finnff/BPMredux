package com.bpmredux

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bpmredux.ui.BpmViewModel
import com.bpmredux.ui.screen.MainScreen
import com.bpmredux.ui.theme.BpmReduxTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            getViewModel()?.onPermissionResult(true)
        }
    }

    private var viewModelRef: BpmViewModel? = null

    private fun getViewModel() = viewModelRef

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BpmReduxTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    val app = application as BpmApp
                    val vm: BpmViewModel = viewModel(
                        factory = BpmViewModelFactory(
                            app.audioCapture,
                            app.onsetDetector,
                            app.tempoEstimator,
                            app.tapProcessor
                        )
                    )
                    viewModelRef = vm

                    val uiState by vm.uiState.collectAsStateWithLifecycle()

                    if (!uiState.hasPermission) {
                        checkAndRequestPermission(vm)
                    }

                    MainScreen(
                        uiState = uiState,
                        onTap = vm::onTap,
                        onBpmRangeChange = vm::onBpmRangeChange,
                        onBandToggle = vm::onBandToggle,
                        onToggleDetection = vm::toggleDetection,
                        onToggleScreenDim = vm::toggleScreenDim,
                        onAmplitudeThresholdChange = vm::onAmplitudeThresholdChange,
                        onStabilityChange = vm::onStabilityChange
                    )
                }
            }
        }
    }

    private fun checkAndRequestPermission(vm: BpmViewModel) {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            vm.onPermissionResult(true)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

class BpmViewModelFactory(
    private val audioCapture: com.bpmredux.audio.AudioCapture,
    private val onsetDetector: com.bpmredux.audio.OnsetDetector,
    private val tempoEstimator: com.bpmredux.audio.TempoEstimator,
    private val tapProcessor: com.bpmredux.audio.TapProcessor
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return BpmViewModel(audioCapture, onsetDetector, tempoEstimator, tapProcessor) as T
    }
}
