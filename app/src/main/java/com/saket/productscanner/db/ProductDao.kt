package com.saket.productscanner.db

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.*
import com.saket.productscanner.models.Product
import com.saket.productscanner.utils.Constants.TAG

@Dao
interface ProductDao {

    @Query("SELECT * FROM product")
    fun getProductList(): LiveData<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertProduct(product: Product)

    @Delete
    fun deleteProduct(product: Product)

    @Query("Select * from product WHERE productId = :productId")
    fun getProductVerification(productId: String): Product?

    @Query("Update product SET quantity = (quantity + :quantity), dateModified = :dateModified WHERE productId = :productId")
    fun updatePedo(quantity: Int, dateModified: String, productId: String)

    fun insertOrUpdatePedo(product: Product){
        val items = getProductVerification(productId = product.productId)
        if (items != null) {
            Log.d(TAG, items.toString())
            updatePedo(product.quantity, dateModified = product.dateModified, productId = product.productId)
        } else {
            insertProduct(product)
        }
    }
}