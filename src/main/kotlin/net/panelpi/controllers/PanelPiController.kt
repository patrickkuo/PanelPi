package net.panelpi.controllers

import javafx.beans.property.SimpleObjectProperty
import net.panelpi.DuetData
import net.panelpi.DuetWifi
import tornadofx.*
import kotlin.concurrent.timer

class PanelPiController : Controller() {
    val duet = DuetWifi()

    val duetData = SimpleObjectProperty<DuetData>()

    init {
        timer(period = 1000, initialDelay = 1000) {
            runLater { duetData.set(duet.getData()) }
        }
    }

/*    private val report by lazy {
        StatusReport().apply {
            timer(period = 1000) {
                duet.getStatusReport()?.let {
                    runLater { updateModel(it) }
                }
            }
        }
    }

    fun statusReport(): StatusReport {
        return report
    }*/
}
