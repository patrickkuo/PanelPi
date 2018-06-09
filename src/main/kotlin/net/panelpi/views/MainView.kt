package net.panelpi.views

import net.panelpi.controllers.PanelPiController
import net.panelpi.map
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import tornadofx.*

class MainView : View() {
    override val root: Parent by fxml()
    private val controller: PanelPiController by inject()

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
    private val comingSoon: ComingSoonView by inject()

    init {
        status.bind(controller.duetData.map { it?.status ?: "disconnected" })
        status.styleProperty().bind(controller.duetData.map {
            "-fx-background-color: ${it?.status?.color ?: "GRAY"}"
        })

        printerName.bind(controller.duetData.map { it?.name ?: "Panel Pi" })

        controller.duet
        val allButton = mapOf(
                statusButton to statusView,
                controlButton to controlView,
                consoleButton to comingSoon,
                fileButton to comingSoon,
                settingButton to comingSoon)

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
        runLater {
            controlButton.fireEvent(MouseEvent(MouseEvent.MOUSE_CLICKED, 0.0, 0.0, 0.0, 0.0, MouseButton.PRIMARY, 1, false, false, false, false, false, false, false, true, false, false, null))
        }

        stop.setOnAction {
            controller.duet.sendCmd("M112")
            controller.duet.sendCmd("M999")
        }
    }


}
