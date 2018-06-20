package net.panelpi.duetwifi

import com.pi4j.io.serial.*
import io.reactivex.Observable
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import mu.KLogging
import net.panelpi.*
import java.io.StringReader
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import javax.json.Json
import javax.json.JsonObject

fun main(args: Array<String>) {

    val fullValue = "{\"status\":\"P\",\"coords\":{\"axesHomed\":[1,1,1],\"xyz\":[143.800,143.800,7.180],\"machine\":[159.800,150.850,7.180],\"extr\":[391.6]},\"currentTool\":0,\"params\":{\"atxPower\":1,\"fanPercent\":[100,100,100,0,0,0,0,0,0],\"speedFactor\":100.0,\"extrFactors\":[100.0],\"babystep\":0.000},\"sensors\":{\"probeValue\":1000,\"fanRPM\":0},\"temps\":{\"bed\":{\"current\":70.0,\"active\":70.0,\"state\":2,\"heater\":1},\"current\":[190.0,70.0,2000.0,2000.0,2000.0,2000.0,2000.0,2000.0],\"state\":[2,2,0,0,0,0,0,0],\"heads\":{\"current\":[],\"active\":[],\"standby\":[],\"state\":[]},\"tools\":{\"active\":[[190.0]],\"standby\":[[190.0]]},\"extra\":[{\"name\":\"MCU\",\"temp\":37.8}]},\"time\":1423.0,\"coldExtrudeTemp\":160.0,\"coldRetractTemp\":90.0,\"controllableFans\":1,\"tempLimit\":290.0,\"endstops\":4092,\"firmwareName\":\"RepRapFirmware for Duet 2 WiFi/Ethernet\",\"geometry\":\"coreXY\",\"axes\":3,\"axisNames\":\"XYZ\",\"volumes\":2,\"mountedVolumes\":1,\"name\":\"DuetWifi\",\"probe\":{\"threshold\":100,\"height\":-0.10,\"type\":8},\"tools\":[{\"number\":0,\"heaters\":[0],\"drives\":[0],\"axisMap\":[[0],[1]],\"fans\":1,\"filament\":\"\",\"offsets\":[0.00,0.00,0.00]}],\"mcutemp\":{\"min\":33.1,\"cur\":37.6,\"max\":37.9},\"vin\":{\"min\":0.1,\"cur\":12.0,\"max\":12.3},\"seq\":11,\"resp\":\"\",\"currentLayer\":36,\"currentLayerTime\":2.7,\"extrRaw\":[394.9],\"fractionPrinted\":28.2,\"firstLayerDuration\":256.1,\"firstLayerHeight\":0.20,\"printDuration\":528.9,\"warmUpDuration\":28.3,\"timesLeft\":{\"file\":626.4,\"filament\":421.7,\"layer\":452.0}}"
    val v2 = "{\"status\":\"P\",\"coords\":{\"axesHomed\":[1,1,1],\"xyz\":[5.000,10.000,5.000],\"machine\":[5.000,10.000,5.000],\"extr\":[0.0]},\"currentTool\":0,\"params\":{\"atxPower\":1,\"fanPercent\":[0,100,100,0,0,0,0,0,0],\"speedFactor\":100.0,\"extrFactors\":[100.0],\"babystep\":0.000},\"sensors\":{\"probeValue\":0,\"fanRPM\":0},\"temps\":{\"bed\":{\"current\":70.0,\"active\":70.0,\"state\":2,\"heater\":1},\"current\":[163.5,70.0,2000.0,2000.0,2000.0,2000.0,2000.0,2000.0],\"state\":[2,2,0,0,0,0,0,0],\"heads\":{\"current\":[],\"active\":[],\"standby\":[],\"state\":[]},\"tools\":{\"active\":[[185.0]],\"standby\":[[185.0]]},\"extra\":[{\"name\":\"MCU\",\"temp\":36.6}]},\"time\":8978.0,\"coldExtrudeTemp\":160.0,\"coldRetractTemp\":90.0,\"controllableFans\":1,\"tempLimit\":290.0,\"endstops\":4092,\"firmwareName\":\"RepRapFirmware for Duet 2 WiFi/Ethernet\",\"geometry\":\"coreXY\",\"axes\":3,\"axisNames\":\"XYZ\",\"volumes\":2,\"mountedVolumes\":1,\"name\":\"DuetWifi\",\"probe\":\"0\",\"tools\":[{\"number\":0,\"heaters\":[0],\"drives\":[0],\"axisMap\":[[0],[1]],\"fans\":1,\"filament\":\"\",\"offsets\":[0.00,0.00,0.00]}],\"mcutemp\":{\"min\":21.8,\"cur\":29.9,\"max\":30.1},\"vin\":{\"min\":0.0,\"cur\":12.1,\"max\":12.2},\"seq\":6,\"resp\":\"\",\"currentLayer\":0,\"currentLayerTime\":0.0,\"extrRaw\":[0.0],\"fractionPrinted\":0.2,\"firstLayerDuration\":0.0,\"firstLayerHeight\":0.20,\"printDuration\":156.6,\"warmUpDuration\":5.0,\"timesLeft\":{\"file\":80600.1,\"filament\":0.0,\"layer\":0.0},\"heaters\":[70.0],\"active\":[70.0],\"standby\":[0.0],\"hstat\":[2],\"pos\":[5.000,10.000,5.000],\"machine\":[5.000,10.000,5.000],\"sfactor\":100.00,\"efactor\":[100.00],\"babystep\":0.000,\"tool\":0,\"fanPercent\":[0.0,100.0,100.0,0.0,0.0,0.0,0.0,0.0,0.0],\"fanRPM\":0,\"homed\":[1,1,1],\"fraction_printed\":0.0019,\"msgBox.mode\":-1}"
    val j = Json.createReader(StringReader(v2)).readObject()


    val v = j.parseAs<DuetData>()

    println(v)
}

