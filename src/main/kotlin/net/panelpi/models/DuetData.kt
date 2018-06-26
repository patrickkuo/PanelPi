package net.panelpi.models

import java.time.LocalDateTime

data class DuetData(val status: Status = Status.X,
                    private val coords: Coordinates = Coordinates(),
                    val params: Parameters = Parameters(),
                    val sensors: Sensors = Sensors(),
                    val temps: Temperatures = Temperatures(),
                    val name: String = "PanelPi",
                    val timesLeft: TimesLeft? = null,
                    val firstLayerDuration: Double? = null,
                    val fractionPrinted: Double = 0.0,
                    val warmUpDuration: Double? = null,
                    val currentLayerTime: Double? = null,
                    val printDuration: Double? = null,
                    private val axisNames: String = "",
                    private val coldExtrudeTemp: Int = Int.MAX_VALUE,
                    private val coldRetractTemp: Int = Int.MAX_VALUE,
                    private val currentTool: Int = 0
) {
    val axes = (axisNames.toUpperCase().toList().map(Char::toString) zip coords.axes).toMap()
    val isExtrudeEnable = temps.current.getOrNull(currentTool)?.let { it > coldExtrudeTemp } ?: false
    val isRetractEnable = temps.current.getOrNull(currentTool)?.let { it > coldRetractTemp } ?: false
}

data class Coordinates(private val axesHomed: List<Boolean> = emptyList(), private val xyz: List<Double> = emptyList()) {
    val axes = (axesHomed zip xyz).map { Axis(it.first, it.second) }
}

data class Axis(val homed: Boolean, val coord: Double)

data class Parameters(val atxPower: Boolean = false,
                      val fanPercent: List<Double> = emptyList(),
                      val speedFactor: Double = 0.0,
                      val extrFactor: List<Double> = emptyList(),
                      val babystep: Double = 0.0)

data class Sensors(val probeValue: Int = 0, val fanRPM: Int = 0)

data class Temperatures(val bed: Bed = Bed(),
                        val current: List<Double> = emptyList(),
                        val state: List<Int> = emptyList(),
                        val tools: Tools = Tools())

data class Bed(val current: Double = 2000.0, val active: Int = -1, val state: Int = 0, val heater: Int = 0)

// From json response, not sure why this is structured this way.
data class Tools(private val active: List<List<Int>> = emptyList(), private val standby: List<List<Int>> = emptyList()) {
    fun activeTemperature(toolNumber: Int): Int = active.firstOrNull()?.getOrNull(toolNumber) ?: -1
    fun standbyTemperature(toolNumber: Int): Int = standby.firstOrNull()?.getOrNull(toolNumber) ?: -1
}

data class TimesLeft(val file: Double, val filament: Double, val layer: Double)

sealed class SDItem {
    abstract val fileName: String
}

data class JsonSDFolder(val files: List<String>)
data class SDFolder(override val fileName: String, val files: List<SDItem>) : SDItem()
data class SDFile(override val fileName: String,
                  val size: Long,
                  val lastModified: LocalDateTime = LocalDateTime.now(),
                  val height: Double,
                  val firstLayerHeight: Double,
                  val layerHeight: Double,
                  val printTime: Int = 0,
                  val filament: List<Double>,
                  val generatedBy: String) : SDItem() {
    val layerHeights: Pair<Double, Double> = Pair(firstLayerHeight, layerHeight)
}


enum class Status(private val value: String, val color: String) {
    I("Idle", "SILVER"),
    P("Printing", "GREEN"),
    S("Stopped", "RED"),
    C("Running config file", "BLUE"),
    A("Paused", "YELLOW"),
    D("Pausing", "YELLOW"),
    R("Resuming", "GREEN"),
    B("Busy", "ORANGE"),
    F("Performing firmware update", "BLUE"),
    X("Disconnected", "Red"),
    O("Off", "Red");

    override fun toString(): String {
        return value
    }
}