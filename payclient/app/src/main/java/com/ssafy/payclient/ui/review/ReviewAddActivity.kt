package com.ssafy.payclient.ui.review

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.ssafy.payclient.MainViewModel
import com.ssafy.payclient.R
import com.ssafy.payclient.data.api.ReviewApiService
import com.ssafy.payclient.data.local.TokenManager
import com.ssafy.payclient.data.network.RetrofitClient
import com.ssafy.payclient.databinding.ActivityReviewAddBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ReviewAddActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReviewAddBinding
    private var selectedImageUri: Uri? = null

    private lateinit var reviewApiService: ReviewApiService

    private val pickMediaLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            binding.layoutPhotoGuide.visibility = View.GONE
            Glide.with(this).load(uri).into(binding.ivReviewPhoto)
        } else {
            Toast.makeText(this, "사진 선택이 취소되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openGallery() // 권한 수락 시 바로 갤러리 오픈!
        } else {
            Toast.makeText(this, "권한을 허용해야 사진을 첨부할 수 있습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewAddBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initApiService()
        initEvent()
    }

    private fun initApiService() {
        val tokenManager = TokenManager(this)
        reviewApiService = RetrofitClient.getReviewApiService(tokenManager)
    }

    private fun initEvent() {
        binding.cvSelectImage.setOnClickListener {
            checkPhotoPermission()
        }

        binding.btnSubmitReview.setOnClickListener {
            val content = binding.etReviewContent.text.toString().trim()
            val rating = binding.rbInputRating.rating.toInt()
            val imageUrl = selectedImageUri?.toString() ?: ""

            if (content.length < 10) {
                Toast.makeText(this, "리뷰 내용을 최소 10자 이상 작성해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (rating == 0) {
                Toast.makeText(this, "만족도 별점을 선택해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendReviewToServer(content, rating)
        }
    }
    private fun checkPhotoPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    private fun openGallery() {
        pickMediaLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun sendReviewToServer(content: String, rating: Int) {
        lifecycleScope.launch {
            try {
                val contentBody = content
                val ratingBody = rating.toString()

                val imagePart = withContext(Dispatchers.IO) {
                    selectedImageUri?.let { uri -> prepareMultipartPart(uri) }
                }

                val newReviewId = withContext(Dispatchers.IO) {
                    reviewApiService.createReview(contentBody, ratingBody, imagePart)
                }

                if (newReviewId > 0) {
                    Toast.makeText(this@ReviewAddActivity, "리뷰가 정상적으로 등록되었습니다! (No.$newReviewId)", Toast.LENGTH_SHORT).show()
                    finish() // 현재 액티비티를 닫고 마이페이지/메뉴 화면으로 복귀
                } else {
                    Toast.makeText(this@ReviewAddActivity, "리뷰 등록에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ReviewAddActivity, "서버 연결에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun prepareMultipartPart(fileUri: Uri): MultipartBody.Part? {
        val contentResolver = contentResolver

        val fileName = contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        } ?: "review_image.jpg"

        val inputStream: InputStream? = contentResolver.openInputStream(fileUri)
        val tempFile = File(cacheDir, fileName)

        inputStream?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output) // 파일 데이터 싹 복사
            }
        }

        val mediaType = contentResolver.getType(fileUri)?.toMediaTypeOrNull()
        val requestFile = RequestBody.create(mediaType, tempFile)
        return MultipartBody.Part.createFormData("image", tempFile.name, requestFile)
    }

}