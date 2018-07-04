package net.panelpi.controllers

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import mu.KLogging
import net.panelpi.duetwifi.DuetWifi
import net.panelpi.map
import net.panelpi.models.*
import net.panelpi.parseAs
import net.panelpi.plus
import net.panelpi.toJson
import tornadofx.*
import javax.json.JsonObject
import kotlin.concurrent.timer

class DuetController : Controller() {
    companion object : KLogging() {
        // Hack to get duet to send back ack after long running moves (e.g grid compensation) completed.
        private const val ACK = "M118 P2 S\"ACK\""
    }

    private val duet = DuetWifi()

    //private val duetDataMessage = observableList(JsonObject.EMPTY_JSON_OBJECT)
    private val jsonDuetData = SimpleObjectProperty(JsonObject.EMPTY_JSON_OBJECT)

    val data: ObservableValue<DuetData> = jsonDuetData.map { it.parseAs() ?: DuetData() }
    private val _sdData = SimpleObjectProperty<SDFolder>(SDFolder("gcodes", emptyList()))
    val sdData: ObservableValue<SDFolder> = _sdData

    private val _currentFile = SimpleObjectProperty<SDFile>()
    val currentFile: ObservableValue<SDFile?> = _currentFile

    private val statusObservable = data.map { it.status }

    init {
        runAsync {
            val status = duet.sendCmd("M408 S3", resultTimeout = 5000).toJson()
            Pair(status, getFile())
        }.ui { (status, currentFile) ->
            status?.let { jsonDuetData.set(jsonDuetData.value + it) }
            _currentFile.set(currentFile)
        }

        refreshSDData()

        timer(initialDelay = 1000, period = 500) {
            try {
                val data = duet.sendCmd("M408 S4", resultTimeout = 5000).toJson()
                runLater {
                    data?.let { jsonDuetData.set(jsonDuetData.value + it) }
                }
            } catch (e: Throwable) {
                logger.debug(e) { "Ignoring error on update thread" }
            }
        }

        statusObservable.onChange {
            if (it == Status.P) runAsync { getFile() }.ui { _currentFile.set(it) }
        }
    }

    private fun getSdData(folder: String = "gcodes"): SDFolder? {
        return duet.sendCmd("M20 S2 P/$folder", resultTimeout = 5000).toJson()?.parseAs<JsonSDFolder>()?.files?.sorted()?.mapNotNull {
            if (it.startsWith("*")) {
                getSdData("$folder/${it.drop(1)}")
            } else {
                getFile("/$folder/$it")
            }
        }?.let { SDFolder(folder.split("/").last(), it) }
    }

    private fun getFile(path: String? = null): SDFile? {
        return if (path == null) {
            duet.sendCmd("M36", resultTimeout = 5000).toJson()
        } else {
            duet.sendCmd("M36 $path", resultTimeout = 5000).toJson()?.plus("fileName" to path.split("/").last())
        }?.parseAs()
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
        duet.sendCmd("G29", ACK, resultTimeout = 60000)
    }

    fun homeAxis(axisName: String? = null) {
        duet.sendCmd(axisName?.let { "G28 $it" } ?: "G28", ACK, resultTimeout = 60000)
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

    fun selectFileAndPrint(fileName: String) {
        duet.sendCmd("M32 $fileName")
    }

    fun resumePrint() {
        duet.sendCmd("M24")
    }

    fun pausePrint() {
        duet.sendCmd("M25")
    }

    fun stopPrint() {
        duet.sendCmd("M0")
    }

    fun setSpeedFactorOverride(speedFactor: Int) {
        duet.sendCmd("M220 S$speedFactor")
    }

    fun babyStepping(up: Boolean) {
        if (up) {
            duet.sendCmd("M290 S0.05")
        } else {
            duet.sendCmd("M290 S-0.05")
        }
    }

    fun refreshSDData(func: () -> Unit = {}) {
        runAsync { getSdData() }.ui {
            it?.let { _sdData.set(it) }
            func()
        }
    }
}