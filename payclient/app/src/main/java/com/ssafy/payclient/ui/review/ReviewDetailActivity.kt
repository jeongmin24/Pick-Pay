package com.ssafy.payclient.ui.review

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
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

        val review = intent.getSerializableExtra("review_data") as? ReviewResponseDTO
        review?.let {
            binding.tvNickname.text = it.nickname
            binding.detailCreatedAt.text = it.createdAt
            binding.rbRating.rating = it.rating.toFloat()
            binding.detailContent.text = it.content

            Glide.with(this).load(it.profileUrl).into(binding.ivProfile)
            Glide.with(this).load(it.imageUrl).into(binding.ivImageView)
        }

        Helper.initialize(this) { status ->
            runOnUiThread {
                Toast.makeText(this@ReviewDetailActivity, status, Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAnalyzeMenu.setOnClickListener {
            val userPrompt = binding.etPrompt.text.toString().trim()
            if (userPrompt.isEmpty()) {
                Toast.makeText(this, "프롬프트를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            generateAiResponse(userPrompt)
        }

    }

    private fun getBitmapFromImageView(): Bitmap? {
        val drawable = binding.ivImageView.drawable
        return if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            null
        }
    }

    private fun generateAiResponse(prompt: String) {
        binding.pbLoading.visibility = View.VISIBLE
        binding.tvAiResult.text = "AI가 이미지를 분석하는 중입니다..."
        binding.btnAnalyzeMenu.isEnabled = false

        val imageBitmap = getBitmapFromImageView()

        val systemInstruction = """
            
            [제약 조건]
            1. 답변은 반드시 3줄 이내로 핵심만 명확하게 작성하세요.
            2. 문장 스타일링을 위한 기호는 절대 사용하지 마세요. (예: '**' 금지, 대신 일반 텍스트 사용)
            3. 이미지와 관련된 내용 위주로 자연스럽게 설명하세요.
        """.trimIndent()

        val finalPrompt = "$prompt\n$systemInstruction"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val responseBuilder = StringBuilder()

                // Helper의 Flow를 수집(collect)하여 결과 스트리밍 받아오기
                Helper.inferenceAsFlow(input = finalPrompt, photo = imageBitmap).collect { message ->
                    withContext(Dispatchers.Main) {
                        responseBuilder.append(message.toString())
                        binding.tvAiResult.text = responseBuilder.toString()
                    }
                }

                withContext(Dispatchers.Main) {
                    binding.pbLoading.visibility = View.GONE
                    binding.btnAnalyzeMenu.isEnabled = true
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.pbLoading.visibility = View.GONE
                    binding.btnAnalyzeMenu.isEnabled = true
                    binding.tvAiResult.text = "오류 발생: ${e.message}"
                    Log.e("싸피", "AI 연산 도중 에러 발생", e)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 메모리 누수 방지를 위한 자원 해제 (만약 다른 화면에서도 앱 실행 중 계속 유지하고 싶다면 Application 레벨에서 해제해도 됨)
        Helper.cleanUp {
            Log.d("싸피", "ReviewDetailActivity Destroy - AI 자원 반환")
        }
    }
}