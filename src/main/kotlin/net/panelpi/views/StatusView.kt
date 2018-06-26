package net.panelpi.views

import javafx.beans.property.SimpleDoubleProperty
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import net.panelpi.controllers.DuetController
import net.panelpi.map
import net.panelpi.models.Status
import tornadofx.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


class StatusView : View() {
    override val root: Parent by fxml()
    private val duetController: DuetController by inject()

    private val duetData = duetController.data
    private val currentFile = duetController.currentFile

    private val progressBar: ProgressBar by fxid()
    private val progressLabel: Label by fxid()

    private val timeLeftFilament: Label by fxid()
    private val timeLeftFile: Label by fxid()
    private val timeLeftLayer: Label by fxid()

    private val warmUp: Label by fxid()
    private val currentLayer: Label by fxid()
    private val lastLayer: Label by fxid()
    private val printDuration: Label by fxid()

    private val timeLeftFilamentTime: Label by fxid()
    private val timeLeftFileTime: Label by fxid()
    private val timeLeftLayerTime: Label by fxid()

    private val currentLayerTime = duetData.map { it.currentLayerTime }
    private val lastLayerTime = SimpleDoubleProperty()

    private val offsetAmount: Label by fxid()

    private val fileName: Label by fxid()
    private val fileSize: Label by fxid()
    private val objectHeight: Label by fxid()
    private val layerHeight: Label by fxid()
    private val filamentUsage: Label by fxid()
    private val generatedBy: Label by fxid()

    private val speedFactor: Button by fxid()
    private val fanControl: Button by fxid()
    private val extrusionFactor: Button by fxid()

    private val babySteppingDown: Button by fxid()
    private val babySteppingUp: Button by fxid()

    private val pauseResumeStart: Button by fxid()

    init {
        progressBar.bind(duetData.map { it.fractionPrinted / 100 })
        progressLabel.bind(duetData.map { "${it.fractionPrinted}%" })

        timeLeftFilament.bind(duetData.map { it.timesLeft?.filament?.nullIfZero() ?: "n/a" })
        timeLeftFile.bind(duetData.map { it.timesLeft?.file?.nullIfZero() ?: "n/a" })
        timeLeftLayer.bind(duetData.map { it.timesLeft?.layer?.nullIfZero() ?: "n/a" })

        warmUp.bind(duetData.map { it.warmUpDuration?.nullIfZero() ?: "n/a" })
        currentLayer.bind(currentLayerTime.map { it?.nullIfZero() ?: "n/a" })

        currentLayerTime.onChange { it?.let(lastLayerTime::set) }

        lastLayer.bind(lastLayerTime.map { it?.toDouble()?.nullIfZero() ?: "n/a" })
        printDuration.bind(duetData.map { it.printDuration?.nullIfZero() ?: "n/a" })

        timeLeftFilamentTime.bind(duetData.map { it.timesLeft?.filament?.nullIfZero()?.toTime() ?: "n/a" })
        timeLeftFileTime.bind(duetData.map { it.timesLeft?.file?.nullIfZero()?.toTime() ?: "n/a" })
        timeLeftLayerTime.bind(duetData.map { it.timesLeft?.filament?.nullIfZero()?.toTime() ?: "n/a" })

        offsetAmount.bind(duetData.map { it.params.babystep })

        fileName.bind(currentFile.map { it?.fileName ?: "n/a" })
        fileSize.bind(currentFile.map { it?.size ?: "n/a" })
        objectHeight.bind(currentFile.map { it?.height ?: "n/a" })
        layerHeight.bind(currentFile.map { it?.let { "${it.firstLayerHeight} / ${it.layerHeight}" } ?: "n/a" })
        filamentUsage.bind(currentFile.map { it?.filament?.first() ?: "n/a" })
        generatedBy.bind(currentFile.map { it?.generatedBy ?: "n/a" })

        fanControl.popup {
            titledpane("Fan Control", collapsible = false) {
                alignment = Pos.CENTER
                gridpane {
                    vgap = 10.0
                    hgap = 5.0
                    row {
                        label("Tool Fan :")
                        slider(0, 100, 0).bind(duetData.map { it.params.fanPercent.firstOrNull() ?: 0.0 })
                    }
                    row {
                        label("Fan 0 :")
                        slider(0, 200, 100)
                    }
                }
            }
        }

        speedFactor.popup {
            titledpane("Speed Factor", collapsible = false) {
                alignment = Pos.CENTER
                gridpane {
                    row {
                        slider(0, 200, 100)
                    }
                }
            }
        }

        extrusionFactor.popup(100) {
            titledpane("Extrusion Factor", collapsible = false) {
                alignment = Pos.CENTER
                gridpane {
                    hgap = 5.0
                    row {
                        label("Extruder 0 :")
                        slider(0, 200, 100)
                    }
                }
            }
        }

        babySteppingDown.setOnAction { duetController.babyStepping(false) }
        babySteppingUp.setOnAction { duetController.babyStepping(true) }

        pauseResumeStart.bind(duetData.map {
            when (it.status) {
                Status.P -> {
                    pauseResumeStart.toggleClass("warning", true)
                    pauseResumeStart.toggleClass("success", false)
                    "Pause Print"
                }
                Status.A -> {
                    pauseResumeStart.toggleClass("warning", false)
                    pauseResumeStart.toggleClass("success", true)
                    "Resume Print"
                }
                else -> {
                    pauseResumeStart.toggleClass("warning", false)
                    pauseResumeStart.toggleClass("success", true)
                    "Print Another"
                }
            }
        })

        pauseResumeStart.setOnAction {
            when (duetData.value.status) {
                Status.P -> duetController.pausePrint()
                Status.A -> duetController.resumePrint()
                else -> currentFile.value?.let { duetController.selectFileAndPrint(it.fileName) }
            }
        }

        pauseResumeStart.disableProperty().bind(currentFile.map { it == null })
    }

    private fun Double.nullIfZero() = if (this == 0.0) null else this

    private fun Double.toTime() = LocalTime.now()
            .plusSeconds(toLong())
            .truncatedTo(ChronoUnit.SECONDS)
            .format(DateTimeFormatter.ISO_LOCAL_TIME)

}