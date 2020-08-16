/*
 * @(#) Constraints.kt
 *
 * json-kotlin-schema Kotlin implementation of JSON Schema
 * Copyright (c) 2020 Peter Wall
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.pwall.json.schema.codegen

import java.math.BigDecimal
import java.net.URI

import net.pwall.json.JSONArray
import net.pwall.json.JSONValue
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.validation.FormatValidator
import net.pwall.util.Strings

open class Constraints(val schema: JSONSchema) {

    var uri: URI? = schema.uri

    var packageName: String? = null
    var description: String? = null

    val systemClasses = mutableListOf<SystemClass>()
    val imports = mutableListOf<String>()

    var localTypeName: String? = null

    @Suppress("unused")
    val isLocalType: Boolean
        get() = localTypeName != null

    val types = mutableListOf<JSONSchema.Type>()

    var systemClass: SystemClass? = null

    var nullable: Boolean? = null

    var isRequired = false

    var defaultValue: DefaultValue? = null

    val properties = mutableListOf<NamedConstraints>()

    val required = mutableListOf<String>()

    var arrayItems: Constraints? = null

    var minimum: Number? = null // Number will be BigDecimal, Long or Int
    var exclusiveMinimum: Number? = null
    var maximum: Number? = null
    var exclusiveMaximum: Number? = null
    var multipleOf: Number? = null

    var maxLength: Int? = null
    var minLength: Int? = null
    var format: FormatValidator.FormatType? = null
    var regex: Regex? = null

    var enumValues: JSONArray? = null
    var constValue: JSONValue? = null

    val nestedClasses = mutableListOf<NamedConstraints>()

    @Suppress("unused")
    val nestedClassesPresent: Boolean
        get() = nestedClasses.isNotEmpty()

    @Suppress("unused")
    var validationsPresent: Boolean = false

    @Suppress("unused")
    val validationsOrNestedClassesPresent: Boolean
        get() = validationsPresent || nestedClassesPresent

    @Suppress("unused")
    val isSystemClass: Boolean
        get() = systemClass != null

    @Suppress("unused")
    val minimumPresent: Boolean
        get() = minimum != null

    @Suppress("unused")
    val exclusiveMinimumPresent: Boolean
        get() = exclusiveMinimum != null

    @Suppress("unused")
    val maximumPresent: Boolean
        get() = maximum != null

    @Suppress("unused")
    val exclusiveMaximumPresent: Boolean
        get() = exclusiveMaximum != null

    @Suppress("unused")
    val multipleOfPresent: Boolean
        get() = multipleOf != null

    @Suppress("unused")
    val nameFromURI: String? by lazy {
        uri?.let {
            // TODO change to allow name ending with "/schema"
            val uriName = it.toString().substringBefore('#').substringAfterLast('/')
            val uriNameWithoutSuffix = when {
                uriName.endsWith(".schema.json", ignoreCase = true) -> uriName.dropLast(12)
                uriName.endsWith("-schema.json", ignoreCase = true) -> uriName.dropLast(12)
                uriName.endsWith("_schema.json", ignoreCase = true) -> uriName.dropLast(12)
                uriName.endsWith(".schema", ignoreCase = true) -> uriName.dropLast(7)
                uriName.endsWith("-schema", ignoreCase = true) -> uriName.dropLast(7)
                uriName.endsWith("_schema", ignoreCase = true) -> uriName.dropLast(7)
                uriName.endsWith(".json", ignoreCase = true) -> uriName.dropLast(5)
                else -> uriName
            }
            uriNameWithoutSuffix.split('-', '.').joinToString(separator = "") { part -> Strings.capitalise(part) }.
                    sanitiseName()
        }
    }

    @Suppress("unused")
    val nameFromTitle: String? by lazy {
        schema.title?.split(' ')?.joinToString(separator = "") { part -> Strings.capitalise(part) }
    }

    @Suppress("unused")
    val nameFromURIOrTitle: String?
        get() = nameFromURI ?: nameFromTitle

    @Suppress("unused")
    val nameFromTitleOrURI: String?
        get() = nameFromTitle ?: nameFromURI

    @Suppress("unused")
    val safeDescription: String?
        get() = schema.description?.trim()?.replace("*/", "* /")

    @Suppress("unused")
    val isIdentifiableType: Boolean
        get() = types.size == 1

    @Suppress("unused")
    val isObject: Boolean
        get() = types.size == 1 && types[0] == JSONSchema.Type.OBJECT || types.isEmpty() && properties.isNotEmpty() // ?

    @Suppress("unused")
    val isArray: Boolean
        get() = types.size == 1 && types[0] == JSONSchema.Type.ARRAY || types.isEmpty() && arrayItems != null // ???

    @Suppress("unused")
    val isString: Boolean
        get() = types.size == 1 && types[0] == JSONSchema.Type.STRING || types.isEmpty() && properties.isEmpty() &&
                arrayItems == null && (format != null || regex != null || maxLength != null || minLength != null) // ???

    @Suppress("unused")
    val isBoolean: Boolean
        get() = types.size == 1 && types[0] == JSONSchema.Type.BOOLEAN

    @Suppress("unused")
    val isDecimal: Boolean
        get() = types.size == 1 && types[0] == JSONSchema.Type.NUMBER && !(multipleOf is Long || multipleOf is Int)

    @Suppress("unused")
    val isInt: Boolean
        get() = isIntOrLong && rangeAllowsInteger()

    @Suppress("unused")
    val isLong: Boolean
        get() = isIntOrLong && !rangeAllowsInteger()

    @Suppress("unused")
    val isIntOrLong: Boolean
        get() = types.size == 1 &&
                (types[0] == JSONSchema.Type.INTEGER ||
                        types[0] == JSONSchema.Type.NUMBER && (multipleOf is Long || multipleOf is Int)) // ?

    @Suppress("unused")
    val isPrimitive: Boolean
        get() = isIntOrLong || isBoolean

