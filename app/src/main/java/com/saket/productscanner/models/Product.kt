package com.saket.productscanner.models

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "product", indices = [Index(value = ["productId"], unique = true)])
data class Product (
    @PrimaryKey(autoGenerate = false)
    val productId: String,
    val productName: String,
    val dateAdded: String,
    val dateModified: String,
    val productDescription: String,
    val quantity: Int,
    val productCost: Double
        ) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readDouble()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(productId)
        parcel.writeString(productName)
        parcel.writeString(dateAdded)
        parcel.writeString(dateModified)
        parcel.writeString(productDescription)
        parcel.writeInt(quantity)
        parcel.writeDouble(productCost)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Product> {
        override fun createFromParcel(parcel: Parcel): Product {
            return Product(parcel)
        }

        override fun newArray(size: Int): Array<Product?> {
            return arrayOfNulls(size)
        }
    }
}