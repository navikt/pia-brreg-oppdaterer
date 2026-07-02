package brreg

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class BrregClientTest {
    companion object {
        private val wireMock = WireMockServer(options().dynamicPort())

        @JvmStatic
        @BeforeAll
        fun startWireMock() {
            wireMock.start()
        }

        @JvmStatic
        @AfterAll
        fun stopWireMock() {
            wireMock.stop()
        }
    }

    private fun lagKlient() = BrregClient(baseUrl = "http://localhost:${wireMock.port()}")

    @Test
    fun `hentOppdaterteUnderenheter returnerer riktig resultat`() = runTest {
        wireMock.stubFor(
            get(urlPathEqualTo("/$BRREG_OPPDATERING_UNDERENHET_PATH"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("oppdatering-underenhet-response.json")
                )
        )

        val resultat = lagKlient().hentOppdaterteUnderenheter(
            tidspunkt = ZonedDateTime.now(),
            oppdateringsId = null,
            side = 0,
        )

        resultat.page.totalElements shouldBe 10
        resultat.page.totalPages shouldBe 1
        resultat._embedded?.oppdaterteUnderenheter?.size shouldBe 10
    }

    @Test
    fun `hentOppdaterteUnderenheter returnerer tomt resultat når Brreg ikke har oppdateringer`() = runTest {
        wireMock.stubFor(
            get(urlPathEqualTo("/$BRREG_OPPDATERING_UNDERENHET_PATH"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("oppdatering-underenhet-tom-response.json")
                )
        )

        val resultat = lagKlient().hentOppdaterteUnderenheter(
            tidspunkt = ZonedDateTime.now(),
            oppdateringsId = null,
            side = 0,
        )

        resultat.page.totalElements shouldBe 0
        resultat.page.totalPages shouldBe 0
        resultat._embedded.shouldBeNull()
    }
}
