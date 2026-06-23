package com.ssafy.payclient.data.messaging

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ssafy.payclient.MainActivity
import com.ssafy.payclient.R
import com.ssafy.payclient.data.local.TokenManager
import com.ssafy.payclient.data.model.FcmTokenRequest
import com.ssafy.payclient.data.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PickPayFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data["type"] != TYPE_GROUP_DUTCH_PAYMENT_REQUEST) return

        showDutchPaymentNotification(message.data)
    }

    override fun onNewToken(token: String) {
        registerFcmToken(token)
    }

    private fun registerFcmToken(token: String) {
        val tokenManager = TokenManager(applicationContext)
        if (tokenManager.accessToken.isNullOrBlank()) return

        serviceScope.launch {
            runCatching {
                RetrofitClient.getApiService(tokenManager)
                    .updateFcmToken(FcmTokenRequest(token))
            }
        }
    }

    private fun showDutchPaymentNotification(data: Map<String, String>) {
        if (!canPostNotifications()) return

        val orderNo = data["orderNo"].orEmpty()
        val groupId = data["groupId"]?.toLongOrNull() ?: -1L
        val amount = data["amount"]?.toLongOrNull() ?: 0L

        if (orderNo.isBlank() || groupId <= 0L || amount <= 0L) return

        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_GROUP_DUTCH_PAYMENT_REQUEST
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_TYPE, TYPE_GROUP_DUTCH_PAYMENT_REQUEST)
            putExtra(MainActivity.EXTRA_GROUP_ID, groupId)
            putExtra(MainActivity.EXTRA_ORDER_ID, orderNo)
            putExtra(MainActivity.EXTRA_TOTAL_PRICE, amount)
            putExtra(MainActivity.EXTRA_ORDER_NAME, "단체 주문 정산")
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            orderNo.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.outline_shopping_cart_24)
            .setContentTitle("정산 요청")
            .setContentText("단체 주문 정산을 진행해주세요.")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(orderNo.hashCode(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "정산 요청",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val CHANNEL_ID = "group_dutch_payment"
        private const val TYPE_GROUP_DUTCH_PAYMENT_REQUEST = "GROUP_DUTCH_PAYMENT_REQUEST"
    }
}
