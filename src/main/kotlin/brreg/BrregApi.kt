package brreg

import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

interface BrregApi {
    suspend fun hentOppdaterteUnderenheter(tidspunkt: ZonedDateTime, side: Int): BrregOppdateringDTO
    suspend fun hentUnderenheter(orgnummere: List<String>): List<BrregVirksomhetDto>
}

@Serializable
data class BrregVirksomhetDto(
    val organisasjonsnummer: String,
    val navn: String,
    val beliggenhetsadresse: Beliggenhetsadresse? = null,
    val naeringskode1: NæringsundergruppeBrreg? = null,
    val naeringskode2: NæringsundergruppeBrreg? = null,
    val naeringskode3: NæringsundergruppeBrreg? = null,
    )

@Serializable
data class NæringsundergruppeBrreg(
    val beskrivelse: String,
    val kode: String
)

@Serializable
data class Beliggenhetsadresse(
    val land: String?,
    val landkode: String?,
    val postnummer: String?,
    val poststed: String?,
    val adresse: List<String>?,
    val kommune: String?,
    val kommunenummer: String?
)
