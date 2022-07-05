package brreg

import kotlinx.serialization.Serializable

@Serializable
data class BrregOppdateringDTO(val _embedded: Embedded, val _links: LinksDTO, val page: PageDTO)

@Serializable
data class Embedded(val oppdaterteUnderenheter: List<OppdateringDTO>)
