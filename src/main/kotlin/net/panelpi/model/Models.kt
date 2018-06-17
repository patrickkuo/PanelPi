package net.panelpi.model

import java.util.*
import kotlin.reflect.KClass

object Models {
    private val modelStore = HashMap<KClass<*>, Any>()

    private val dependencyGraph = HashMap<KClass<*>, MutableSet<KClass<*>>>()

    fun <M : Any> initModel(klass: KClass<M>) = modelStore.getOrPut(klass) { klass.java.newInstance() }
    fun <M : Any> get(klass: KClass<M>, origin: KClass<*>): M {
        dependencyGraph.getOrPut(origin) { mutableSetOf() }.add(klass)
        val model = initModel(klass)
        if (model.javaClass != klass.java) {
            throw IllegalStateException("Model stored as ${klass.qualifiedName} has type ${model.javaClass}")
        }
        return model as M
    }

    inline fun <reified M : Any> get(origin: KClass<*>): M = get(M::class, origin)
}