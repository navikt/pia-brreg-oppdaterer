package brreg

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class OppdateringDTO(
    val oppdateringsid: Long,
    val dato: Instant,
    val organisasjonsnummer: String,
    val endringstype: Endringstype,
    val _links: LinksDTO
)

enum class Endringstype {
    Ukjent, // Ukjent type endring. Ofte fordi endringen har skjedd før endringstype ble innført.
    Endring, // Enheten har blitt endret i Enhetsregisteret
    Ny, // Enheten har blitt lagt til i Enhetsregisteret
    Sletting, // Enheten har blitt slettet fra Enhetsregisteret
    Fjernet // Enheten har blitt fjernet fra Åpne Data. Eventuelle kopier skal også fjerne enheten.
}
