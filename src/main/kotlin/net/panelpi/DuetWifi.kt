package net.panelpi

import com.pi4j.io.serial.*
import mu.KLogging
import java.io.StringReader
import java.util.concurrent.locks.ReentrantLock
import javax.json.Json
import javax.json.JsonObject
import javax.json.JsonValue

class DuetWifi private constructor() {
    companion object : KLogging() {
        private const val fullDummyValue = "{\"status\":\"I\",\"coords\":{\"axesHomed\":[0,0,0],\"extr\":[0.0],\"xyz\":[0.000,0.000,0.000]},\"currentTool\":0,\"params\":{\"atxPower\":0,\"fanPercent\":[0.00,100.00,100.00,0.00,0.00,0.00,0.00,0.00,0.00],\"speedFactor\":100.00,\"extrFactors\":[100.00],\"babystep\":0.000},\"sensors\":{\"probeValue\":0,\"fanRPM\":0},\"temps\":{\"bed\":{\"current\":23.5,\"active\":0.0,\"state\":0,\"heater\":1},\"current\":[23.1,23.5,2000.0,2000.0,2000.0,2000.0,2000.0,2000.0],\"state\":[2,0,0,0,0,0,0,0],\"heads\":{\"current\":[],\"active\":[],\"standby\":[],\"state\":[]},\"tools\":{\"active\":[[0.0]],\"standby\":[[0.0]]},\"extra\":[{\"name\":\"MCU\",\"temp\":23.5}]},\"time\":53.0,\"coldExtrudeTemp\":160,\"coldRetractTemp\":90,\"tempLimit\":290,\"endstops\":3096,\"firmwareName\":\"RepRapFirmware for Duet WiFi\",\"geometry\":\"coreXY\",\"axes\":3,\"axisNames\":\"XYZ\",\"volumes\":2,\"mountedVolumes\":1,\"name\":\"CR-10\",\"probe\":{\"threshold\":100,\"height\":-0.10,\"type\":8},\"tools\":[{\"number\":0,\"name\":\"\",\"heaters\":[0],\"drives\":[0],\"axisMap\":[[0],[1]],\"fans\":1,\"filament\":\"\",\"offsets\":[0.00,0.00,0.00]}],\"mcutemp\":{\"min\":19.1,\"cur\":23.5,\"max\":23.7},\"vin\":{\"min\":0.0,\"cur\":0.8,\"max\":0.9},\"seq\":0,\"resp\":\"\"}"
        private const val dummyValue = "{\"status\":\"I\",\"coords\":{\"axesHomed\":[0,1,0],\"extr\":[7971.1],\"xyz\":[141.119,290.000,-122.167]},\"currentTool\":0,\"params\":{\"atxPower\":1,\"fanPercent\":[0.00,100.00,100.00,0.00,0.00,0.00,0.00,0.00,0.00],\"speedFactor\":100.00,\"extrFactors\":[100.00],\"babystep\":0.150},\"sensors\":{\"probeValue\":0,\"fanRPM\":0},\"temps\":{\"bed\":{\"current\":35.9,\"active\":0.0,\"state\":2,\"heater\":1},\"current\":[24.4,35.9,2000.0,2000.0,2000.0,2000.0,2000.0,2000.0],\"state\":[2,2,0,0,0,0,0,0],\"heads\":{\"current\":[],\"active\":[],\"standby\":[],\"state\":[]},\"tools\":{\"active\":[[0.0]],\"standby\":[[0.0]]},\"extra\":[{\"name\":\"MCU\",\"temp\":33.1}]},\"time\":24321.0,\"currentLayer\":0,\"currentLayerTime\":0.0,\"extrRaw\":[7971.1],\"fractionPrinted\":0.0,\"firstLayerDuration\":0.0,\"firstLayerHeight\":0.18,\"printDuration\":0.0,\"warmUpDuration\":0.0,\"timesLeft\":{\"file\":0.0,\"filament\":0.0,\"layer\":0.0},\"seq\":25,\"resp\":\"\"}"
        val instance = DuetWifi()
    }

    private var buffer = ""
    private val serial = SerialFactory.createInstance()

    private var _devMode = true
    val devMode get() = _devMode

    private var duetData: JsonObject = JsonValue.EMPTY_JSON_OBJECT

    private val lock = ReentrantLock()

    init {
        createSerialConfig()?.let {
            serial.open(it)
            serial.addListener(SerialDataEventListener { event ->
                buffer += event.asciiString
            })
            _devMode = false
        }
    }

    // TODO: rx Observable?
    fun getData(): DuetData? {
        val full = duetData.isEmpty()
        return synchronized(duetData) {
            try {
                val builder = Json.createObjectBuilder()
                duetData.forEach { t, u -> builder.add(t, u) }
                if (devMode) {
                    Json.createReader(StringReader(if (full) fullDummyValue else dummyValue)).readObject().forEach { t, u -> builder.add(t, u) }
                } else {
                    sendCmdWithResponse("M408 S${if (full) "3" else "4"}")?.forEach { t, u -> builder.add(t, u) }
                }
                duetData = builder.build()
                duetData.parseAs()
            } catch (e: Throwable) {
                e.printStackTrace()
                null
            }
        }
    }

    fun logDuetData() {
        logger.info { duetData.toString() }
    }

    private fun sendCmdWithResponse(cmd: String): JsonObject? {
        return synchronized(buffer) {
            if (buffer.isNotEmpty()) {
                logger.info { buffer }
            }
            buffer = ""
            serial.writeln(cmd.appendCheckSum())
            var waited = 0
            while (!buffer.endsWith("\n") && waited < 5000) {
                Thread.sleep(100)
                waited += 100
            }
            try {
                Json.createReader(StringReader(buffer)).readObject()
            } catch (e: Throwable) {
                logger.error(e) { "Some error while getting data. Buffer = $buffer" }
                null
            } finally {
                //logger.trace { buffer }
                buffer = ""
            }
        }
    }

    fun sendCmd(vararg cmd: String) {
        if (devMode) {
            cmd.forEach {
                logger.debug { it }
            }
            return
        }
        lock.tryWithLock {
            cmd.forEach {
                serial.writeln(it.appendCheckSum())
            }
        }
    }

    private fun createSerialConfig(): SerialConfig? {
        return try {
            SerialConfig().device(SerialPort.getDefaultPort())
                    .baud(Baud._57600)
                    .parity(Parity.NONE)
                    .flowControl(FlowControl.NONE)
        } catch (e: Throwable) {
            logger.info { "RaspberryPi library not found, running in dev mode." }
            null
        }
    }
}

data class DuetData(val status: Status = Status.X,
                    private val coords: Coordinates = Coordinates(),
                    val params: Parameters = Parameters(),
                    val sensors: Sensors = Sensors(),
                    val temps: Temperatures = Temperatures(),
                    val name: String = "PanelPi",
                    val timesLeft: TimesLeft? = null,
                    val firstLayerDuration: Double? = null,
                    val fractionPrinted: Double? = null,
                    val warmUpDuration: Double? = null,
                    val currentLayerTime: Double? = null,
                    private val axisNames: String = "",
                    private val coldExtrudeTemp: Int = Int.MAX_VALUE,
                    private val coldRetractTemp: Int = Int.MAX_VALUE,
                    private val currentTool: Int = 0
) {
    val axes = (axisNames.toList().map(Char::toString) zip coords.axes).toMap()
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