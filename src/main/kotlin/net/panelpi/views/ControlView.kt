package net.panelpi.views

import javafx.beans.value.ObservableValue
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.util.StringConverter
import net.panelpi.controllers.PanelPiController
import net.panelpi.map
import tornadofx.*

class ControlView : View() {
    override val root: Parent by fxml()
    private val controller: PanelPiController by inject()

    private val homeX: Button by fxid()
    private val homeY: Button by fxid()
    private val homeZ: Button by fxid()

    private val onButton: ToggleButton by fxid()
    private val offButton: ToggleButton by fxid()

    private val bedComp: Button by fxid()
    private val gridComp: Button by fxid()
    private val homeAll: Button by fxid()

    private val xAmount: ComboBox<String> by fxid()
    private val yAmount: ComboBox<String> by fxid()
    private val zAmount: ComboBox<String> by fxid()

    private val xLeft: Button by fxid()
    private val xRight: Button by fxid()

    private val yLeft: Button by fxid()
    private val yRight: Button by fxid()

    private val zLeft: Button by fxid()
    private val zRight: Button by fxid()

    private val toolTemp: Label by fxid()
    private val bedTemp: Label by fxid()

    private val xCoord: Label by fxid()
    private val yCoord: Label by fxid()
    private val zCoord: Label by fxid()

    private val extrude: Button by fxid()
    private val retract: Button by fxid()

    private val feedAmountCB: ComboBox<String> by fxid()
    private val feedRateCB: ComboBox<String> by fxid()

    private val bedActiveTemp: ComboBox<Int> by fxid()
    private val toolActiveTemp: ComboBox<Int> by fxid()
    private val toolStandbyTemp: ComboBox<Int> by fxid()

    private val fanSlider: Slider by fxid()
    private val fanSliderAmount: Label by fxid()

    // Observable value for listening will need to be declared here, or else listener will get GCed and won't trigger event.
    private val coords = controller.duetData.map { it?.coords }
    private val xHomed = coords.map { it?.xHomed ?: false }
    private val yHomed = coords.map { it?.yHomed ?: false }
    private val zHomed = coords.map { it?.zHomed ?: false }

    private val bedActive = controller.duetData.map { it?.temps?.bed?.active }
    private val toolActive = controller.duetData.map { it?.temps?.tools?.active?.firstOrNull()?.firstOrNull() }
    private val toolStandby = controller.duetData.map { it?.temps?.tools?.standby?.firstOrNull()?.firstOrNull() }

