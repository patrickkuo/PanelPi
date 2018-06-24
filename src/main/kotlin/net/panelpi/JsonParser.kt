package net.panelpi

import mu.KotlinLogging
import tornadofx.*
import java.lang.reflect.InvocationTargetException
import java.net.Proxy
import java.time.LocalDateTime
import javax.json.Json
import javax.json.JsonNumber
import javax.json.JsonObject
import javax.json.JsonString
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

val logger = KotlinLogging.logger {}

inline fun <reified T : Any> JsonObject.parseAs(): T? = try {
    parseAs(T::class)
} catch (e: Exception) {
    logger.debug(e) { "Error parsing ${T::class.simpleName}" }
    null
}

fun <T : Any> JsonObject.parseAs(clazz: KClass<T>): T {
    require(clazz.isData) { "Only Kotlin data classes can be parsed. Offending: ${clazz.qualifiedName}" }
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
        LocalDateTime::class -> LocalDateTime.parse(getString(path))
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
    val values: List<Any> = try {
        val array = getJsonArray(path)
        when (elementClass) {
            String::class -> array.getValuesAs<String, JsonString> { it.string }
            Int::class -> array.getValuesAs<Int, JsonNumber> { it.intValue() }
            Long::class -> array.getValuesAs<Long, JsonNumber> { it.longValue() }
            Double::class -> array.getValuesAs<Double, JsonNumber> { it.doubleValue() }
            Boolean::class -> array.getValuesAs<Boolean, JsonNumber> { it.intValue() == 1 }
            List::class -> array.map { Json.createObjectBuilder().add("array", it).build().getCollectionValue("array", type.arguments.first().type!!) }
            else -> if (elementClass.java.isEnum) {
                array.map { parseEnum(elementClass.java, it.asJsonObject().getString()) }
            } else {
                array.map { it.asJsonObject().parseAs(elementClass) }
            }
        }
    } catch (e: Throwable) {
        logger.error { "Error parsing property $path" }
        throw e
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
