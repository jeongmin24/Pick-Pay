package com.ssafy.payclient

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.messaging.FirebaseMessaging
import com.ssafy.payclient.data.model.FcmTokenRequest
import com.ssafy.payclient.data.local.TokenManager
import com.ssafy.payclient.data.network.RetrofitClient
import com.ssafy.payclient.data.repository.AuthRepository
import com.ssafy.payclient.databinding.ActivityMainBinding
import com.ssafy.payclient.fragment.PaymentFragment
import com.ssafy.payclient.ui.login.LoginActivity
import com.ssafy.payclient.util.UiState
import com.ssafy.payclient.util.ViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels {
        val tokenManager = TokenManager(applicationContext)
        val apiService = RetrofitClient.getApiService(tokenManager)
        val repository = AuthRepository(apiService)
        ViewModelFactory(repository, tokenManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }


        initViews()
        requestNotificationPermissionIfNeeded()
        registerFcmToken()
        handlePaymentDeepLink(intent)
        handleDutchPaymentIntent(intent)
//        observeViewModel()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePaymentDeepLink(intent)
        handleDutchPaymentIntent(intent)
    }

    private fun initViews() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)
        binding.bottomNavigation.itemIconTintList = null

    }

    private fun handlePaymentDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "tosspayments" || uri.host != "payment") return

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val navController = navHostFragment?.navController ?: return
        navController.currentBackStackEntry
            ?.savedStateHandle
            ?.set(PAYMENT_DEEP_LINK_URI, uri.toString())
    }

    private fun handleDutchPaymentIntent(intent: Intent?) {
        if (intent == null) return
        val isDutchPaymentRequest = intent.action == ACTION_GROUP_DUTCH_PAYMENT_REQUEST ||
            intent.getStringExtra(EXTRA_TYPE) == TYPE_GROUP_DUTCH_PAYMENT_REQUEST
        if (!isDutchPaymentRequest) return

        val orderId = intent.getStringExtra(EXTRA_ORDER_ID)
            ?: intent.getStringExtra("orderNo")
            ?: ""
        val totalPrice = intent.getLongExtra(EXTRA_TOTAL_PRICE, 0L)
            .takeIf { it > 0L }
            ?: intent.getStringExtra("amount")?.toLongOrNull()
            ?: 0L
        val groupId = intent.getLongExtra(EXTRA_GROUP_ID, -1L)
            .takeIf { it > 0L }
            ?: intent.getStringExtra("groupId")?.toLongOrNull()
            ?: -1L
        val orderName = intent.getStringExtra(EXTRA_ORDER_NAME) ?: "단체 주문 정산"

        if (orderId.isBlank() || totalPrice <= 0L || groupId <= 0L) {
            Toast.makeText(this, "정산 요청 정보를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val navController = navHostFragment?.navController ?: return
        navController.navigate(
            R.id.fragment_payment,
            Bundle().apply {
                putString(PaymentFragment.ARG_ORDER_ID, orderId)
                putLong(PaymentFragment.ARG_TOTAL_PRICE, totalPrice)
                putString(PaymentFragment.ARG_ORDER_NAME, orderName)
                putLong(PaymentFragment.ARG_GROUP_ID, groupId)
            }
        )
    }

    private fun registerFcmToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (token.isBlank()) return@addOnSuccessListener

                lifecycleScope.launch {
                    runCatching {
                        val tokenManager = TokenManager(applicationContext)
                        RetrofitClient.getApiService(tokenManager)
                            .updateFcmToken(FcmTokenRequest(token))
                    }
                }
            }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) return

        ActivityCompat.requestPermissions(
            this,
            arrayOf(permission),
            REQUEST_POST_NOTIFICATIONS
        )
    }

//    private fun observeViewModel() {
//        lifecycleScope.launch {
//            viewModel.logoutState.collect { state ->
//                when (state) {
//                    is UiState.Loading -> {
//                        binding.btnLogout.isEnabled = false
//                    }
//                    is UiState.Success -> {
//                        navigateToLogin()
//                    }
//                    is UiState.Error -> {
//                        binding.btnLogout.isEnabled = true
//                        Toast.makeText(this@MainActivity, state.message, Toast.LENGTH_SHORT).show()
//                        // 실패하더라도 로그아웃 처리를 하고 싶다면 여기서 navigateToLogin() 호출 가능
//                    }
//                    else -> {}
//                }
//            }
//        }
//    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    companion object {
        const val PAYMENT_DEEP_LINK_URI = "PAYMENT_DEEP_LINK_URI"
        const val ACTION_GROUP_DUTCH_PAYMENT_REQUEST = "com.ssafy.payclient.GROUP_DUTCH_PAYMENT_REQUEST"
        const val EXTRA_GROUP_ID = "GROUP_ID"
        const val EXTRA_ORDER_ID = "ORDER_ID"
        const val EXTRA_TOTAL_PRICE = "TOTAL_PRICE"
        const val EXTRA_ORDER_NAME = "ORDER_NAME"
        const val EXTRA_TYPE = "type"
        const val TYPE_GROUP_DUTCH_PAYMENT_REQUEST = "GROUP_DUTCH_PAYMENT_REQUEST"
        private const val REQUEST_POST_NOTIFICATIONS = 1001
    }
}
