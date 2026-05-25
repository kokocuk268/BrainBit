package com.brainfocus.app.ui.connection

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.brainfocus.app.brainbit.BrainBitDevice
import com.brainfocus.app.brainbit.ConnectionState
import com.brainfocus.app.ui.game.GameViewModel
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class DisconnectReconnectTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var context: Context
    private lateinit var viewModel: ConnectionViewModel
    private lateinit var gameViewModel: GameViewModel

    @Mock
    lateinit var mockBrainBitManager: com.brainfocus.app.brainbit.BrainBitManager

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        context = ApplicationProvider.getApplicationContext()
        viewModel = ConnectionViewModel()
        gameViewModel = GameViewModel()
        val brainBitManagerField = viewModel.javaClass.getDeclaredField("brainBitManager")
        brainBitManagerField.isAccessible = true
        brainBitManagerField.set(viewModel, mockBrainBitManager)
        val gameViewModelField = viewModel.javaClass.getDeclaredField("gameViewModel")
        gameViewModelField.isAccessible = true
        gameViewModelField.set(viewModel, gameViewModel)
    }

    @Test
    fun disconnect_then_reconnect_should_allow_game_to_start() {
        viewModel.initialize(context)
        val mockDevice = Mockito.mock(BrainBitDevice::class.java)
        viewModel.connect(context, mockDevice)
        assertThat(viewModel.connectionState.value).isEqualTo(ConnectionState.Connected)
        viewModel.disconnect()
        assertThat(viewModel.connectionState.value).isEqualTo(ConnectionState.Disconnected)
        assertThat(gameViewModel.gameScore.value).isEqualTo(0)
        assertThat(gameViewModel.isGameOver.value).isEqualTo(false)
        viewModel.connect(context, mockDevice)
        assertThat(viewModel.connectionState.value).isEqualTo(ConnectionState.Connected)
        assertThat(gameViewModel.gameScore.value).isEqualTo(0)
        assertThat(gameViewModel.isGameOver.value).isEqualTo(false)
    }
}
