package cc.utils

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.afterburner.AfterburnerModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.File
import java.io.InputStream

object JSON {

    val mapper = ObjectMapper()

    init {
        mapper.registerModule(AfterburnerModule())
        mapper.registerModule(JavaTimeModule())
        mapper.registerModule(KotlinModule.Builder().build())

        mapper.configure(WRITE_BIGDECIMAL_AS_PLAIN, true)
        mapper.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
        mapper.enable(JsonParser.Feature.ALLOW_COMMENTS)
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        // normal.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT); // FIX FOR unicredit mobile app
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)

        val module = SimpleModule()
        mapper.registerModule(module)
    }

    fun parse(json: String) = mapper.readTree(json)

    fun parseObject(json: String) = mapper.readTree(json) as ObjectNode

    fun parseToMap(json: String): Map<String, String> {
        val o = mapper.readTree(json) as ObjectNode
        val ret = HashMap<String, String>()
        o.fieldNames().forEach {
            ret[it] = o.get(it).asText()
        }
        return ret
    }

    fun anyToObject(data: Any): ObjectNode =
        if (data is ObjectNode) {
            data
        } else {
            parseObject(stringify(data))
        }

    inline fun <reified T> parse(json: String): T =
        mapper.readValue(json, T::class.java)

    inline fun <reified T> parse(json: InputStream): T =
        mapper.readValue(json, T::class.java)

    inline fun <reified T> parseList(json: String): List<T> {
        val array = mapper.readTree(json) as ArrayNode
        return array.map { mapper.treeToValue(it, T::class.java) }
    }

    inline fun <reified T> parseConfDirFile(fileName: String): T {
        val confFolder = System.getProperty("service.conf.dir")
        val file = if (fileName.startsWith("/")) File(fileName) else File(confFolder, fileName)
        val configRaw = file.readText()
        return parse<T>(configRaw)
    }

    fun <T> parse(json: String, clazz: Class<T>): T =
        mapper.readValue(json, clazz)

    fun <T> parse(json: String, tr: TypeReference<T>): T =
        mapper.readValue(json, tr)

    inline fun <reified T> parse(json: JsonNode): T =
        mapper.treeToValue(json, T::class.java)

    fun <T> parse(json: JsonNode, clazz: Class<T>): T =
        mapper.treeToValue(json, clazz)

    fun <T> stringify(data: T): String =
        mapper.writeValueAsString(data)

    fun minifyJson(json: String): String =
        mapper.writeValueAsString(mapper.readTree(json))

    fun toNode(data: Any): JsonNode =
        mapper.valueToTree(data)

    fun toObject(data: Any): ObjectNode =
        mapper.valueToTree(data)

    fun toArray(data: List<Any>): ArrayNode =
        mapper.valueToTree(data)

    fun objectNode() = mapper.createObjectNode()

    fun escapeText(text: String): String {
        val t = JSON.toNode(text).toString()
        return t.substring(1, t.length - 1)
    }

    fun canBeParsed(json: String, javaClass: Class<out Any>): Boolean {
        return runCatching {
            parse(json, javaClass)
        }.isSuccess
    }

    fun <T> parseOrNull(json: String, javaClass: Class<out T>): T? {
        return runCatching {
            parse(json, javaClass)
        }.getOrNull()
    }

    val Any.asJson: String
        get() = mapper.writeValueAsString(this)
}
