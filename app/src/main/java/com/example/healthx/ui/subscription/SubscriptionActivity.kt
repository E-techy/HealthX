package com.example.healthx.ui.subscription

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthx.MainActivity
import com.example.healthx.data.local.SessionManager
import com.example.healthx.data.network.RetrofitClient // Assume you have a network module supplying the API
import com.example.healthx.data.network.SubscriptionPlan
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import org.json.JSONObject

class SubscriptionActivity : ComponentActivity(), PaymentResultWithDataListener {

    private val viewModel: SubscriptionViewModel by viewModels {
        SubscriptionViewModelFactory(
            RetrofitClient.subscriptionApi,
            SessionManager(this)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Preload Razorpay Checkout for faster loading
        Checkout.preload(applicationContext)

        val passedSubscriptionId = intent.getStringExtra("EXTRA_SUBSCRIPTION_ID")

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                SubscriptionScreenContainer(
                    viewModel = viewModel,
                    initialSubscriptionId = passedSubscriptionId,
                    onLaunchRazorpay = { orderId, amount, keyId, name, desc ->
                        startPayment(orderId, amount, keyId, name, desc)
                    }
                )
            }
        }
    }

    private fun startPayment(orderId: String, amount: Int, keyId: String, planName: String, desc: String) {
        val checkout = Checkout()
        checkout.setKeyID(keyId)

        try {
            val options = JSONObject()
            options.put("name", "HealthX")
            options.put("description", "$planName - $desc")
            options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.jpg") // Replace with your logo URL
            options.put("order_id", orderId)
            options.put("theme.color", "#3399cc")
            options.put("currency", "INR")
            options.put("amount", amount) // Amount in paise
            options.put("retry", JSONObject().apply { put("enabled", true); put("max_count", 4) })

            checkout.open(this, options)
        } catch (e: Exception) {
            Toast.makeText(this, "Error in payment: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // --- Razorpay Callbacks ---

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        if (paymentData != null) {
            viewModel.verifyPayment(
                orderId = paymentData.orderId,
                paymentId = paymentData.paymentId,
                signature = paymentData.signature
            )
        }
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        Toast.makeText(this, "Payment failed: $response", Toast.LENGTH_LONG).show()
        viewModel.resetState()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreenContainer(
    viewModel: SubscriptionViewModel,
    initialSubscriptionId: String?,
    onLaunchRazorpay: (String, Int, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var activeSubscriptionId by remember { mutableStateOf(initialSubscriptionId) }

    // Fetch initial data based on intent
    LaunchedEffect(activeSubscriptionId) {
        if (activeSubscriptionId != null && uiState is SubscriptionState.Idle) {
            viewModel.fetchPlanDetails(activeSubscriptionId!!)
        } else if (activeSubscriptionId == null && uiState is SubscriptionState.Idle) {
            viewModel.fetchAllPlans()
        }
    }

    // Trigger Razorpay when order is created
    LaunchedEffect(uiState) {
        if (uiState is SubscriptionState.OrderCreated) {
            val order = (uiState as SubscriptionState.OrderCreated).orderData
            onLaunchRazorpay(order.orderId, order.amount, order.keyId, order.planName, order.planDescription)
        }
    }

    BackHandler {
        if (activeSubscriptionId != null && initialSubscriptionId == null) {
            // Came from the list view originally, go back to list
            activeSubscriptionId = null
            viewModel.fetchAllPlans()
        } else {
            // Came directly from notification to a specific ID, or at top level list
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            (context as Activity).finish()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (activeSubscriptionId == null) "Choose a Plan" else "Plan Details") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (activeSubscriptionId != null && initialSubscriptionId == null) {
                            activeSubscriptionId = null
                            viewModel.fetchAllPlans()
                        } else {
                            val intent = Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                            (context as Activity).finish()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E), titleContentColor = Color.White)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize().background(Color(0xFF121212))) {
            when (uiState) {
                is SubscriptionState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                is SubscriptionState.Error -> {
                    val msg = (uiState as SubscriptionState.Error).message
                    Text(msg, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                }

                is SubscriptionState.PlansLoaded -> {
                    val plans = (uiState as SubscriptionState.PlansLoaded).plans
                    PlanList(plans) { selectedPlan ->
                        activeSubscriptionId = selectedPlan._id
                        viewModel.fetchPlanDetails(selectedPlan._id)
                    }
                }

                is SubscriptionState.SinglePlanLoaded -> {
                    val plan = (uiState as SubscriptionState.SinglePlanLoaded).plan
                    PlanDetail(plan) { viewModel.initiateCheckout(plan._id) }
                }

                is SubscriptionState.PaymentVerified -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = Color.Green, modifier = Modifier.size(72.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Subscription Activated Successfully!", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = {
                            context.startActivity(Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            })
                            (context as Activity).finish()
                        }) {
                            Text("Go to Dashboard")
                        }
                    }
                }
                else -> {} // Idle or OrderCreated (Handled by LaunchedEffect)
            }
        }
    }
}

@Composable
fun PlanList(plans: List<SubscriptionPlan>, onPlanSelected: (SubscriptionPlan) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(plans) { plan ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onPlanSelected(plan) },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(plan.name, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(plan.shortDescription, style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("₹${plan.price.toInt()} / ${plan.billingCycle}", style = MaterialTheme.typography.titleMedium, color = Color(0xFF3399cc))
                }
            }
        }
    }
}

@Composable
fun PlanDetail(plan: SubscriptionPlan, onSubscribeClicked: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(plan.name, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("₹${plan.price.toInt()} / ${plan.billingCycle}", style = MaterialTheme.typography.titleLarge, color = Color(0xFF3399cc))
        Spacer(modifier = Modifier.height(24.dp))
        Text(plan.detailedDescription, style = MaterialTheme.typography.bodyLarge, color = Color.LightGray)

        Spacer(modifier = Modifier.height(24.dp))
        Text("Features Included:", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        plan.features.forEach { feature ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(feature, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onSubscribeClicked,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3399cc))
        ) {
            Text("Subscribe Now", style = MaterialTheme.typography.titleMedium)
        }
    }
}