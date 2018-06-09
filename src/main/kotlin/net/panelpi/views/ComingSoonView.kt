package net.panelpi.views

import javafx.scene.Parent
import tornadofx.*

class ComingSoonView : View() {
    override val root: Parent = borderpane {
        center {
            label("Coming Soon...")
        }
    }
}