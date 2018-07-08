package net.panelpi.views

import javafx.scene.Parent
import javafx.scene.control.Slider
import mu.KLogging
import net.panelpi.controllers.DuetController
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
        brightnessSlider.valueChangingProperty().onChange {
            if (!it) {
                PrintWriter(brightnessControl).use {
                    it.print(brightnessSlider.value.toInt().toString())
                }
            }
        }
    }

    fun logDuetData() {
        duetController.logDuetData()
    }
}