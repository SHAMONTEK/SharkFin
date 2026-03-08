package com.example.sharkfin

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface AlphaVantageApi {
    @GET("query")
    suspend fun getQuote(
        @Query("function") function: String = "GLOBAL_QUOTE",
        @Query("symbol") symbol: String,
        @Query("apikey") apiKey: String
    ): GlobalQuoteResponse

    @GET("query")
    suspend fun getForexRate(
        @Query("function") function: String = "CURRENCY_EXCHANGE_RATE",
        @Query("from_currency") fromCurrency: String,
        @Query("to_currency") toCurrency: String,
        @Query("apikey") apiKey: String
    ): ForexResponse
}

data class GlobalQuoteResponse(
    @com.google.gson.annotations.SerializedName("Global Quote") val lastQuote: GlobalQuote? = null
) {
    data class GlobalQuote(
        @com.google.gson.annotations.SerializedName("01. symbol") val symbol: String,
        @com.google.gson.annotations.SerializedName("02. open") val open: String,
        @com.google.gson.annotations.SerializedName("03. high") val high: String,
        @com.google.gson.annotations.SerializedName("04. low") val low: String,
        @com.google.gson.annotations.SerializedName("05. price") val price: String,
        @com.google.gson.annotations.SerializedName("06. volume") val volume: String,
        @com.google.gson.annotations.SerializedName("08. previous close") val previousClose: String,
        @com.google.gson.annotations.SerializedName("09. change") val change: String,
        @com.google.gson.annotations.SerializedName("10. change percent") val changePercent: String
    )
}

data class ForexResponse(
    @com.google.gson.annotations.SerializedName("Realtime Currency Exchange Rate") val rate: ExchangeRate? = null
) {
    data class ExchangeRate(
        @com.google.gson.annotations.SerializedName("1. From_Currency Code") val fromCode: String,
        @com.google.gson.annotations.SerializedName("2. From_Currency Name") val fromName: String,
        @com.google.gson.annotations.SerializedName("3. To_Currency Code") val toCode: String,
        @com.google.gson.annotations.SerializedName("4. To_Currency Name") val toName: String,
        @com.google.gson.annotations.SerializedName("5. Exchange Rate") val price: String,
        @com.google.gson.annotations.SerializedName("8. Bid Price") val bid: String,
        @com.google.gson.annotations.SerializedName("9. Ask Price") val ask: String
    )
}

object MarketRetrofit {
    private const val BASE_URL = "https://www.alphavantage.co/"

    val api: AlphaVantageApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AlphaVantageApi::class.java)
    }
}
