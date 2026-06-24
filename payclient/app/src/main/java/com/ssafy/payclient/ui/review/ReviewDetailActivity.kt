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
import com.ssafy.payclient.data.model.ReviewResponseDTO
import com.ssafy.payclient.databinding.ActivityReviewDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReviewDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReviewDetailBinding
    private var detectedMenu: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewDetailBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomPadding = if (ime.bottom > 0) ime.bottom else systemBars.bottom
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding)
            WindowInsetsCompat.CONSUMED
        }

        // 초기 상태 설정: 질문 영역 숨기기
        binding.llAiSection.visibility = View.GONE
        binding.ivBack.setOnClickListener { finish() }

        val review = intent.getSerializableExtra("review_data") as? ReviewResponseDTO
        review?.let {
            binding.tvNickname.text = it.nickname
            binding.detailCreatedAt.text = if (it.createdAt.length >= 10) it.createdAt.substring(0, 10) else it.createdAt
            binding.rbRating.rating = it.rating.toFloat()
            binding.detailContent.text = it.content
            Glide.with(this).load(it.profileUrl).into(binding.ivProfile)
            Glide.with(this).load(it.imageUrl).into(binding.ivImageView)
        }

        Helper.initialize(this) { status ->
            runOnUiThread {
                Log.d("싸피", "AI Helper Status: $status")
            }
        }

        // 1단계: 메뉴 분석 버튼
        binding.btnAnalyzeMenu.setOnClickListener {
            analyzeMenuOnly()
        }

        // 2단계: 질문하기 버튼
        binding.btnAskAi.setOnClickListener {
            val userQuestion = binding.etPrompt.text.toString().trim()
            if (userQuestion.isEmpty()) {
                Toast.makeText(this, "궁금한 점을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            askAboutMenu(userQuestion)
        }
    }

    // [1단계] 이미지에서 메뉴 이름만 추출
    private fun analyzeMenuOnly() {
        binding.pbLoading.visibility = View.VISIBLE
        binding.btnAnalyzeMenu.isEnabled = false
        val imageBitmap = getBitmapFromImageView()

        val menuAnalysisPrompt = """
            [역할]
            너는 카페 키오스크 및 주문 관리 시스템의 이미지 인식 AI이다.
            
            [작업]
            제공된 사진(이미지)을 분석하여 다음 [메뉴 목록] 중 어떤 메뉴들이 포함되어 있는지 정확하게 찾아내라. 사진 속에 메뉴가 2개 이상 존재할 수 있다.
            
            [메뉴 목록]
            - 아이스커피
            - 아이스카페모카
            - 아이스화이트초콜릿모카
            - 아포가토
            - 딸기요거트블렌디드
            - 아이스밀크티
            - 복숭아아이스티
            - 아이스얼그레이티
            - 아이스히비스커스티
            - 소금빵
            - 플레인베이글
            - 블루베리베이글
            - 생크림카스테라
            - 생크림롤
            - 초콜릿케이크
            - 초콜릿쿠키
            
            [출력 규칙 - 필수 지시사항]
            1. 오직 위 [메뉴 목록]에 존재하는 이름으로만 답변해야 한다.
            2. 부연 설명, 마침표(.), 앞뒤 따옴표 등을 절대 포함하지 마라.
            3. 사진에 감지된 모든 메뉴 이름을 출력하되, 2개 이상일 경우 쉼표(,)로만 구분하여 한 줄로 출력해라.
               - 예시 (1개일 때): 아이스커피
               - 예시 (2개 이상일 때): 아이스커피,소금빵,초콜릿케이크
        """.trimIndent()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                var result = ""
                Helper.inferenceAsFlow(input = menuAnalysisPrompt, photo = imageBitmap).collect { message ->
                    result += message.toString()
                }

                withContext(Dispatchers.Main) {
                    detectedMenu = result.trim()
                    binding.tvAiResult.text = detectedMenu
                    binding.pbLoading.visibility = View.GONE
                    // 메뉴 분석 성공 시 질문 영역 노출
                    binding.llAiSection.visibility = View.VISIBLE
                    binding.btnAnalyzeMenu.text = "메뉴 분석 완료"
                }
            } catch (e: Exception) {
                handleAiError(e)
            }
        }
    }

    // [2단계] 분석된 메뉴를 바탕으로 사용자 질문에 답변
    private fun askAboutMenu(question: String) {
        binding.pbLoading.visibility = View.VISIBLE
        binding.btnAskAi.isEnabled = false

        // 카페 전체 메뉴 리스트 (추천 등을 위해 AI에게 제공)
        val allMenuList = "아이스커피, 아이스카페모카, 아이스화이트초콜릿모카, 아포가토, 딸기요거트블렌디드, 아이스밀크티, 복숭아아이스티, 아이스얼그레이티, 아이스히비스커스티, 소금빵, 플레인베이글, 블루베리베이글, 생크림카스테라, 생크림롤, 초콜릿케이크, 초콜릿쿠키"

        val qaPrompt = """
            [데이터]
            - 분석된 메뉴: $detectedMenu
            - 카페 전체 메뉴: $allMenuList
            
            [상황] 사용자가 위 정보를 바탕으로 질문함: "$question"
            
            [답변 규칙]
            1. 1~2줄 내외로 아주 짧게 답변할 것.
            2. '**' 같은 강조 기호나 특수 기호를 절대 사용하지 말 것.
            3. 분석된 메뉴($detectedMenu)나 전체 메뉴 중 관련 있는 것을 언급하며 친절하게 답할 것.
        """.trimIndent()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val responseBuilder = StringBuilder()
                // 2단계에서는 이미지를 분석하지 않으므로 photo = null 전달
                Helper.inferenceAsFlow(input = qaPrompt, photo = null).collect { message ->
                    withContext(Dispatchers.Main) {
                        responseBuilder.append(message.toString())
                        // 실시간으로 텍스트를 업데이트하며 특수 기호 제거
                        val cleanText = responseBuilder.toString().replace("**", "").replace("*", "")
                        binding.tvAiResult.text = cleanText
                    }
                }

                withContext(Dispatchers.Main) {
                    binding.pbLoading.visibility = View.GONE
                    binding.btnAskAi.isEnabled = true
                    binding.etPrompt.text?.clear() // 질문 후 입력창 비우기
                }
            } catch (e: Exception) {
                handleAiError(e)
            }
        }
    }

    private fun handleAiError(e: Exception) {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.pbLoading.visibility = View.GONE
            binding.btnAnalyzeMenu.isEnabled = true
            binding.btnAskAi.isEnabled = true
            binding.tvAiResult.text = "오류 발생: ${e.message}"
            Log.e("싸피", "AI Error", e)
        }
    }

    private fun getBitmapFromImageView(): Bitmap? {
        val drawable = binding.ivImageView.drawable
        return if (drawable is BitmapDrawable) drawable.bitmap else null
    }

    override fun onDestroy() {
        super.onDestroy()
        Helper.cleanUp { Log.d("싸피", "AI 자원 반환") }
    }
}
