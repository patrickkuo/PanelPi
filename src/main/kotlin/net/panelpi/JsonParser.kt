package net.panelpi

import tornadofx.*
import java.lang.reflect.InvocationTargetException
import java.net.Proxy
import javax.json.JsonNumber
import javax.json.JsonObject
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

inline fun <reified T : Any> JsonObject.parseAs(): T = parseAs(T::class)

fun <T : Any> JsonObject.parseAs(clazz: KClass<T>): T {
    kotlin.require(clazz.isData) { "Only Kotlin data classes can be parsed. Offending: ${clazz.qualifiedName}" }
    val constructor = clazz.primaryConstructor!!
    val args = constructor.parameters.filterNot { it.isOptional && !containsKey(it.name!!) }.associateBy({ it }) { param ->
        // Get the matching property for this parameter
        val property = clazz.memberProperties.first { it.name == param.name }
        uncheckedCast(if (param.type.arguments.isEmpty()) getSingleValue(property.name, param.type) else getCollectionValue(property.name, param.type))
    }
    try {
        return constructor.callBy(args)
    } catch (e: InvocationTargetException) {
        throw e.cause!!
    }
}

private fun JsonObject.getSingleValue(path: String, type: KType): Any? {
    if (type.isMarkedNullable && !containsKey(path)) return null
    val typeClass = type.jvmErasure
    return when (typeClass) {
        String::class -> getString(path)
        Int::class -> getInt(path)
        Long::class -> getLong(path)
        Double::class -> getDouble(path)
        Boolean::class -> try {
            getBoolean(path)
        } catch (e: ClassCastException) {
            getInt(path) == 1
        }
        else -> if (typeClass.java.isEnum) {
            parseEnum(typeClass.java, getString(path))
        } else {
            getJsonObject(path).parseAs(typeClass)
        }
    }
}

private fun JsonObject.getCollectionValue(path: String, type: KType): Collection<Any> {
    val typeClass = type.jvmErasure
    kotlin.require(typeClass == kotlin.collections.List::class || typeClass == kotlin.collections.Set::class) { "$typeClass is not supported" }
    val elementClass = type.arguments[0].type?.jvmErasure
            ?: throw IllegalArgumentException("Cannot work with star projection: $type")
    if (!containsKey(path)) {
        return if (typeClass == List::class) emptyList() else emptySet()
    }
    val values: List<Any> = when (elementClass) {
        String::class -> getJsonArray(path).getValuesAs<String, JsonObject> { it.getString() }
        Int::class -> getJsonArray(path).getValuesAs<Int, JsonNumber> { it.intValue() }
        Long::class -> getJsonArray(path).getValuesAs<Long, JsonNumber> { it.longValue() }
        Double::class -> getJsonArray(path).getValuesAs<Double, JsonNumber> { it.doubleValue() }
        Boolean::class -> getJsonArray(path).getValuesAs<Boolean, JsonNumber> { it.intValue() == 1 }
        else -> if (elementClass.java.isEnum) {
            getJsonArray(path).map { parseEnum(elementClass.java, it.asJsonObject().getString()) }
        } else {
            getJsonArray(path).map { it.asJsonObject().parseAs(elementClass) }
        }
    }
    return if (typeClass == Set::class) values.toSet() else values
}

@Suppress("UNCHECKED_CAST")
fun <T, U : T> uncheckedCast(obj: T) = obj as U

private fun parseEnum(enumType: Class<*>, name: String): Enum<*> = enumBridge<Proxy.Type>(uncheckedCast(enumType), name) // Any enum will do
private fun <T : Enum<T>> enumBridge(clazz: Class<T>, name: String): T {
    try {
        return java.lang.Enum.valueOf(clazz, name)
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("$name is not one of { ${clazz.enumConstants.joinToString()} }")
    }
}
