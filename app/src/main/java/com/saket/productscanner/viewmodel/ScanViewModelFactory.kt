package com.saket.productscanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saket.productscanner.repository.ProductRepository

class ScanViewModelFactory(private val productRepository: ProductRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ScanViewModel(productRepository) as T
    }
}