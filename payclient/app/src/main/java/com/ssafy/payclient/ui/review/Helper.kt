package com.ssafy.payclient.ui.review

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import kotlinx.coroutines.flow.Flow
import java.io.ByteArrayOutputStream
import java.io.File

object Helper {
    private const val TAG = "Helper_싸피"
    private const val MODEL_FILE_NAME = "gemma-4-E2B-it.litertlm"

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var fileName: String = ""

    /**
     * 온디바이스 LLM 엔진 및 세션 초기화
     */
    fun initialize(context: Context, onDone: (String) -> Unit) {
        Log.d(TAG, "LLM 초기화 시작...")

        if (conversation != null) {
            onDone("현재 사용 중 모델: $fileName (이미 초기화됨)")
            return
        }

        val modelFile = File(context.filesDir, MODEL_FILE_NAME)
        if (!modelFile.exists()) {
            Log.e(TAG, "엔진 파일이 존재하지 않습니다: ${modelFile.absolutePath}")
            onDone("현재 사용 중 모델: (엔진 파일이 존재하지 않습니다.)")
            return
        }

        runCatching {
            if (engine == null) {
                val config = EngineConfig(
                    modelPath = modelFile.path,
                    backend = Backend.CPU(),
                    visionBackend = Backend.CPU(),
                    audioBackend = Backend.CPU(),
                    cacheDir = context.cacheDir.absolutePath
                )

                engine = Engine(config).apply { initialize() }
            }

            if (conversation == null) {
                conversation = engine!!.createConversation()
            }

            fileName = MODEL_FILE_NAME
        }.onSuccess {
            onDone("현재 사용 중 모델: $fileName")
        }.onFailure {
            Log.e(TAG, "초기화 실패", it)
            onDone(it.message ?: "Unknown error")
        }
    }

    /**
     * 텍스트 프롬프트와 비트맵 이미지를 함께 받아 추론을 수행하는 Flow 반환
     */
    fun inferenceAsFlow(input: String, photo: Bitmap? = null): Flow<Message> {
        val currentConversation = conversation ?: error("Conversation이 초기화되지 않았습니다.")

        return if (photo != null) {
            currentConversation.sendMessageAsync(
                Contents.of(
                    Content.ImageBytes(bitmapToByteArray(photo)),
                    Content.Text(input)
                )
            )
        } else {
            currentConversation.sendMessageAsync(
                Contents.of(Content.Text(input))
            )
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return stream.toByteArray()
    }

    fun cleanUp(onDone: () -> Unit) {
        runCatching { conversation?.close() }
        runCatching { engine?.close() }
        conversation = null
        engine = null
        onDone()
        Log.d(TAG, "LLM 자원 정리 완료")
    }
}