package com.saket.productscanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saket.productscanner.models.Product
import com.saket.productscanner.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScanViewModel(private val productRepository: ProductRepository):ViewModel() {

    fun saveProduct(product: Product){
        viewModelScope.launch(Dispatchers.IO) {
            productRepository.saveProducts(product)
        }
    }
}