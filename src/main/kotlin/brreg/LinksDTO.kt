package brreg

import kotlinx.serialization.Serializable

@Serializable
data class LinksDTO(val first: Href, val next: Href, val self: Href, val last: Href)

@Serializable
data class Href(val href: String)