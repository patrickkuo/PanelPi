package net.panelpi.views

import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import net.panelpi.DuetWifi
import net.panelpi.controllers.DuetController
import net.panelpi.map
import tornadofx.*

class MainView : View() {
    override val root: Parent by fxml()
    private val controller: DuetController by inject()

    private val status: Label by fxid()
    private val centerPane: BorderPane by fxid()
    private val printerName: Label by fxid()

    private val controlButton: Label by fxid()
    private val statusButton: Label by fxid()
    private val consoleButton: Label by fxid()
    private val fileButton: Label by fxid()
    private val settingButton: Label by fxid()

    private val stop: Button by fxid()

    private val statusView: StatusView by inject()
    private val controlView: ControlView by inject()
    private val settingView: SettingView by inject()
    private val comingSoon: ComingSoonView by inject()

    init {
        // Status icon
        status.bind(controller.duetData.map { it.status })
        status.styleProperty().bind(controller.duetData.map { "-fx-background-color: ${it.status.color}" })

        // Printer name
        printerName.bind(controller.duetData.map { it.name })

        // Menu buttons
        val allButton = mapOf(
                statusButton to statusView,
                controlButton to controlView,
                consoleButton to comingSoon,
                fileButton to comingSoon,
                settingButton to settingView)

        allButton.forEach { button, view ->
            button.setOnMouseClicked {
                if (!button.hasClass("menu-icon-selected")) {
                    centerPane.center = view.root
                    button.toggleClass("menu-icon-selected", true)
                    allButton.keys.filter { it != button }.forEach {
                        it.toggleClass("menu-icon-selected", false)
                    }
                }
            }
        }

        // Select control view by default.
        runLater {
            controlButton.fireEvent(MouseEvent(MouseEvent.MOUSE_CLICKED, 0.0, 0.0, 0.0, 0.0, MouseButton.PRIMARY, 1, false, false, false, false, false, false, false, true, false, false, null))
        }

        // Emergency stop button.
        stop.setOnAction {
            DuetWifi.instance.sendCmd("M112", "M999")
        }
    }
}
