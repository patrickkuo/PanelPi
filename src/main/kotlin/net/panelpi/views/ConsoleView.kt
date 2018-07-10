package net.panelpi.views

import com.sun.javafx.scene.control.skin.FXVK
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.geometry.VPos
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.layout.FlowPane
import javafx.scene.layout.GridPane
import net.panelpi.controllers.DuetController
import net.panelpi.models.ConsoleMessage
import net.panelpi.models.MessageType
import tornadofx.*

class ConsoleView : View() {
    override val root: Parent by fxml()

    private val duet: DuetController by inject()
    private val console: ListView<ConsoleMessage> by fxid()
    private val clear: Button by fxid()
    private val send: Button by fxid()
    private val buttonsPane: FlowPane by fxid()
    private val commandTextField: TextField by fxid()

    init {
        console.apply {
            items = duet.console
            cellFormat {
                toggleClass("console-command", it.messageType == MessageType.COMMAND)
                toggleClass("console-info", it.messageType == MessageType.INFO)
                graphic = gridpane {
                    hgap = 40.0
                    row {
                        label(it.timestamp.toString().split(".").first()) {
                            style = "-fx-font-weight: bold;"
                            GridPane.setValignment(this, VPos.TOP)
                        }
                        vbox {
                            it.commands?.let {
                                label(it.joinToString("\n")) {
                                    style = "-fx-font-weight: bold;"
                                }
                            }
                            it.message?.let { msg ->
                                label(msg) {
                                    if (it.commands == null) {
                                        style = "-fx-font-weight: bold;"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        clear.setOnAction { duet.console.clear() }

        val buttonText = listOf(
                'D', 'E', 'F', 'H',
                'I', 'J', 'N', 'P',
                'R', 'S', 'T', 'X',
                '7', '8', '9', 'Y',
                '4', '5', '6', 'Z',
                '1', '2', '3', ' ',
                'G', '0', 'M')

        buttonText.forEach { text ->
            buttonsPane.add(Button(text.toString()).apply {
                toggleClass("info", text.isLetter() && text !in "GM")
                toggleClass("success", text in "GM")
                prefWidth = 46.0
                setOnAction {
                    commandTextField.text += text
                    commandTextField.requestFocus()
                    commandTextField.positionCaret(commandTextField.text.length)
                }
            })
        }

        buttonsPane.add(Button().apply {
            addClass("danger")
            prefWidth = 46.0
            graphic = FontAwesomeIconView(FontAwesomeIcon.LONG_ARROW_LEFT).apply {
                style = " -glyph-size: 20px; -fx-fill: white;"
            }
            setOnAction {
                commandTextField.text = commandTextField.text.dropLast(1)
                commandTextField.requestFocus()
                commandTextField.positionCaret(commandTextField.text.length)
            }
        })

        send.setOnAction {
            duet.sendCmd(commandTextField.text)
            commandTextField.clear()
            commandTextField.positionCaret(commandTextField.text.length)
        }

        commandTextField.focusedProperty().onChange {
            FXVK.detach()
        }
    }
}
