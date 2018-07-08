package net.panelpi.views

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleDoubleProperty
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.layout.HBox
import net.panelpi.controllers.DuetController
import net.panelpi.map
import net.panelpi.models.Status
import tornadofx.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt


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

    private val pauseResume: Button by fxid()

    private val startStop: Button by fxid()

    private val fanPercent = duetData.map { it.params.fanPercent.firstOrNull() }
    private val extrudeFactor = duetData.map { it.params.extrFactors.firstOrNull() }
    private val speedFactorValue = duetData.map { it.params.speedFactor }

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
        fileSize.bind(currentFile.map { it?.size?.toHumanReadableByteCount() ?: "n/a" })
        objectHeight.bind(currentFile.map { it?.height?.let { "$it mm" } ?: "n/a" })
        layerHeight.bind(currentFile.map { it?.let { "${it.firstLayerHeight} / ${it.layerHeight} mm" } ?: "n/a" })
        filamentUsage.bind(currentFile.map { it?.filament?.first()?.let { "$it mm" } ?: "n/a" })
        generatedBy.bind(currentFile.map { it?.generatedBy ?: "n/a" })

        fanControl.popup {
            titledpane("Fan Control", collapsible = false) {
                alignment = Pos.CENTER
                gridpane {
                    vgap = 10.0
                    hgap = 5.0
                    row {
                        label("Fan 0:")
                        labeledSlider(0, 100) {
                            prefWidth = 300.0
                            fanPercent.onChange { it?.let { this.value = it } }
                            valueChangingProperty().onChange {
                                if (!it) {
                                    val intValue = value.roundToInt()
                                    value = intValue.toDouble()
                                    if (intValue != duetData.value.params.fanPercent.firstOrNull()?.toInt()) {
                                        duetController.setFanSpeed(intValue)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        speedFactor.popup {
            titledpane("Speed Factor", collapsible = false) {
                alignment = Pos.CENTER
                gridpane {
                    row {
                        labeledSlider(0.0, 300.0) {
                            prefWidth = 300.0
                            speedFactorValue.onChange { it?.let { this.value = it } }
                            valueChangingProperty().onChange {
                                if (!it) {
                                    val intValue = value.roundToInt()
                                    value = intValue.toDouble()
                                    if (value.toInt() != duetData.value.params.speedFactor.toInt()) {
                                        duetController.setSpeedFactorOverride(intValue)
                                    }
                                }
                            }
                        }
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
                        labeledSlider(0, 200) {
                            prefWidth = 300.0
                            extrudeFactor.onChange { it?.let { this.value = it } }
                            valueChangingProperty().onChange {
                                if (!it) {
                                    val intValue = value.roundToInt()
                                    value = intValue.toDouble()
                                    if (intValue != duetData.value.params.extrFactors.firstOrNull()?.toInt()) {
                                        duetController.setExtrudeFactorOverride(intValue)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        babySteppingDown.setOnAction { duetController.babyStepping(false) }
        babySteppingUp.setOnAction { duetController.babyStepping(true) }


        val resume = Label("Resume Print").withIcon(FontAwesomeIcon.PLAY)
        val pause = Label("Pause Print").withIcon(FontAwesomeIcon.PAUSE)
        pauseResume.graphicProperty().bind(duetData.map {
            when (it.status) {
                Status.A, Status.S -> {
                    pauseResume.toggleClass("warning", false)
                    pauseResume.toggleClass("success", true)
                    resume
                }
                else -> {
                    pauseResume.toggleClass("warning", true)
                    pauseResume.toggleClass("success", false)
                    pause
                }
            }
        })
        pauseResume.setOnAction {
            when (duetData.value.status) {
                Status.A, Status.S -> duetController.resumePrint()
                else -> duetController.pausePrint()
            }
        }
        pauseResume.disableProperty().bind(duetData.map {
            when (it.status) {
                Status.A, Status.S, Status.P -> false
                else -> true
            }
        })

        val stop = Label("Stop Print").withIcon(FontAwesomeIcon.STOP)
        val start = Label("Print Another").withIcon(FontAwesomeIcon.PLAY)
        startStop.graphicProperty().bind(duetData.map {
            when (it.status) {
                Status.A, Status.S, Status.P -> {
                    startStop.toggleClass("danger", true)
                    startStop.toggleClass("success", false)
                    stop
                }
                else -> {
                    startStop.toggleClass("danger", false)
                    startStop.toggleClass("success", true)
                    start
                }
            }
        })
        startStop.setOnAction {
            when (duetData.value.status) {
                Status.A, Status.S, Status.P -> duetController.stopPrint()
                else -> currentFile.value?.let { duetController.selectFileAndPrint(it.fileName) }
            }
        }

        startStop.disableProperty().bind(duetData.map {
            when (it.status) {
                Status.A, Status.S -> false
                Status.I -> currentFile.value == null
                else -> true
            }
        })
    }

    private fun Double.nullIfZero() = if (this == 0.0) null else this

    private fun Double.toTime() = LocalTime.now()
            .plusSeconds(toLong())
            .truncatedTo(ChronoUnit.SECONDS)
            .format(DateTimeFormatter.ISO_LOCAL_TIME)

    private fun Label.withIcon(icon: FontAwesomeIcon): Node {
        val iconView = FontAwesomeIconView(icon).apply {
            style = "-fx-fill: white;"
        }
        this.style = "-fx-text-fill: white;"
        return HBox(iconView, this).apply {
            spacing = 3.0
            alignment = Pos.CENTER
            maxWidth = Double.MAX_VALUE
        }
    }

}