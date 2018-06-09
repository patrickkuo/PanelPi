package net.panelpi.views

import net.panelpi.controllers.PanelPiController
import net.panelpi.map
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
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

    init {
        val homeButtons = listOf(homeX, homeY, homeZ)
        controller.duetData.map { it?.coords?.axesHomed ?: emptyList() }.addListener { _, _, newValue ->
            newValue.zip(homeButtons)
                    .forEach { (homed, button) ->
                        button.toggleClass("primary", homed)
                        button.toggleClass("warning", !homed)
                    }
        }

        controller.duetData.get()?.coords?.axesHomed?.zip(homeButtons)?.forEach { (homed, button) ->
            button.toggleClass("primary", homed)
            button.toggleClass("warning", !homed)
        }


        homeButtons.zip(listOf("X", "Y", "Z")).forEach { (button, name) ->
            button.setOnMouseClicked {
                controller.duet.sendCmd("G28 $name")
            }
        }

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
    }
}