package com.ssafy.payclient.ui.review

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Backend
import com.ssafy.payclient.data.model.ReviewResponseDTO
import com.ssafy.payclient.databinding.ActivityReviewDetailBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ReviewDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReviewDetailBinding
    private var engine: Engine? = null
    private var conversation: Conversation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val review = intent.getSerializableExtra("review_data") as ReviewResponseDTO

        review?.let {
            binding.tvNickname.text = it.nickname
            binding.detailCreatedAt.text = it.createdAt
            binding.rbRating.rating = it.rating.toFloat()
            binding.detailContent.text = it.content

            Glide.with(this).load(it.profileUrl).into(binding.ivProfile)
            Glide.with(this).load(it.imageUrl).into(binding.ivImageView)
        }

        initOnDeviceLLM()

        binding.btnAnalyzeMenu.setOnClickListener {
            val userPrompt = binding.etPrompt.text.toString().trim()
            if(userPrompt.isEmpty()) {
                Toast.makeText(this, "프롬프트를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            generateAiResponse(userPrompt)
        }

    }

    private fun initOnDeviceLLM() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val modelFile = File(filesDir, "gemma-4-E2B-it.litertlm")

                if (!modelFile.exists()) {
                    throw IllegalStateException(
                        "모델 파일이 없습니다: ${modelFile.absolutePath}"
                    )
                }

                val engineConfig = EngineConfig(
                    modelPath = modelFile.canonicalPath,
                    backend = Backend.CPU(),
                    cacheDir = cacheDir.absolutePath
                )

                engine = Engine(engineConfig)
                engine?.initialize()

                conversation = engine?.createConversation()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ReviewDetailActivity,
                        "온디바이스 AI 준비 완료",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ReviewDetailActivity,
                        "AI 로드 실패: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("싸피", "AI 로드 실패", e)
                }
            }
        }
    }

    private fun generateAiResponse(prompt: String) {
        binding.pbLoading.visibility = View.VISIBLE
        binding.tvAiResult.text = "AI가 연산 중입니다..."
        binding.btnAnalyzeMenu.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val conv = conversation ?: throw IllegalStateException("엔진 준비 중입니다.")

                val response = conv.sendMessage(prompt)

                withContext(Dispatchers.Main) {
                    binding.pbLoading.visibility = View.GONE
                    binding.btnAnalyzeMenu.isEnabled = true
                    binding.tvAiResult.text = response.toString()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.pbLoading.visibility = View.GONE
                    binding.btnAnalyzeMenu.isEnabled = true
                    binding.tvAiResult.text = "오류 발생: ${e.message}"
                    Log.e("싸피", "AI 응답 생성 실패", e)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        conversation?.close()
        engine?.close()
    }
}