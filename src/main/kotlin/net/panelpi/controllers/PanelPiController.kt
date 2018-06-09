package net.panelpi.controllers

import net.panelpi.DuetWifi
import javafx.beans.property.SimpleObjectProperty
import tornadofx.*
import kotlin.concurrent.timer

class PanelPiController : Controller() {
    val duet = DuetWifi()

    val duetData = SimpleObjectProperty(duet.getData())

    init {
        println("controller created")

        timer(period = 1000) {
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
