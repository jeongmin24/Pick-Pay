package com.ssafy.payclient

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.app.PendingIntent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
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
import com.ssafy.payclient.ui.menu.MenuViewModel // 추가
import com.ssafy.payclient.ui.menu.MenuViewModelFactory
import com.ssafy.payclient.util.ViewModelFactory
import kotlinx.coroutines.launch
import java.nio.charset.Charset

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val sharedTokenManager by lazy { TokenManager(applicationContext) }

    private val menuViewModel: MenuViewModel by viewModels {
        MenuViewModelFactory(sharedTokenManager)
    }

    // NFC 관련 변수
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var nfcPendingIntent: PendingIntent

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

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        nfcPendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        initViews()
        requestNotificationPermissionIfNeeded()
        registerFcmToken()
        handlePaymentDeepLink(intent)
        handleDutchPaymentIntent(intent)
        handlePaymentDeepLink(getIntent())
        handleGoToCartIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, nfcPendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePaymentDeepLink(intent)
        handleDutchPaymentIntent(intent)
        handleMenuNfcIntent(intent)
        handleGoToCartIntent(intent)
    }

    private fun initViews() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)
    }

    private fun handleMenuNfcIntent(intent: Intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {

            val menuId = parseNfcIntent(intent)
            if (menuId != null) {
                menuViewModel.onMenuNfcScanned(menuId) { success ->
                    if (success) {
                        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
                        val navController = navHostFragment?.navController

                        navController?.navigate(R.id.fragment_cart)
                    } else {
                        Toast.makeText(this, "NFC 상품 장바구니 추가 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun parseNfcIntent(intent: Intent): Long? {
        val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES) ?: return null
        try {
            val record = (rawMsgs[0] as NdefMessage).records[0]
            val payload = record.payload

            val isTextRecord = record.tnf == android.nfc.NdefRecord.TNF_WELL_KNOWN
                    && java.util.Arrays.equals(record.type, android.nfc.NdefRecord.RTD_TEXT)

            val resultString = if (isTextRecord) {
                val languageCodeLength = (payload[0].toInt() and 0x003F)
                String(payload, languageCodeLength + 1, payload.size - languageCodeLength - 1, Charset.forName("UTF-8"))
            } else {
                String(payload, Charset.forName("UTF-8"))
            }

            return resultString.trim().toLongOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
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
        val groupId = intent.getStringExtra(EXTRA_GROUP_ID)
            ?: intent.getStringExtra("groupId")
            ?: ""
        val orderName = intent.getStringExtra(EXTRA_ORDER_NAME) ?: "단체 주문 정산"

        if (orderId.isBlank() || totalPrice <= 0L || groupId.isBlank()) {
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
                putString(PaymentFragment.ARG_GROUP_ID, groupId)
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

    private fun handleGoToCartIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("GO_TO_CART", false) == true) {
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
            val navController = navHostFragment?.navController

            navController?.navigate(R.id.fragment_cart)

            intent.removeExtra("GO_TO_CART")
        }

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

//    private fun navigateToLogin() {
//        val intent = Intent(this, LoginActivity::class.java)
//        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        startActivity(intent)
//        finish()
//    }

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
