package net.panelpi.controllers

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import mu.KLogging
import net.panelpi.duetwifi.DuetWifi
import net.panelpi.fold
import net.panelpi.map
import net.panelpi.models.*
import net.panelpi.parseAs
import net.panelpi.plus
import tornadofx.*
import java.io.StringReader
import javax.json.Json
import javax.json.JsonObject
import kotlin.concurrent.timer

class DuetController : Controller() {
    companion object : KLogging() {
        // Hack to get duet to send back ack after long running moves (e.g grid compensation) completed.
        private const val ACK = "M118 P2 S\"ACK\""
    }

    private val duet = DuetWifi()

    private val duetDataMessage = observableList(JsonObject.EMPTY_JSON_OBJECT)
    private val jsonDuetData = duetDataMessage.fold(JsonObject.EMPTY_JSON_OBJECT, JsonObject::plus)

    val data: ObservableValue<DuetData> = jsonDuetData.map { it.parseAs<DuetData>() }
    val _sdData = SimpleObjectProperty<SDFolder>()
    val sdData: ObservableValue<SDFolder> = _sdData

    val _currentFile = SimpleObjectProperty<SDFile>()
    val currentFile: ObservableValue<SDFile> = _currentFile

    private val statusObservable = data.map { it.status }

    init {
        runAsync {
            val status = duet.sendCmd("M408 S3", resultTimeout = 5000).toJson()
            Triple(status, getSdData(), getFile())
        }.ui { (status, sd, currentFile) ->
            duetDataMessage.addAll(status)
            _sdData.set(sd)
            _currentFile.set(currentFile)
        }

        timer(initialDelay = 1000, period = 500) {
            val data = duet.sendCmd("M408 S4", resultTimeout = 5000).toJson()
            runLater {
                duetDataMessage.addAll(data)
            }
        }

        statusObservable.addListener { _, _, newValue ->
            if (newValue == Status.P) {
                runAsync { getFile() }.ui { _currentFile.set(it) }
            }
        }
    }

    fun getSdData(folder: String = "gcodes"): SDFolder {
        return SDFolder(folder, duet.sendCmd("M20 S2 P/$folder", resultTimeout = 5000).toJson().parseAs<JsonSDFolder>().files.mapNotNull {
            if (it.startsWith("*")) {
                getSdData("$folder/${it.drop(1)}")
            } else {
                getFile("/$folder/$it")
            }
        })
    }

    fun getFile(path: String? = null): SDFile? {
        val json = if (path == null) {
            duet.sendCmd("M36", resultTimeout = 5000).toJson()
        } else {
            duet.sendCmd("M36 $path", resultTimeout = 5000).toJson().plus("fileName" to path.split("/").last())
        }
        return try {
            json.parseAs()
        } catch (e: Throwable) {
            logger.warn(e) { "Error parsing SDFile." }
            null
        }
    }

    fun moveAxis(axisName: String, amount: Double, speed: Int = 6000) {
        duet.sendCmd("M120", "G91", "G1 $axisName$amount F$speed", "M121")
    }

    fun atxPower(on: Boolean = true) {
        if (data.value.params.atxPower != on) {
            duet.sendCmd(if (on) "M80" else "M81")
        }
    }

    fun logDuetData() {
        logger.info { jsonDuetData.value.toString() }
    }

    fun bedCompensation() {
        duet.sendCmd("G32", ACK, resultTimeout = 60000)
    }

    fun gridCompensation() {
        duet.sendCmd("G29")
    }

    fun homeAxis(axisName: String? = null) {
        duet.sendCmd(axisName?.let { "G28 $it" } ?: "G28")
    }

    fun setBedTemperature(temperature: Int) {
        duet.sendCmd("M140 S$temperature")
    }

    fun setToolTemperature(tool: Int, temperature: Int, standby: Boolean = false) {
        duet.sendCmd("G10 P$tool ${if (standby) "R" else "S"}$temperature")
    }

    fun emergencyStop() {
        duet.sendCmd("M112", "M999")
    }

    private fun String.toJson(): JsonObject = try {
        Json.createReader(StringReader(this)).readObject()
    } catch (e: Throwable) {
        logger.warn(e) { "Error parsing Json response : $this" }
        JsonObject.EMPTY_JSON_OBJECT
    }
}