//    fun negate() {
//        TODO()
//    }

//    fun combine(type: String, constraintsList: List<Constraints>) {
//        TODO()
//    }

    private fun rangeAllowsInteger(): Boolean = minimumOK() && maximumOK()

    private fun minimumOK(): Boolean {
        if (minimum == null && exclusiveMinimum == null)
            return false
        if (minimum.belowLimitForInt(Int.MIN_VALUE.toLong()))
            return false
        if (exclusiveMinimum.belowLimitForInt(Int.MIN_VALUE.toLong() - 1))
            return false
        return true
    }

    private fun maximumOK(): Boolean {
        if (maximum == null && exclusiveMaximum == null)
            return false
        if (maximum.aboveLimitForInt(Int.MAX_VALUE.toLong()))
            return false
        if (exclusiveMaximum.aboveLimitForInt(Int.MAX_VALUE.toLong() + 1))
            return false
        return true
    }

    data class DefaultValue(val defaultValue: Any?, val type: JSONSchema.Type)

    enum class SystemClass(val order: Int) {
        // collections etc.
        LIST(0),
        // math etc.
        DECIMAL(20),
        // date, time etc.
        DATE(40),
        DATE_TIME(41),
        TIME(42),
        DURATION(45),
        // utility
        UUID(60),
        // local
        VALIDATION(90)
    }

    companion object {

        fun String.sanitiseName(): String {
            for (i in 0 until length) {
                val ch = this[i]
                if (!(ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9')) {
                    val sb = StringBuilder(substring(0, i))
                    for (j in i + 1 until length) {
                        val ch2 = this[j]
                        if (ch2 in 'A'..'Z' || ch2 in 'a'..'z' || ch2 in '0'..'9')
                            sb.append(ch)
                    }
                    return sb.toString()
                }
            }
            return this
        }

        private fun Number?.belowLimitForInt(limit: Long): Boolean = when (this) {
            null -> false
            is BigDecimal -> this < BigDecimal(limit)
            else -> this.toLong() < limit
        }

        private fun Number?.aboveLimitForInt(limit: Long): Boolean = when (this) {
            null -> false
            is BigDecimal -> this > BigDecimal(limit)
            else -> this.toLong() > limit
        }

        fun minimumOf(a: Number?, b: Number?): Number? {
            return when (a) {
                null -> b
                is BigDecimal -> when (b) {
                    null -> a
                    is BigDecimal -> if (a < b) a else b
                    else -> if (a < BigDecimal(b.toLong())) a else b
                }
                else -> when (b) {
                    null -> a
                    is BigDecimal -> if (BigDecimal(a.toLong()) < b) a else b
                    else -> if (a.toLong() < b.toLong()) a else b
                }
            }
        }

        fun maximumOf(a: Number?, b: Number?): Number? {
            return when (a) {
                null -> b
                is BigDecimal -> when (b) {
                    null -> a
                    is BigDecimal -> if (a > b) a else b
                    else -> if (a > BigDecimal(b.toLong())) a else b
                }
                else -> when (b) {
                    null -> a
                    is BigDecimal -> if (BigDecimal(a.toLong()) > b) a else b
                    else -> if (a.toLong() > b.toLong()) a else b
                }
            }
        }

        fun lcm(a: Number?, b: Number?): Number? { // TODO this isn't really LCM
            return when (a) {
                null -> b
                is BigDecimal -> when (b) {
                    null -> a
                    is BigDecimal -> a.multiply(b)
                    else -> a.multiply(BigDecimal(b.toLong()))
                }
                else -> when (b) {
                    null -> a
                    is BigDecimal -> BigDecimal(a.toLong()).multiply(b)
                    else -> pseudoLCM(a.toLong(), b.toLong()).intOrLong()
                }
            }
        }

        private fun pseudoLCM(a: Long, b: Long): Long {
            require(a > 0 && b > 0)
            var aVar = a
            var aZeroBits = 0
            while ((aVar and 1) == 0L) {
                aVar = aVar shr 1
                aZeroBits++
            }
            var bVar = b
            var bZeroBits = 0
            while ((bVar and 1) == 0L) {
                bVar = bVar shr 1
                bZeroBits++
            }
            return (aVar * bVar) shl aZeroBits.coerceAtLeast(bZeroBits)
        }

        private fun Long.intOrLong(): Number =
                if (this in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) toInt() else this

    }

}
