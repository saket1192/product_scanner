package com.saket.productscanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saket.productscanner.models.Product
import com.saket.productscanner.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel(private val productRepository: ProductRepository) : ViewModel() {


    val productList = productRepository.productLiveData

    fun deleteProduct(product: Product) {
        viewModelScope.launch(Dispatchers.Default) {
            productRepository.deleteProductsFromCart(product)
        }
    }

    fun updateProductQuantity(product: Product){
            productRepository.updateQuantity(product)
    }



}