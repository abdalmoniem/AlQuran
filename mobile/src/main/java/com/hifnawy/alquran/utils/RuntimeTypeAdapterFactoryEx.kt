package com.hifnawy.alquran.utils

import com.google.gson.typeadapters.RuntimeTypeAdapterFactory
import kotlin.reflect.KClass

/**
 * Util class providing extension properties and functions for [RuntimeTypeAdapterFactory].
 *
 * @author AbdElMoniem ElHifnawy
 */
object RuntimeTypeAdapterFactoryEx {

    /**
     * Recursively finds all sealed subclasses of the given sealed class.
     *
     * @return [List<KClass< out T >>][List] A list of all sealed subclasses, including nested sealed classes.
     */
    @Suppress("NO_REFLECTION_IN_CLASS_PATH")
    private val <T : Any> KClass<T>.allSealedLeafSubclasses: List<KClass<out T>>
        get() = sealedSubclasses.flatMap { sub ->
            when {
                sub.isSealed -> sub.allSealedLeafSubclasses
                else         -> listOf(sub)
            }
        }

    /**
     * Returns the type field name used by the RuntimeTypeAdapterFactory.
     *
     * @return [String] The type field name.
     */
    val RuntimeTypeAdapterFactory<*>.registeredTypeFieldName: String
        get() {
            val clazz = this::class.java
            val typeField = clazz.getDeclaredField("typeFieldName").apply { isAccessible = true }
            return typeField.get(this) as String
        }

    /**
     * Returns a string representation of the registered subtypes in the RuntimeTypeAdapterFactory.
     *
     * @return [String] A string containing the registered subtypes and their corresponding labels.
     */
    val RuntimeTypeAdapterFactory<*>.registeredSubtypes: String
        get() {
            val clazz = this::class.java

            val labelToSubtypeField = clazz.getDeclaredField("labelToSubtype").apply { isAccessible = true }
            val subtypeToLabelField = clazz.getDeclaredField("subtypeToLabel").apply { isAccessible = true }

            @Suppress("UNCHECKED_CAST")
            val labelToSubtype = labelToSubtypeField.get(this) as Map<String, Class<*>>

            @Suppress("UNCHECKED_CAST")
            val subtypeToLabel = subtypeToLabelField.get(this) as Map<Class<*>, String>

            return buildString {
                appendLine("Subtype - Label:")
                labelToSubtype.forEach { label, subtype -> appendLine("$label → ${subtype.name}") }

                appendLine("Label - Subtype:")
                subtypeToLabel.forEach { subtype, label -> appendLine("${subtype.name} → $label") }
            }
        }

    /**
     * Extension function to automatically register all sealed subclasses
     * with Gson's RuntimeTypeAdapterFactory.
     *
     * @return [RuntimeTypeAdapterFactory < T >][RuntimeTypeAdapterFactory] A configured RuntimeTypeAdapterFactory for the sealed class.
     */
    val <T : Any> KClass<T>.registerSealedSubtypes: RuntimeTypeAdapterFactory<T>
        get() = registerSealedSubtypes("${this.simpleName?.lowercase()}_type")

    /**
     * Extension function to automatically register all sealed subclasses
     * with Gson's RuntimeTypeAdapterFactory.
     *
     * @param typeFieldName [String] The name of the JSON field that holds the subtype tag (e.g., "type").
     *
     * @return [RuntimeTypeAdapterFactory < T >][RuntimeTypeAdapterFactory] A configured RuntimeTypeAdapterFactory for the sealed class.
     */
    fun <T : Any> KClass<T>.registerSealedSubtypes(typeFieldName: String): RuntimeTypeAdapterFactory<T> {
        @Suppress("NO_REFLECTION_IN_CLASS_PATH")
        require(this.isSealed)

        var factory = RuntimeTypeAdapterFactory.of(this.java, typeFieldName)

        this.allSealedLeafSubclasses.forEach { kSubclass ->
            val tag = kSubclass.simpleName?.lowercase()
            factory = factory.registerSubtype(kSubclass.java as Class<T>, tag)
        }

        return factory
    }
}
