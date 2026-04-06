package com.example.sharkfin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale

data class MarketAsset(
    val symbol: String,
    val name: String,
    val price: Double = 0.0,
    val change: Double = 0.0,
    val isForex: Boolean = false,
    val details: GlobalQuoteResponse.GlobalQuote? = null,
    val forexDetails: ForexResponse.ExchangeRate? = null,
    val isLoading: Boolean = false
)

@Composable
fun StockForexScreen(
    uid: String,
    db: FirebaseFirestore,
    portfolio: List<PortfolioAsset>
) {
    val apiKey = "SCA27TYZQC1JJ5A0"
    var showTutorial by remember { mutableStateOf(true) }
    var selectedAsset by remember { mutableStateOf<MarketAsset?>(null) }
    var showAddToPortfolio by remember { mutableStateOf<MarketAsset?>(null) }
    
    var assets by remember { mutableStateOf(listOf(
        MarketAsset("AAPL", "Apple Inc."),
        MarketAsset("MSFT", "Microsoft"),
        MarketAsset("GOOGL", "Alphabet Inc."),
        MarketAsset("TSLA", "Tesla, Inc."),
        MarketAsset("NVDA", "NVIDIA Corp."),
        MarketAsset("BTC", "Bitcoin"),
        MarketAsset("ETH", "Ethereum"),
        MarketAsset("EUR", "Euro / USD", isForex = true),
        MarketAsset("GBP", "Pound / USD", isForex = true),
        MarketAsset("JPY", "Yen / USD", isForex = true)
    ))}

    val scope = rememberCoroutineScope()

    fun refreshAsset(asset: MarketAsset) {
        scope.launch {
            assets = assets.map { if (it.symbol == asset.symbol) it.copy(isLoading = true) else it }
            try {
                if (asset.isForex) {
                    val response = MarketRetrofit.api.getForexRate(fromCurrency = asset.symbol, toCurrency = "USD", apiKey = apiKey)
                    response.rate?.let { rate ->
                        assets = assets.map { 
                            if (it.symbol == asset.symbol) it.copy(
                                price = rate.price.toDoubleOrNull() ?: it.price,
                                forexDetails = rate,
                                isLoading = false
                            ) else it 
                        }
                    }
                } else {
                    val symbol = if (asset.symbol == "BTC" || asset.symbol == "ETH") "${asset.symbol}USD" else asset.symbol
                    val response = MarketRetrofit.api.getQuote(symbol = symbol, apiKey = apiKey)
                    response.lastQuote?.let { quote ->
                        assets = assets.map { 
                            if (it.symbol == asset.symbol) it.copy(
                                price = quote.price.toDoubleOrNull() ?: it.price,
                                change = quote.changePercent.replace("%", "").toDoubleOrNull() ?: it.change,
                                details = quote,
                                isLoading = false
                            ) else it 
                        }
                    }
                }
            } catch (e: Exception) {
                assets = assets.map { if (it.symbol == asset.symbol) it.copy(isLoading = false) else it }
            }
        }
    }

    LaunchedEffect(Unit) {
        assets.forEach { refreshAsset(it) }
        while(true) {
            delay(300000) // Poll every 5 mins to stay within free tier limits
            assets.forEach { refreshAsset(it) }
        }
    }

    // Calculate Portfolio Performance
    val portfolioTotalValue = portfolio.sumOf { asset ->
        val currentMarketPrice = assets.find { it.symbol == asset.symbol }?.price ?: asset.averageCost
        asset.quantity * currentMarketPrice
    }
    val portfolioTotalCost = portfolio.sumOf { it.quantity * it.averageCost }
    val portfolioPnL = if (portfolioTotalCost > 0) ((portfolioTotalValue - portfolioTotalCost) / portfolioTotalCost) * 100 else 0.0

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(56.dp))
            Text("Market Intelligence", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Real-time terminal powered by Alpha Vantage", color = SharkMuted, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(16.dp))

            // Benchmark Performance Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 24f, alpha = 0.1f)
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Analytics, null, tint = SharkAmber, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Benchmark Comparison", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        BenchmarkItem("S&P 500 (SPY)", "+1.24%", SharkGreen)
                        BenchmarkItem("Nasdaq (QQQ)", "+0.85%", SharkGreen)
                        BenchmarkItem("Shark Portfolio", "${if(portfolioPnL >= 0) "+" else ""}${String.format("%.2f", portfolioPnL)}%", if(portfolioPnL >= 0) SharkNavy else SharkRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(assets) { asset ->
                    AssetCard(
                        asset = asset,
                        isSelected = selectedAsset?.symbol == asset.symbol,
                        onClick = { 
                            selectedAsset = if (selectedAsset?.symbol == asset.symbol) null else asset 
                        },
                        onRefresh = { refreshAsset(asset) },
                        onAddToPortfolio = { showAddToPortfolio = asset }
                    )
                }
                item { Spacer(modifier = Modifier.height(120.dp)) }
            }
        }

        if (showAddToPortfolio != null) {
            AddToPortfolioSheet(
                asset = showAddToPortfolio!!,
                uid = uid,
                db = db,
                onDismiss = { showAddToPortfolio = null }
            )
        }

        if (showTutorial) {
            FeatureTutorialOverlay(
                title = "Market Terminal",
                description = "Tap any asset to reveal deep fundamentals including volume, previous close, and intraday range. Use the refresh icon to pull the latest spot prices.",
                onDismiss = { showTutorial = false }
            )
        }
    }
}

