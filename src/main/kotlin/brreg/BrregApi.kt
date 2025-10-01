package brreg

import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

interface BrregApi {
    suspend fun hentOppdaterteUnderenheter(
        tidspunkt: ZonedDateTime,
        oppdateringsId: Long?,
        side: Int,
    ): BrregOppdateringDTO

    suspend fun hentUnderenheter(orgnummere: List<String>): List<BrregVirksomhetDto>
}

@Serializable
data class BrregVirksomhetDto(
	val organisasjonsnummer: String,
	val navn: String,
	val oppstartsdato: String? = null,
	val beliggenhetsadresse: Adresse? = null,
	val postadresse: Adresse? = null,
	val naeringskode1: NæringsundergruppeBrreg? = null,
	val naeringskode2: NæringsundergruppeBrreg? = null,
	val naeringskode3: NæringsundergruppeBrreg? = null,
)

@Serializable
data class NæringsundergruppeBrreg(
    val beskrivelse: String,
    val kode: String,
)

@Serializable
data class Adresse(
    val land: String? = null,
    val landkode: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
    val adresse: List<String>? = null,
    val kommune: String? = null,
    val kommunenummer: String? = null,
)
