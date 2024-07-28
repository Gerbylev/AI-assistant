package cc.services.connectors

import cc.services.CailaClient
import cc.services.parseModelId
import com.mlp.api.TypeInfo
import com.mlp.api.datatypes.chatgpt.ChatCompletionRequest
import com.mlp.api.datatypes.chatgpt.ChatCompletionResult
import com.mlp.api.datatypes.chatgpt.ChatMessage
import com.mlp.api.datatypes.chatgpt.ChatRole
import com.mlp.gate.ClientResponseProto
import com.mlp.sdk.Payload
import com.mlp.sdk.utils.JSON
import kotlinx.coroutines.flow.Flow
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

@Service
class GPTConnector(
    val caila: CailaClient,
    @Value("\${caila.defaultModel.model}") val model: String,
    @Value("\${caila.defaultModel.systemPrompt}") val prompt: String?,
) {

    suspend fun gpt(query: Collection<ChatMessage>, collector: (String?) -> Unit): Flow<ClientResponseProto> {
        val modelId = model.parseModelId()
        val req = buildRequest(query, modelId.model, prompt)
        val flow = caila.grpcClient.predictStream(
            modelId.account,
            modelId.service,
            buildPayload(req),
            caila.defaultModel.predictConfig?.let { Payload(it.toString()) },
            authToken = caila.grpcClient.config.clientToken!!
        )
        val buffer = StringBuilder()
        var pointer = 0

        flow.collect {
            val r = JSON.parse(it.partialPredict.data.json, ChatCompletionResult::class.java)
            val f = it.partialPredict.finish
            val t = r.choices.getOrNull(0)?.delta?.content

            if (t != null) {
                buffer.append(t)
            }
            val i = buffer.indexOfAny(listOf(".", "!", "?", ":"), pointer)
            if (i != -1) {
                val t2 = buffer.substring(pointer, i + 1)
                pointer = i + 1
                collector(t2)
            }

            if (f) {
                collector(null)
            }
        }
        return flow
    }

    private fun buildRequest(query: Collection<ChatMessage>, modelId: String?, systemPrompt: String?): ChatCompletionRequest {
        val sp = if (!systemPrompt.isNullOrEmpty()) listOf(
            ChatMessage(ChatRole.system, systemPrompt))
            else emptyList()

        return ChatCompletionRequest(
            model = modelId,
            messages = sp + query,
            stream = true
        )
    }

}

fun buildPayload(req: Any): Payload {
    val json = JSON.stringify(req)
    return Payload(TypeInfo.canonicalName(req.javaClass), json)
}
