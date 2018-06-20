package net.panelpi.views

import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.Parent
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import net.panelpi.duetwifi.DuetWifi
import net.panelpi.map
import tornadofx.*

class StatusView : View() {
    override val root: Parent by fxml()

    private val progressBar: ProgressBar by fxid()
    private val progressLabel: Label by fxid()

    private val timeLeftFilament: Label by fxid()
    private val timeLeftFile: Label by fxid()
    private val timeLeftLayer: Label by fxid()

    private val warmUp: Label by fxid()
    private val currentLayer: Label by fxid()
    private val lastLayer: Label by fxid()
    private val printDuration: Label by fxid()


    private val duetData = DuetWifi.instance.duetData
    private val currentLayerTime = duetData.map { it.currentLayerTime }
    private val lastLayerTime = SimpleDoubleProperty()

    init {
        progressBar.bind(duetData.map { it.fractionPrinted / 100 })
        progressLabel.bind(duetData.map { "${it.fractionPrinted}%" })

        timeLeftFilament.bind(duetData.map { it.timesLeft?.filament ?: "N/A" })
        timeLeftFile.bind(duetData.map { it.timesLeft?.file ?: "N/A" })
        timeLeftLayer.bind(duetData.map { it.timesLeft?.layer ?: "N/A" })

        warmUp.bind(duetData.map { it.warmUpDuration ?: "N/A" })
        currentLayer.bind(currentLayerTime.map { it ?: "N/A" })

        currentLayerTime.addListener { _, old, _ -> old?.let(lastLayerTime::set) }

        lastLayer.bind(lastLayerTime.map { it ?: "N/A" })
        printDuration.bind(duetData.map { it.printDuration ?: "N/A" })
    }
}