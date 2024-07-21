package `in`.co.gorest.grblcontroller.model

import kotlinx.serialization.Serializable

@Serializable
data class Tool(
    val number: Int,
    val description: String,
    val diameter: Double,
    val spindleSpeed: Int,
    val feedrateCutting: Double,
    val feedratePlunge: Double
)