class DuetWifi private constructor() {
    companion object : KLogging() {
        private const val fullDummyValue = "{\"status\":\"I\",\"coords\":{\"axesHomed\":[0,0,0],\"extr\":[0.0],\"xyz\":[0.000,0.000,0.000]},\"currentTool\":0,\"params\":{\"atxPower\":0,\"fanPercent\":[0.00,100.00,100.00,0.00,0.00,0.00,0.00,0.00,0.00],\"speedFactor\":100.00,\"extrFactors\":[100.00],\"babystep\":0.000},\"sensors\":{\"probeValue\":0,\"fanRPM\":0},\"temps\":{\"bed\":{\"current\":23.5,\"active\":0.0,\"state\":0,\"heater\":1},\"current\":[23.1,23.5,2000.0,2000.0,2000.0,2000.0,2000.0,2000.0],\"state\":[2,0,0,0,0,0,0,0],\"heads\":{\"current\":[],\"active\":[],\"standby\":[],\"state\":[]},\"tools\":{\"active\":[[0.0]],\"standby\":[[0.0]]},\"extra\":[{\"name\":\"MCU\",\"temp\":23.5}]},\"time\":53.0,\"coldExtrudeTemp\":160,\"coldRetractTemp\":90,\"tempLimit\":290,\"endstops\":3096,\"firmwareName\":\"RepRapFirmware for Duet WiFi\",\"geometry\":\"coreXY\",\"axes\":3,\"axisNames\":\"XYZ\",\"volumes\":2,\"mountedVolumes\":1,\"name\":\"CR-10\",\"probe\":{\"threshold\":100,\"height\":-0.10,\"type\":8},\"tools\":[{\"number\":0,\"name\":\"\",\"heaters\":[0],\"drives\":[0],\"axisMap\":[[0],[1]],\"fans\":1,\"filament\":\"\",\"offsets\":[0.00,0.00,0.00]}],\"mcutemp\":{\"min\":19.1,\"cur\":23.5,\"max\":23.7},\"vin\":{\"min\":0.0,\"cur\":0.8,\"max\":0.9},\"seq\":0,\"resp\":\"\"}"
        private const val dummyValue = "{\"status\":\"I\",\"coords\":{\"axesHomed\":[0,1,0],\"extr\":[7971.1],\"xyz\":[141.119,290.000,-122.167]},\"currentTool\":0,\"params\":{\"atxPower\":1,\"fanPercent\":[0.00,100.00,100.00,0.00,0.00,0.00,0.00,0.00,0.00],\"speedFactor\":100.00,\"extrFactors\":[100.00],\"babystep\":0.150},\"sensors\":{\"probeValue\":0,\"fanRPM\":0},\"temps\":{\"bed\":{\"current\":35.9,\"active\":0.0,\"state\":2,\"heater\":1},\"current\":[24.4,35.9,2000.0,2000.0,2000.0,2000.0,2000.0,2000.0],\"state\":[2,2,0,0,0,0,0,0],\"heads\":{\"current\":[],\"active\":[],\"standby\":[],\"state\":[]},\"tools\":{\"active\":[[0.0]],\"standby\":[[0.0]]},\"extra\":[{\"name\":\"MCU\",\"temp\":33.1}]},\"time\":24321.0,\"currentLayer\":0,\"currentLayerTime\":0.0,\"extrRaw\":[7971.1],\"fractionPrinted\":0.0,\"firstLayerDuration\":0.0,\"firstLayerHeight\":0.18,\"printDuration\":0.0,\"warmUpDuration\":0.0,\"timesLeft\":{\"file\":0.0,\"filament\":0.0,\"layer\":0.0},\"seq\":25,\"resp\":\"\"}"
        val instance = DuetWifi()
    }

