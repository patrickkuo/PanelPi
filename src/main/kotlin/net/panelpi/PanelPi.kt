package net.panelpi

import javafx.stage.Stage
import javafx.stage.StageStyle
import mu.KLogging
import net.panelpi.controllers.DuetController
import net.panelpi.views.MainView
import tornadofx.*

class PanelPiApp : App(MainView::class) {
    companion object : KLogging()
    private val duetController: DuetController by inject()

    init {
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            logger.error(e) { "Uncaught exception." }
            duetController.logDuetData()
        }
    }

    override fun start(stage: Stage) {
        stage.initStyle(StageStyle.UNDECORATED)
        super.start(stage)
    }
}
