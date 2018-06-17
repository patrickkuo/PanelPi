package net.panelpi

import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import org.fxmisc.easybind.EasyBind
import java.util.concurrent.TimeUnit

fun String.appendCheckSum(): String {
    val checkSum = if (isEmpty()) {
        0
    } else {
        toCharArray().map(Char::toInt).reduce { acc, i -> acc xor i }
    }
    return this + "*" + checkSum
}

fun <A, B> ObservableValue<out A>.map(function: (A) -> B): ObservableValue<B> = EasyBind.map(this, function)

fun <A, B> ObservableList<out A>.map(function: (A) -> B): ObservableList<B> = EasyBind.map(this, function)

fun List<String>.runCommand(): String {
    val proc = ProcessBuilder(this)
            .start()

    proc.waitFor(60, TimeUnit.MINUTES)
    return proc.inputStream.bufferedReader().readLines().firstOrNull() ?: ""
}