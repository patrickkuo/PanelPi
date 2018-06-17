package net.panelpi.views

import javafx.scene.Parent
import javafx.scene.control.Slider
import mu.KLogging
import net.panelpi.DuetWifi
import tornadofx.*
import java.io.PrintWriter
import java.nio.file.Paths

class SettingView : View() {
    override val root: Parent by fxml()
    private val brightnessSlider: Slider by fxid()

    companion object : KLogging()

    init {
        if (!DuetWifi.instance.devMode) {
            brightnessSlider.value = Paths.get("/sys/class/backlight/rpi_backlight/brightness").toFile().readLines().first().toDouble()
        }
        brightnessSlider.min = 0.0
        brightnessSlider.max = 255.0
        brightnessSlider.valueProperty().addListener { _, _, newValue ->
            if (!brightnessSlider.isValueChanging) {
                if (!DuetWifi.instance.devMode) {
                    PrintWriter("/sys/class/backlight/rpi_backlight/brightness").use {
                        it.print(newValue.toInt().toString())
                    }
                }
            }
        }
    }

    fun logDuetData() {
        DuetWifi.instance.logDuetData()
    }
}