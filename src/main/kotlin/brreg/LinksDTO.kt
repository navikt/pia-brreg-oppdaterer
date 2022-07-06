package brreg

import kotlinx.serialization.Serializable

@Serializable
data class LinksDTO(
    val first: Href? = null,
    val next: Href? = null,
    val self: Href? = null,
    val last: Href? = null,
    val prev: Href? = null,
    val underenhet: Href? = null
)

@Serializable
data class Href(val href: String)