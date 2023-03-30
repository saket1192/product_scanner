package com.saket.productscanner.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.saket.productscanner.db.ProductDatabase
import com.saket.productscanner.models.Product
import com.saket.productscanner.utils.Constants.TAG
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ProductRepository(
    private val productDatabase: ProductDatabase
) {

    var productLiveData: LiveData<List<Product>> = productDatabase.productDao().getProductList()


    fun saveProducts(product: Product) {
        try {
            productDatabase.productDao().insertOrUpdatePedo(product)
        } catch (e: Exception) {
            Log.d(TAG, e.message.toString())
        }

    }

    fun updateQuantity(product: Product){
        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val current = LocalDateTime.now().format(formatter)
            productDatabase.productDao().updateSingleQuantity(product.quantity, current, product.productId)
        } catch (e: Exception){
            Log.d(TAG, e.message.toString())
        }
    }

    fun deleteProductsFromCart(product: Product){
        try {
            productDatabase.productDao().deleteProduct(product)
        } catch (e: Exception){
            Log.d(TAG, e.message.toString())
        }
    }


}