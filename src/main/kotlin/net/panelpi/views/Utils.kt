package net.panelpi.views

import javafx.util.StringConverter

object DegreeConverter : StringConverter<Int>() {
    override fun toString(o: Int?): String {
        return o?.let { "$it °C" } ?: ""
    }

    override fun fromString(string: String?): Int {
        return string?.split(" ")?.firstOrNull()?.toInt() ?: 0
    }
}
