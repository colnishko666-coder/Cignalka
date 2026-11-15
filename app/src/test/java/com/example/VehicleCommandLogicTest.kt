package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CarCommand
import com.example.data.model.ConnectionStatus
import com.example.data.model.EngineState
import com.example.data.model.SecurityState
import com.example.data.model.VehicleStatus
import com.example.data.repository.VehicleRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VehicleCommandLogicTest {

    @Test
    fun `commands update vehicle status accurately`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = VehicleRepository(context)
        val initial = VehicleStatus()

        val unlocked = repo.applyCommandToStatus(
            initial,
            CarCommand.UNLOCK,
            warmupMinutes = 15,
            connectionStatus = ConnectionStatus.SIMULATION
        )
        assertEquals(SecurityState.DISARMED, unlocked.securityState)
        assertTrue(unlocked.isSimulatedTelemetry)

        val started = repo.applyCommandToStatus(
            unlocked,
            CarCommand.ENGINE_START,
            warmupMinutes = 20,
            connectionStatus = ConnectionStatus.CONNECTED_BLE
        )
        assertEquals(EngineState.CRANKING, started.engineState)
        assertEquals(SecurityState.ARMED, started.securityState)
        assertEquals(20 * 60, started.engineRemainingSeconds)
        assertFalse(started.isSimulatedTelemetry)

        val trunkToggled = repo.applyCommandToStatus(
            started,
            CarCommand.TRUNK_OPEN,
            warmupMinutes = 20,
            connectionStatus = ConnectionStatus.CONNECTED_BLE
        )
        assertTrue(trunkToggled.isTrunkOpen)

        val stopped = repo.applyCommandToStatus(
            trunkToggled,
            CarCommand.ENGINE_STOP,
            warmupMinutes = 20,
            connectionStatus = ConnectionStatus.CONNECTED_BLE
        )
        assertEquals(EngineState.STOPPED, stopped.engineState)
        assertEquals(0, stopped.engineRemainingSeconds)
    }
}
