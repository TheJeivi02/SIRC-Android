package com.sirc.feature.settings

import com.sirc.domain.model.AdditionalCost
import com.sirc.domain.model.DriverConfig
import com.sirc.domain.model.OverlayConfig
import com.sirc.domain.model.RidePlatform
import com.sirc.domain.repository.DriverConfigRepository
import com.sirc.domain.repository.OverlayConfigRepository
import com.sirc.domain.usecase.GetDriverConfigUseCase
import com.sirc.domain.usecase.GetOverlayConfigUseCase
import com.sirc.domain.usecase.SaveDriverConfigUseCase
import com.sirc.domain.usecase.SaveOverlayConfigUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private lateinit var configRepo: FakeDriverConfigRepository
    private lateinit var overlayRepo: FakeOverlayConfigRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        configRepo = FakeDriverConfigRepository()
        overlayRepo = FakeOverlayConfigRepository()
        viewModel =
            SettingsViewModel(
                getDriverConfig = GetDriverConfigUseCase(configRepo),
                saveDriverConfig = SaveDriverConfigUseCase(configRepo),
                getOverlayConfig = GetOverlayConfigUseCase(overlayRepo),
                saveOverlayConfig = SaveOverlayConfigUseCase(overlayRepo),
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun persistedConfig(): DriverConfig =
        DriverConfig.default().copy(
            profile = DriverConfig.default().profile.copy(country = "Ecuador", city = "Quito"),
        )

    @Test
    fun `carga la configuracion persistida`() =
        runTest(dispatcher) {
            configRepo.config = persistedConfig()

            advanceUntilIdle()

            assertEquals("Ecuador", viewModel.state.value.config.profile.country)
            assertEquals("Quito", viewModel.state.value.config.profile.city)
            // Un cambio real de config incrementa reloadTick para re-sembrar los campos.
            assertEquals(1, viewModel.state.value.reloadTick)
        }

    @Test
    fun `editar perfil y vehiculo actualiza el estado sin persistir`() =
        runTest(dispatcher) {
            configRepo.config = persistedConfig()
            advanceUntilIdle()

            val current = viewModel.state.value.config
            viewModel.updateProfile(current.profile.copy(country = "Perú", city = "Lima"))
            viewModel.updateVehicle(current.vehicle.copy(brand = "Toyota", model = "Corolla", year = 2021))

            val state = viewModel.state.value.config
            assertEquals("Perú", state.profile.country)
            assertEquals("Lima", state.profile.city)
            assertEquals("Toyota", state.vehicle.brand)
            assertEquals(2021, state.vehicle.year)
            // Sin guardar, el repositorio no cambió.
            assertEquals("Ecuador", configRepo.config?.profile?.country)
        }

    @Test
    fun `el costo por km derivado refleja los componentes editados`() =
        runTest(dispatcher) {
            configRepo.config = persistedConfig()
            advanceUntilIdle()

            assertEquals(2.5, viewModel.state.value.derivedCostPerKm, 0.001)

            viewModel.updateFuelPrice(48.0)
            viewModel.updateMaintenanceCost(1.5)
            viewModel.updateAdditionalCosts(listOf(AdditionalCost(label = "Peajes", costPerKm = 0.6)))

            assertEquals(6.1, viewModel.state.value.derivedCostPerKm, 0.001)
        }

    @Test
    fun `togglePlatform activa y desactiva plataformas`() =
        runTest(dispatcher) {
            configRepo.config = persistedConfig().copy(platforms = setOf(RidePlatform.UBER))
            advanceUntilIdle()

            viewModel.togglePlatform(RidePlatform.DIDI)
            assertTrue(RidePlatform.DIDI in viewModel.state.value.config.platforms)
            assertTrue(RidePlatform.UBER in viewModel.state.value.config.platforms)

            viewModel.togglePlatform(RidePlatform.UBER)
            assertFalse(RidePlatform.UBER in viewModel.state.value.config.platforms)
            assertTrue(RidePlatform.DIDI in viewModel.state.value.config.platforms)
        }

    @Test
    fun `save persiste config y overlay y normaliza el costo por km derivado`() =
        runTest(dispatcher) {
            configRepo.config = persistedConfig()
            advanceUntilIdle()

            val current = viewModel.state.value.config
            viewModel.updateProfile(current.profile.copy(city = "Guayaquil"))
            viewModel.updateFuelPrice(30.0)
            viewModel.updateOverlay(OverlayConfig(showProfitPerKm = true))
            viewModel.save()
            advanceUntilIdle()

            assertTrue(viewModel.state.value.saved)
            val saved = configRepo.savedConfig
            assertEquals("Guayaquil", saved?.profile?.city)
            // El costo/km persistido se normaliza al derivado: 30/12 + 0.5 = 3.0
            assertEquals(3.0, saved?.costs?.costPerKm ?: -1.0, 0.001)
            assertEquals(3.0, viewModel.state.value.derivedCostPerKm, 0.001)
            assertTrue(overlayRepo.savedOverlay.showProfitPerKm)
        }

    @Test
    fun `discard restaura la configuracion persistida`() =
        runTest(dispatcher) {
            configRepo.config = persistedConfig()
            advanceUntilIdle()
            val tick = viewModel.state.value.reloadTick

            viewModel.updateProfile(viewModel.state.value.config.profile.copy(city = "Cuenca"))
            viewModel.updateFuelPrice(99.0)
            assertNotEquals("Cuenca", configRepo.config?.profile?.city)

            viewModel.discard()

            assertEquals("Quito", viewModel.state.value.config.profile.city)
            assertEquals(24.0, viewModel.state.value.config.fuelPrice, 0.001)
            assertEquals(tick + 1, viewModel.state.value.reloadTick)
            assertFalse(viewModel.state.value.saved)
        }

    private class FakeDriverConfigRepository : DriverConfigRepository {
        private val flow = MutableStateFlow<DriverConfig?>(null)

        var config: DriverConfig?
            get() = flow.value
            set(value) {
                flow.value = value
            }

        var savedConfig: DriverConfig? = null

        override suspend fun getDriverConfig(): DriverConfig? = flow.value

        override fun observeDriverConfig(): Flow<DriverConfig?> = flow

        override fun isConfigured(): Flow<Boolean> = MutableStateFlow(flow.value != null)

        override suspend fun save(driverConfig: DriverConfig) {
            savedConfig = driverConfig
            flow.value = driverConfig
        }
    }

    private class FakeOverlayConfigRepository : OverlayConfigRepository {
        private val overlay = MutableStateFlow(OverlayConfig())
        var savedOverlay: OverlayConfig = OverlayConfig()

        override suspend fun getOverlayConfig(): OverlayConfig = overlay.value

        override suspend fun save(overlayConfig: OverlayConfig) {
            savedOverlay = overlayConfig
            overlay.value = overlayConfig
        }

        override fun observeOverlayConfig(): Flow<OverlayConfig> = overlay
    }
}
