package net.panelpi.views

import javafx.scene.Parent
import javafx.scene.control.Slider
import mu.KLogging
import net.panelpi.controllers.DuetController
import net.panelpi.duetwifi.DuetWifi
import tornadofx.*
import java.io.PrintWriter
import java.nio.file.Paths

class SettingView : View() {
    override val root: Parent by fxml()
    private val brightnessSlider: Slider by fxid()
    private val duetController: DuetController by inject()

    companion object : KLogging()

    init {
        val brightnessControl = Paths.get("/sys/class/backlight/rpi_backlight/brightness").toFile()
        if (brightnessControl.exists()) {
            brightnessSlider.value = Paths.get("/sys/class/backlight/rpi_backlight/brightness").toFile().readLines().first().toDouble()
        }
        brightnessSlider.isDisable = !brightnessControl.exists()
        brightnessSlider.min = 0.0
        brightnessSlider.max = 255.0
        brightnessSlider.valueProperty().addListener { _, _, newValue ->
            if (!brightnessSlider.isValueChanging) {
                PrintWriter(brightnessControl).use {
                    it.print(newValue.toInt().toString())
                }
            }
        }
    }

    fun logDuetData() {
        duetController.logDuetData()
    }
}