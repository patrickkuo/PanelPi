package net.panelpi

import com.pi4j.io.serial.*
import java.io.StringReader
import javax.json.Json
import javax.json.JsonObject
import javax.json.JsonValue

class DuetWifi {
    companion object {
        private const val fullDummyValue = "{\"status\":\"I\",\"coords\":{\"axesHomed\":[0,0,0],\"extr\":[0.0],\"xyz\":[0.000,0.000,0.000]},\"currentTool\":0,\"params\":{\"atxPower\":0,\"fanPercent\":[0.00,100.00,100.00,0.00,0.00,0.00,0.00,0.00,0.00],\"speedFactor\":100.00,\"extrFactors\":[100.00],\"babystep\":0.000},\"sensors\":{\"probeValue\":0,\"fanRPM\":0},\"temps\":{\"bed\":{\"current\":23.5,\"active\":0.0,\"state\":0,\"heater\":1},\"current\":[23.1,23.5,2000.0,2000.0,2000.0,2000.0,2000.0,2000.0],\"state\":[2,0,0,0,0,0,0,0],\"heads\":{\"current\":[],\"active\":[],\"standby\":[],\"state\":[]},\"tools\":{\"active\":[[0.0]],\"standby\":[[0.0]]},\"extra\":[{\"name\":\"MCU\",\"temp\":23.5}]},\"time\":53.0,\"coldExtrudeTemp\":160,\"coldRetractTemp\":90,\"tempLimit\":290,\"endstops\":3096,\"firmwareName\":\"RepRapFirmware for Duet WiFi\",\"geometry\":\"coreXY\",\"axes\":3,\"axisNames\":\"XYZ\",\"volumes\":2,\"mountedVolumes\":1,\"name\":\"CR-10\",\"probe\":{\"threshold\":100,\"height\":-0.10,\"type\":8},\"tools\":[{\"number\":0,\"name\":\"\",\"heaters\":[0],\"drives\":[0],\"axisMap\":[[0],[1]],\"fans\":1,\"filament\":\"\",\"offsets\":[0.00,0.00,0.00]}],\"mcutemp\":{\"min\":19.1,\"cur\":23.5,\"max\":23.7},\"vin\":{\"min\":0.0,\"cur\":0.8,\"max\":0.9},\"seq\":0,\"resp\":\"\"}"
        private const val dummyValue = "{\"status\":\"I\",\"coords\":{\"axesHomed\":[1,0,1],\"extr\":[0.0],\"xyz\":[35.000,264.800,5.332]},\"currentTool\":0,\"params\":{\"atxPower\":1,\"fanPercent\":[0.00,100.00,100.00,0.00,0.00,0.00,0.00,0.00,0.00],\"speedFactor\":100.00,\"extrFactors\":[100.00],\"babystep\":0.000},\"sensors\":{\"probeValue\":0,\"fanRPM\":0},\"temps\":{\"bed\":{\"current\":23.6,\"active\":0.0,\"state\":0,\"heater\":1},\"current\":[22.9,23.6,2000.0,2000.0,2000.0,2000.0,2000.0,2000.0],\"state\":[2,0,0,0,0,0,0,0],\"heads\":{\"current\":[],\"active\":[],\"standby\":[],\"state\":[]},\"tools\":{\"active\":[[0.0]],\"standby\":[[0.0]]},\"extra\":[{\"name\":\"MCU\",\"temp\":32.2}]},\"time\":3257.0,\"seq\":12,\"resp\":\"\"}"
    }

    private var buffer = ""
    private val serial = SerialFactory.createInstance()

    private var connected = false
    private var duetData: JsonObject = JsonValue.EMPTY_JSON_OBJECT

    init {
        createSerialConfig()?.let {
            serial.open(it)
            serial.addListener(SerialDataEventListener { event ->
                //println("buffer = $buffer, new string = ${event.asciiString}")
                buffer += event.asciiString
            })
            connected = true
        }
        getData(true)
    }

    fun getData(full: Boolean = false): DuetData? {
        return synchronized(duetData) {
            try {
                val builder = Json.createObjectBuilder()
                duetData.forEach { t, u -> builder.add(t, u) }
                if (connected) {
                    sendCmdWithResponse("M408 S${if (full) "3" else "4"}")?.forEach { t, u -> builder.add(t, u) }
                } else {
                    Json.createReader(StringReader(if (full) fullDummyValue else dummyValue)).readObject().forEach { t, u -> builder.add(t, u) }
                }
                duetData = builder.build()
                duetData.parseAs()
            } catch (e: Throwable) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun sendCmdWithResponse(cmd: String): JsonObject? {
        return synchronized(buffer) {
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
                null
            } finally {
                println(buffer)
                buffer = ""
            }
        }
    }

    fun sendCmd(cmd: String) {
        if (!connected) {
            println(cmd)
            return
        }
        return synchronized(buffer) {
            serial.writeln(cmd.appendCheckSum())
        }
    }

    private fun createSerialConfig(): SerialConfig? {
        return try {
            SerialConfig().apply {
                device(SerialPort.getDefaultPort()).baud(Baud._57600)
                        .parity(Parity.NONE)
                        .flowControl(FlowControl.NONE)
            }
        } catch (e: Throwable) {
            null
        }
    }
}

data class DuetData(val status: Status, val coords: Coordinates, val params: Parameters, val sensors: Sensors, val temps: Temperatures, val name: String)

data class Coordinates(val axesHomed: List<Boolean>, val xyz: List<Double>)

data class Parameters(val atxPower: Boolean, val fanPercent: List<Double>, val speedFactor: Double, val extrFactor: List<Double>, val babystep: Double)

data class Sensors(val probeValue: Int, val fanRPM: Int)

data class Temperatures(val bed: Bed, val current: List<Double>, val state: List<Int>)

data class Bed(val current: Double, val active: Double, val state: Int, val heater: Int)

enum class Status(val value: String, val color: String) {
    I("Idle", "GRAY"),
    P("Printing", "GREEN"),
    S("Stopped", "RED"),
    C("Running config file", "BLUE"),
    A("Paused", "YELLOW"),
    D("Pausing", "YELLOW"),
    R("Resuming", "GREEN"),
    B("Busy", "RED"),
    F("Performing firmware update", "BLUE");

    override fun toString(): String {
        return value
    }
}