package brreg

import java.time.ZonedDateTime

interface BrregApi {
    suspend fun hentOppdaterteUnderenheter(tidspunkt: ZonedDateTime, side: Int): BrregOppdateringDTO
    suspend fun hentUnderenheter(orgnummere: List<String>): String
}