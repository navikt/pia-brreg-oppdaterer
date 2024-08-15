import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.testcontainers.Testcontainers

class WiremockContainerHelper {
    private val brregMock: WireMockServer

    init {
        brregMock = lagMockServer("brreg")
    }

    private fun lagMockServer(service: String) = WireMockServer(WireMockConfiguration.options().dynamicPort()).also {
        if (!it.isRunning) {
            it.start()
        }

        println("Starter Wiremock for $service på port ${it.port()}")
        Testcontainers.exposeHostPorts(it.port())
    }

    fun envVars() = mapOf(
        "BRREG_OPPDATERING_UNDERENHET_URL" to "http://host.testcontainers.internal:${brregMock.port()}/enhetsregisteret/api/oppdateringer/underenheter",
        "BRREG_UNDERENHET_URL" to "http://host.testcontainers.internal:${brregMock.port()}/enhetsregisteret/api/underenheter",
        "FULL_EKSPORT_URL" to "http://host.testcontainers.internal:${brregMock.port()}/enhetsregisteret/api/underenheter/lastned",
    )
}