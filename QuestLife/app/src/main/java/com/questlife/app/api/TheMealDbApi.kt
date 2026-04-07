package com.questlife.app.api

import com.questlife.app.models.TheMealDbResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit интерфейс для TheMealDB API
 */
interface TheMealDbApi {
    
    @GET("search.php")
    suspend fun searchRecipes(@Query("s") name: String): TheMealDbResponse
    
    @GET("search.php")
    suspend fun searchMeals(@Query("s") query: String): Response<TheMealDbResponse>
    
    @GET("random.php")
    suspend fun getRandomMeals(): Response<TheMealDbResponse>
}
