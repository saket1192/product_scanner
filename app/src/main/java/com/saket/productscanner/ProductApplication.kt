package com.saket.productscanner

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.saket.productscanner.db.ProductDatabase
import com.saket.productscanner.repository.ProductRepository
import com.saket.productscanner.utils.Constants.TAG
import com.saket.productscanner.utils.Utility

class ProductApplication: Application() {

    lateinit var productRepository: ProductRepository

    override fun onCreate() {
        super.onCreate()

        Utility.instance?.AssignDirectory(applicationContext) // setup directories for log files

        Utility.instance?.writeLogSpp(TAG, "App Starting....")


        val database = ProductDatabase.getDatabase(applicationContext)
        productRepository = ProductRepository(database)

        DynamicColors.applyToActivitiesIfAvailable(this)

    }
}