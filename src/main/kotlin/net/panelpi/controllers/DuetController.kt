package net.panelpi.controllers

import javafx.beans.property.SimpleObjectProperty
import net.panelpi.DuetData
import net.panelpi.DuetWifi
import tornadofx.*
import kotlin.concurrent.timer

class DuetController : Controller() {
    val duetData = SimpleObjectProperty<DuetData>(DuetData())

    init {
        timer(period = 1000, initialDelay = 1000) {
            val data = DuetWifi.instance.getData()
            runLater { duetData.set(data) }
        }
    }
}
