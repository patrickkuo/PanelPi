package net.panelpi

import javafx.beans.binding.Bindings
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import org.fxmisc.easybind.EasyBind
import java.time.Duration
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.json.Json
import javax.json.JsonObject
import kotlin.concurrent.read
import kotlin.concurrent.write

fun String.appendCheckSum(): String {
    val checkSum = if (isEmpty()) {
        0
    } else {
        toCharArray().map(Char::toInt).reduce { acc, i -> acc xor i }
    }
    return "${this}*$checkSum"
}

fun <A, B> ObservableValue<out A>.map(function: (A) -> B): ObservableValue<B> = EasyBind.map(this, function)

fun <A, B> ObservableList<out A>.map(function: (A) -> B): ObservableList<B> = EasyBind.map(this, function)

fun <A, B> ObservableList<out A>.fold(initial: B, folderFunction: (B, A) -> B): ObservableValue<B> {
    return Bindings.createObjectBinding({
        var current = initial
        forEach {
            current = folderFunction(current, it)
        }
        current
    }, arrayOf(this))
}

class ConcurrentBox<out T>(val content: T) {
    val lock = ReentrantReadWriteLock()

    inline fun <R> concurrent(block: T.() -> R): R = lock.read { block(content) }
    inline fun <R> exclusive(block: T.() -> R): R = lock.write { block(content) }
}

val Number.millis: Duration get() = Duration.ofMillis(this.toLong())
val Number.seconds: Duration get() = Duration.ofSeconds(this.toLong())
val Number.minutes: Duration get() = Duration.ofMinutes(this.toLong())
val Number.hours: Duration get() = Duration.ofHours(this.toLong())

operator fun JsonObject.plus(other: JsonObject): JsonObject {
    val builder = Json.createObjectBuilder()
    forEach { t, u -> builder.add(t, u) }
    other.forEach { t, u -> builder.add(t, u) }
    return builder.build()
}

operator fun JsonObject.plus(other: Pair<String, String>): JsonObject {
    val builder = Json.createObjectBuilder()
    forEach { t, u -> builder.add(t, u) }
    builder.add(other.first, other.second)
    return builder.build()
}
