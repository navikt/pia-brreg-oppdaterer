package brreg

import kotlinx.serialization.Serializable

@Serializable
data class PageDTO(val size: Int, val totalElements: Int, val totalPages: Int, val number: Int)
