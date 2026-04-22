package com.example.sharkfin

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- UI Models ---

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

// --- Screen Component ---

@Composable
fun StockForexScreen(
    uid: String,
    db: FirebaseFirestore,
    portfolio: List<PortfolioAsset>
) {
    val apiKey = "SCDE285T91MJQSHQ"
    var showTutorial by remember { mutableStateOf(true) }
    var selectedAsset by remember { mutableStateOf<MarketAsset?>(null) }
    var showAddToPortfolio by remember { mutableStateOf<MarketAsset?>(null) }

    var assets by remember { mutableStateOf(listOf(
        MarketAsset("AAPL", "Apple Inc."),
        MarketAsset("MSFT", "Microsoft"),
        MarketAsset("GOOGL", "Alphabet Inc."),
        MarketAsset("TSLA", "Tesla, Inc."),
        MarketAsset("NVDA", "NVIDIA Corp."),
        MarketAsset("BTC", "Bitcoin", isForex = true),
        MarketAsset("ETH", "Ethereum", isForex = true),
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
                    if (response.rate != null) {
                        assets = assets.map {
                            if (it.symbol == asset.symbol) it.copy(
                                price = response.rate.price.toDoubleOrNull() ?: it.price,
                                forexDetails = response.rate,
                                isLoading = false
                            ) else it
                        }
                    } else {
                        assets = assets.map { if (it.symbol == asset.symbol) it.copy(isLoading = false) else it }
                    }
                } else {
                    val response = MarketRetrofit.api.getQuote(symbol = asset.symbol, apiKey = apiKey)
                    if (response.lastQuote != null) {
                        val cleanPercent = response.lastQuote.changePercent.replace("%", "").toDoubleOrNull() ?: 0.0
                        assets = assets.map {
                            if (it.symbol == asset.symbol) it.copy(
                                price = response.lastQuote.price.toDoubleOrNull() ?: it.price,
                                change = cleanPercent,
                                details = response.lastQuote,
                                isLoading = false
                            ) else it
                        }
                    } else {
                        assets = assets.map { if (it.symbol == asset.symbol) it.copy(isLoading = false) else it }
                    }
                }
            } catch (e: Exception) {
                assets = assets.map { if (it.symbol == asset.symbol) it.copy(isLoading = false) else it }
            }
        }
    }

    LaunchedEffect(Unit) {
        assets.forEachIndexed { index, asset ->
            delay(index * 13000L)
            refreshAsset(asset)
        }
    }

    val portfolioMetrics = remember(portfolio, assets) {
        val totalValue = portfolio.sumOf { it.quantity * (assets.find { a -> a.symbol == it.symbol }?.price ?: it.averageCost) }
        val totalCost = portfolio.sumOf { it.quantity * it.averageCost }
        val pnl = if (totalCost > 0) ((totalValue - totalCost) / totalCost) * 100 else 0.0
        
        object {
            val totalValue = totalValue
            val totalCost = totalCost
            val pnl = pnl
        }
    }
    
    val portfolioPnL = portfolioMetrics.pnl

    Box(modifier = Modifier.fillMaxSize().background(SharkBase)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(56.dp))
            Text("Market Intelligence", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Real-time terminal powered by Alpha Vantage", color = SharkSecondary, fontSize = 13.sp)

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
                        BenchmarkItem("S&P 500 (SPY)", "+1.24%", SharkGreenMid)
                        BenchmarkItem("Nasdaq (QQQ)", "+0.85%", SharkGreenMid)
                        BenchmarkItem("Shark Portfolio", "${if(portfolioPnL >= 0) "+" else ""}${String.format("%.2f", portfolioPnL)}%", if(portfolioPnL >= 0) SharkGold else SharkRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(assets) { asset ->
                    AssetCard(
                        asset = asset,
                        isSelected = selectedAsset?.symbol == asset.symbol,
                        onClick = { selectedAsset = if (selectedAsset?.symbol == asset.symbol) null else asset },
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
        Text(label, color = SharkSecondary, fontSize = 10.sp)
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
                        if (asset.change >= 0) SharkGreenMid.copy(alpha = 0.1f) else SharkRed.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (asset.isForex) Icons.Default.CurrencyExchange else Icons.Default.ShowChart,
                    null,
                    tint = if (asset.change >= 0) SharkGreenMid else SharkRed,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(asset.symbol, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(asset.name, color = SharkSecondary, fontSize = 12.sp)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                if (asset.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SharkGold, strokeWidth = 2.dp)
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
                            tint = if(asset.change >= 0) SharkGreenMid else SharkRed,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${String.format("%.2f", asset.change)}%",
                            color = if(asset.change >= 0) SharkGreenMid else SharkRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        if (isSelected) {
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
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
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onRefresh,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, tint = SharkSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh", color = Color.White, fontSize = 12.sp)
                }
                
                Button(
                    onClick = onAddToPortfolio,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = SharkGold.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = SharkGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add to Portfolio", color = SharkGold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun MarketDetailItem(label: String, value: String) {
    Column {
        Text(label, color = SharkSecondary, fontSize = 10.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
    var avgCost by remember { mutableStateOf(asset.price.toString()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SharkSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SharkCardBorder) }
    ) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
            Text("Add to Portfolio", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(asset.name, color = SharkSecondary, fontSize = 14.sp)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            SheetInputField(
                label = "Quantity / Shares",
                value = quantity,
                onValueChange = { quantity = it },
                keyboardType = KeyboardType.Decimal,
                placeholder = "0.00"
            )
            
            SheetInputField(
                label = "Average Purchase Price ($)",
                value = avgCost,
                onValueChange = { avgCost = it },
                keyboardType = KeyboardType.Decimal,
                placeholder = asset.price.toString()
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    val q = quantity.toDoubleOrNull() ?: 0.0
                    val cost = avgCost.toDoubleOrNull() ?: asset.price
                    if (q > 0) {
                        val portfolioAsset = PortfolioAsset(
                            symbol = asset.symbol,
                            quantity = q,
                            averageCost = cost,
                            currentPrice = asset.price,
                            currentValue = q * asset.price
                        )
                        db.collection("users").document(uid)
                            .collection("portfolio").add(portfolioAsset)
                            .addOnSuccessListener { onDismiss() }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SharkGold),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirm Purchase", color = SharkBlack, fontWeight = FontWeight.Bold)
            }
        }
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