@Composable
fun BenchmarkItem(label: String, value: String, color: Color) {
    Column {
        Text(label, color = SharkMuted, fontSize = 10.sp)
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AssetCard(
    asset: MarketAsset,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRefresh: () -> Unit,
    onAddToPortfolio: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 24f, alpha = if (isSelected) 0.12f else 0.06f)
            .clickable { onClick() }
            .padding(16.dp)
            .animateContentSize()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (asset.change >= 0) SharkGreen.copy(alpha = 0.1f) else Color(0xFFef4444).copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (asset.isForex) Icons.Default.CurrencyExchange else if (asset.symbol == "BTC" || asset.symbol == "ETH") Icons.Default.CurrencyBitcoin else Icons.Default.ShowChart,
                    null,
                    tint = if (asset.change >= 0) SharkGreen else Color(0xFFef4444),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(asset.symbol, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(asset.name, color = SharkMuted, fontSize = 12.sp)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                if (asset.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SharkGreen, strokeWidth = 2.dp)
                } else {
                    Text(
                        "\$${String.format(if(asset.price < 10) "%.4f" else "%,.2f", asset.price)}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if(asset.change >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            null,
                            tint = if(asset.change >= 0) SharkGreen else Color(0xFFef4444),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${String.format("%.2f", asset.change)}%",
                            color = if(asset.change >= 0) SharkGreen else Color(0xFFef4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        if (isSelected) {
            Spacer(modifier = Modifier.height(20.dp))
            Divider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))
            
            if (asset.isForex && asset.forexDetails != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MarketDetailItem("Bid", asset.forexDetails.bid)
                    MarketDetailItem("Ask", asset.forexDetails.ask)
                    MarketDetailItem("From", asset.forexDetails.fromCode)
                }
            } else if (asset.details != null) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MarketDetailItem("Open", "\$${asset.details.open}")
                        MarketDetailItem("Prev Close", "\$${asset.details.previousClose}")
                        MarketDetailItem("Volume", formatVolume(asset.details.volume))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MarketDetailItem("Day High", "\$${asset.details.high}")
                        MarketDetailItem("Day Low", "\$${asset.details.low}")
                        MarketDetailItem("Change", asset.details.change)
                    }
                }
            } else {
                Text("Fundamentals loading...", color = SharkMuted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onRefresh,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, tint = SharkMuted, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh", color = Color.White, fontSize = 12.sp)
                }
                
                Button(
                    onClick = onAddToPortfolio,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = SharkNavy.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = SharkNavy, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add to Portfolio", color = SharkNavy, fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPortfolioSheet(
    asset: MarketAsset,
    uid: String,
    db: FirebaseFirestore,
    onDismiss: () -> Unit
) {
    var quantity by remember { mutableStateOf("") }
    var costBasis by remember { mutableStateOf(asset.price.toString()) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SharkCard,
        contentColor = Color.White,
        scrimColor = SharkBlack.copy(alpha = 0.8f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Add ${asset.symbol} to Portfolio", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            
            SheetInputField(
                value = quantity,
                onValueChange = { quantity = it },
                label = "Quantity",
                placeholder = "0.00",
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            SheetInputField(
                value = costBasis,
                onValueChange = { costBasis = it },
                label = "Average Cost Basis",
                placeholder = "0.00",
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    val q = quantity.toDoubleOrNull() ?: 0.0
                    val c = costBasis.toDoubleOrNull() ?: 0.0
                    if (q > 0) {
                        val portfolioAsset = hashMapOf(
                            "symbol" to asset.symbol,
                            "quantity" to q,
                            "averageCost" to c,
                            "assetType" to (if(asset.isForex) "FOREX" else "STOCK"),
                            "createdAt" to Date()
                        )
                        db.collection("users").document(uid).collection("portfolio").add(portfolioAsset)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SharkNavy),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Confirm Purchase", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MarketDetailItem(label: String, value: String) {
    Column {
        Text(label, color = SharkMuted, fontSize = 10.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

fun formatVolume(vol: String): String {
    val v = vol.toLongOrNull() ?: return vol
    return when {
        v >= 1_000_000_000 -> String.format("%.1fB", v / 1_000_000_000.0)
        v >= 1_000_000 -> String.format("%.1fM", v / 1_000_000.0)
        v >= 1_000 -> String.format("%.1fK", v / 1_000.0)
        else -> vol
    }
}
