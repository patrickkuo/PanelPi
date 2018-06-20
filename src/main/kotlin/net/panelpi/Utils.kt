package net.panelpi

import io.reactivex.Observable
import io.reactivex.plugins.RxJavaPlugins.onError
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import org.fxmisc.easybind.EasyBind
import tornadofx.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock
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

inline fun <T> Lock.tryWithLock(action: () -> T): T? {
    return if (tryLock()) {
        try {
            return action()
        } finally {
            unlock()
        }
    } else {
        null
    }
}

fun <T, R> Observable<T>.fold(accumulator: R, folderFun: (R, T) -> Unit): R {
    buffer(1, TimeUnit.SECONDS).subscribe({
        if (it.isNotEmpty()) {
            runLater {
                it.fold(accumulator) { list, item ->
                    folderFun.invoke(list, item)
                    list
                }
            }
        }
    }, ::onError)
    return accumulator
}

class ConcurrentBox<out T>(val content: T) {
    val lock = ReentrantReadWriteLock()

    inline fun <R> concurrent(block: T.() -> R): R = lock.read { block(content) }
    inline fun <R> exclusive(block: T.() -> R): R = lock.write { block(content) }
}