    private val serial = SerialFactory.createInstance()

    private var _devMode = true
    val devMode get() = _devMode
    private val lock = ReentrantLock()

    private val jsonDuetData: ObservableValue<JsonObject>
    val duetData: ObservableValue<DuetData>

    init {
        val duetMessage = createSerialConfig()?.let {
            serial.open(it)
            _devMode = false
            Observable.create<String> {
                var buffer = ""
                serial.addListener(SerialDataEventListener { event ->
                    buffer += event.asciiString
                    if (buffer.endsWith("\n")) {
                        it.onNext(buffer)
                        buffer = ""
                    }
                })
            }
        } ?: Observable.interval(1, TimeUnit.SECONDS).timeInterval().map { dummyValue }.startWith(fullDummyValue)

        val rawDuetData = duetMessage.filter { it.startsWith("{") }.map {
            Json.createReader(StringReader(it)).readObject()
        }

        jsonDuetData = rawDuetData.fold(SimpleObjectProperty(JsonObject.EMPTY_JSON_OBJECT)) { t1: SimpleObjectProperty<JsonObject>, t2: JsonObject ->
            val builder = Json.createObjectBuilder()
            t1.value.forEach { t, u -> builder.add(t, u) }
            t2.forEach { t, u -> builder.add(t, u) }
            t1.set(builder.build())
        }

        duetData = jsonDuetData.map { it.parseAs<DuetData>() }

        // TODO: for debug only, remove this.
        duetMessage.filter { !it.startsWith("{") }.subscribe {
            logger.info { it }
        }

        if (!devMode) {
            // Refresh data and retry on timeout.
            rawDuetData.delay(500, TimeUnit.MILLISECONDS).timeout(10, TimeUnit.SECONDS).retry { _ ->
                sendCmd("M408 S3")
                true
            }.subscribe { sendCmd("M408 S4") }
            sendCmd("M408 S3")
        }
    }

    fun logDuetData() {
        logger.info { jsonDuetData.value.toString() }
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
