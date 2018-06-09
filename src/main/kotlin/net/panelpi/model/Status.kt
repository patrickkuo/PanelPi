package net.panelpi.model

import tornadofx.*
import javax.json.JsonArray
import javax.json.JsonNumber
import javax.json.JsonObject

/*class StatusReport : JsonModel {
    private var status by property<Status>()
    fun statusProperty() = getProperty(StatusReport::status)

    private var heaters by property<List<Heater>>()
    fun heatersProperty() = getProperty(StatusReport::heaters)

    private var myName by property<String>()
    fun myNameProperty() = getProperty(StatusReport::myName)

    private var homedAxes by property<List<Boolean>>()
    fun homedAxesProperty() = getProperty(StatusReport::homedAxes)


    init {
        homedAxes = listOf(false, false, false)
    }

    override fun updateModel(json: JsonObject) {
        with(json) {
            safeGetString("myName")?.let { myName = it }
            safeGetString("status")?.let { status = Status.valueOf(it) }

            val temp = safeGetJsonArray("heaters")?.getValuesAs<Double, JsonNumber> { it.doubleValue() }
            val active = safeGetJsonArray("active")?.getValuesAs<Double, JsonNumber> { it.doubleValue() }
            val standBy = safeGetJsonArray("standby")?.getValuesAs<Double, JsonNumber> { it.doubleValue() }
            val heaterStatus = safeGetJsonArray("hstat")?.getValuesAs<Int, JsonNumber> { it.intValue() }

           // heaters = temp.zip(active).zip(standBy.zip(heaterStatus)).map { Heater(it.first.first, it.first.second, it.second.first, Heater.Status.values()[it.second.second]) }

            homedAxes = safeGetJsonArray("axesHomed")?.getValuesAs<Boolean, JsonNumber> { it.intValue() == 1 }
        }
    }
}

private fun JsonObject.safeGetString(key: String): String? {
    return if (containsKey(key)) {
        getString(key)
    } else {
        System.err.println("[WARNING] Cannot get data '$key'")
        null
    }
}

private fun JsonObject.safeGetJsonArray(key: String): JsonArray? {
    return if (containsKey(key)) {
        getJsonArray(key)
    } else {
        System.err.println("[WARNING] Cannot get data '$key'")
        null
    }
}

data class Heater(val temp: Double, val active: Double, val standBy: Double, val status: Status) {
    enum class Status {
        OFF, STANDBY, ACTIVE, FAULT
    }
}



data class Axis(val name: String, val homed: Boolean)*/

