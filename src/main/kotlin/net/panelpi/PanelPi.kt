package net.panelpi

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
}
