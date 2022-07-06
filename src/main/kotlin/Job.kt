import brreg.BrregClient
import brreg.OppdateringService

suspend fun main() {
    OppdateringService(BrregClient()).oppdater()
}