    init {
        root.disableProperty().bind(controller.duetData.map { it == null })

        bindHomeButton(homeX, xHomed, "X")
        bindHomeButton(homeY, yHomed, "Y")
        bindHomeButton(homeZ, zHomed, "Z")

        val toggleGroup = ToggleGroup()
        onButton.toggleGroup = toggleGroup
        offButton.toggleGroup = toggleGroup

        onButton.setOnAction {
            if (controller.duetData.value?.params?.atxPower == false) {
                controller.duet.sendCmd("M80")
            }
            it.consume()
            onButton.isSelected = controller.duetData.value?.params?.atxPower == true
        }
        offButton.setOnAction {
            if (controller.duetData.value?.params?.atxPower == true) {
                controller.duet.sendCmd("M81")
            }
            it.consume()
            offButton.isSelected = controller.duetData.value?.params?.atxPower == false
        }

        bedComp.setOnAction { controller.duet.sendCmd("G32") }
        homeAll.setOnAction { controller.duet.sendCmd("G28") }
        gridComp.setOnAction { controller.duet.sendCmd("G29") }

        val amounts = observableList("100", "10", "1", "0.1")

        listOf(xAmount, yAmount, zAmount).forEach {
            it.items = amounts
            it.selectionModel.select(0)
        }

        bedTemp.bind(controller.duetData.map { it?.temps?.bed?.let { "${it.current}°C" } ?: "--" })
        toolTemp.bind(controller.duetData.map { it?.temps?.current?.let { "${it.first()}°C" } ?: "--" })

        xCoord.bind(controller.duetData.map { "${it?.coords?.x}" })
        yCoord.bind(controller.duetData.map { "${it?.coords?.y}" })
        zCoord.bind(controller.duetData.map { "${it?.coords?.z}" })

        val tempEnableProperties = controller.duetData.map { (it?.temps?.current?.first() ?: 0.0) < 180 }

        val feedAmounts = observableList("100", "50", "20", "10", "5", "1")
        val feedRate = observableList("60", "30", "15", "5", "1")

        feedAmountCB.items = feedAmounts
        feedRateCB.items = feedRate

        feedAmountCB.selectionModel.select("10")
        feedRateCB.selectionModel.select("5")

        extrude.disableProperty().bind(tempEnableProperties)
        retract.disableProperty().bind(tempEnableProperties)

        // TODO: Make these configurable.
        val bedTemps = observableList(0, 55, 60, 65, 70, 80, 90, 110, 120)
        val toolTemps = observableList(0, 130, 180, 190, 200, 210, 220, 230, 235)

        bedActiveTemp.items = bedTemps
        toolActiveTemp.items = toolTemps
        toolStandbyTemp.items = toolTemps

        configTemperatureComboBox(bedActiveTemp, toolActiveTemp, toolStandbyTemp)

        bedActive.addListener { _, _, newValue -> bedActiveTemp.selectionModel.select(newValue) }
        toolActive.addListener { _, _, newValue -> toolActiveTemp.selectionModel.select(newValue) }
        toolStandby.addListener { _, _, newValue -> toolStandbyTemp.selectionModel.select(newValue) }

        bedActiveTemp.setOnAction {
            val selected = bedActiveTemp.selectionModel.selectedItem
            if (controller.duetData.get().temps.bed.active != selected) {
                controller.duet.sendCmd("M140 S$selected")
            }
        }

        toolActiveTemp.setOnAction {
            val selected = toolActiveTemp.selectionModel.selectedItem
            if (controller.duetData.get().temps.tools.active.first().first() != selected) {
                // TODO: Support more extruder
                controller.duet.sendCmd("G10 P0 S$selected")
            }
        }

        toolStandbyTemp.setOnAction {
            val selected = toolStandbyTemp.selectionModel.selectedItem
            if (controller.duetData.get().temps.tools.standby.first().first() != selected) {
                // TODO: Support more extruder
                controller.duet.sendCmd("G10 P0 R$selected")
            }
        }

        fanSlider.min = 0.0
        fanSlider.max = 100.0
        fanSlider.majorTickUnit = 1.0
        fanSliderAmount.bind(fanSlider.valueProperty().map { "${it.toInt()} %" })
    }

    private fun bindHomeButton(homeButton: Button, homed: ObservableValue<Boolean>, axisName: String) {
        homed.addListener { _, _, newValue ->
            homeButton.toggleClass("primary", newValue)
            homeButton.toggleClass("warning", !newValue)
        }
        homeButton.setOnMouseClicked {
            controller.duet.sendCmd("G28 $axisName")
        }
    }

    private fun configTemperatureComboBox(vararg comboBox: ComboBox<Int>) {
        comboBox.forEach {
            it.converter = DegreeConverter
            it.buttonCell = object : ListCell<Int>() {
                override fun updateItem(item: Int?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (item != null) {
                        text = "$item °C"
                        alignment = Pos.CENTER_RIGHT
                        padding = Insets(padding.top, 0.0, padding.bottom, 0.0)
                    }
                }
            }
        }
    }

    private object DegreeConverter : StringConverter<Int>() {
        override fun toString(o: Int?): String {
            return o?.let { "$it °C" } ?: ""
        }

        override fun fromString(string: String?): Int {
            return string?.split(" ")?.firstOrNull()?.toInt() ?: 0
        }
    }
}