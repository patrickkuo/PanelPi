package net.panelpi.views

import javafx.event.EventTarget
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Slider
import javafx.scene.effect.DropShadow
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.stage.Popup
import javafx.util.StringConverter
import net.panelpi.map
import tornadofx.*

object DegreeConverter : StringConverter<Int>() {
    override fun toString(o: Int?): String {
        return o?.let { "$it °C" } ?: ""
    }

    override fun fromString(string: String?): Int {
        return string?.split(" ")?.firstOrNull()?.toInt() ?: 0
    }
}


fun Button.popup(triangleLocation: Int = 30, op: BorderPane.() -> Unit) {
    val popup = Popup().apply {
        scene.stylesheets.add("net/panelpi/css/PanelPi.css")
        content.addAll(stackpane {
            borderpane {
                StackPane.setMargin(this, Insets(16.0, 0.0, 0.0, 0.0))
                background = Background(BackgroundFill(Color.valueOf("#f4f4f4"), CornerRadii(10.0), Insets.EMPTY))
                border = Border(BorderStroke(Color.LIGHTGRAY, BorderStrokeStyle.SOLID, CornerRadii(10.0), BorderWidths(1.0)))
                padding = Insets(10.0, 10.0, 10.0, 10.0)
                effect = DropShadow(5.0, 3.0, 3.0, Color.GRAY)
                center {
                    op()
                }
            }
            canvas(20.0 + triangleLocation, 15.0) {
                StackPane.setAlignment(this, Pos.TOP_LEFT)
                StackPane.setMargin(this, Insets(2.0, 0.0, 0.0, 0.0))
                graphicsContext2D.lineWidth = 1.0
                graphicsContext2D.fill = Color.valueOf("#f4f4f4")
                graphicsContext2D.stroke = Color.LIGHTGRAY

                graphicsContext2D.fillPolygon(listOf(0.0, 10.0, 20.0).map { it + triangleLocation }.toDoubleArray(),
                        listOf(15.0, 0.0, 15.0).toDoubleArray(), 3)
                graphicsContext2D.strokeLine(0.0 + triangleLocation, 15.0, 10.0 + triangleLocation, 0.0)
                graphicsContext2D.strokeLine(10.0 + triangleLocation, 0.0, 20.0 + triangleLocation, 15.0)
                toFront()
            }
        })
    }

    setOnAction {
        val bound = localToScreen(boundsInLocal)
        if (popup.isShowing) {
            popup.hide()
        } else {
            popup.show(this, bound.minX - triangleLocation + 7, bound.maxY)
            requestFocus()
        }
    }

    focusedProperty().onChange {
        if (!it) {
            popup.hide()
        }
    }
}

fun EventTarget.labeledSlider(
        min: Number? = null,
        max: Number? = null,
        value: Number? = null,
        op: Slider.() -> Unit = {}
): Node {
    return AnchorPane().attachTo(this) {
        val slider = slider(min, max, value, op = op)
        AnchorPane.setTopAnchor(slider, 30.0)
        AnchorPane.setRightAnchor(slider, 20.0)
        AnchorPane.setLeftAnchor(slider, 10.0)
        val label = label(slider.valueProperty().map { "${it.toInt()} %" }) {
            style = "-fx-font-size: 12; -fx-text-fill: #428bca; -fx-background-color: #DEEFF7; -fx-background-radius: 5; -fx-label-padding: 4 8 4 8"
        }
        slider.minWidth = Double.MIN_VALUE
        canvas(10.0, 5.0) {
            AnchorPane.setTopAnchor(this, 23.0)
            AnchorPane.setLeftAnchor(this, 13.0)
            graphicsContext2D.fill = Color.valueOf("#DEEFF7")
            graphicsContext2D.fillPolygon(listOf(0.0, 5.0, 10.0).map { it }.toDoubleArray(),
                    listOf(0.0, 5.0, 0.0).toDoubleArray(), 3)

            runLaterRetry {
               val thumb =  slider.lookup(".thumb")
                AnchorPane.setLeftAnchor(this, thumb.boundsInParent.maxX - 2 - (boundsInParent.maxX - boundsInParent.minX) / 2)
                AnchorPane.setLeftAnchor(label, thumb.boundsInParent.maxX - 2 - (label.boundsInParent.maxX - label.boundsInParent.minX) / 2)
                thumb.boundsInParentProperty().onChange {
                    it?.let {
                        AnchorPane.setLeftAnchor(this, it.maxX - 2 - (boundsInParent.maxX - boundsInParent.minX) / 2)
                        AnchorPane.setLeftAnchor(label, it.maxX - 2 - (label.boundsInParent.maxX - label.boundsInParent.minX) / 2)
                    }
                }
            }
        }
        label.toFront()
        slider.toFront()
    }
}

fun Long.toHumanReadableByteCount(): String {
    val unit = 1024
    if (this < unit) return toString() + " B"
    val exp = (Math.log(toDouble()) / Math.log(unit.toDouble())).toInt()
    val pre = ("KMGTPE")[exp - 1] + "i"
    return String.format("%.1f %sB", this / Math.pow(unit.toDouble(), exp.toDouble()), pre)
}

fun runLaterRetry(op: () -> Unit) {
    runLater {
        try {
            op()
        } catch (e: Throwable) {
            // Ignore
            runLaterRetry(op)
        }
    }
}