package com.saket.productscanner.adpater

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.saket.productscanner.databinding.ProductItemBinding
import com.saket.productscanner.models.Product

class ProductAdapter(private val productLongPressed: (Product) -> Unit, private val itemCounter: (Product) -> Unit) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ComparatorDiffUtil()) {

    inner class ProductViewHolder(private val binding:ProductItemBinding) :RecyclerView.ViewHolder(binding.root){
        fun bind(product: Product){
            binding.title.text = product.productName
            binding.desc.text = "${product.productDescription} \n\nItem Cost : Rs. ${String.format("%.2f", product.productCost)}"
            binding.root.setOnLongClickListener{
                productLongPressed(product)
                true
            }
            binding.quantity.text = "${product.quantity}"
            binding.increment.setOnClickListener {
                product.quantity++
                binding.total.text = "Total : Rs. ${String.format("%.2f", (product.productCost * product.quantity))}"
                itemCounter(product)
            }
            binding.decrement.setOnClickListener {
                if (product.quantity > 1){
                    product.quantity--
                    binding.total.text = "Total : Rs. ${String.format("%.2f", (product.productCost * product.quantity))}"
                    itemCounter(product)
                }
            }
            binding.total.text = "Total : Rs. ${String.format("%.2f", (product.productCost * product.quantity))}"
        }
    }



    class ComparatorDiffUtil: DiffUtil.ItemCallback<Product>(){
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.productId == newItem.productId
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ProductItemBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = getItem(position)
        product?.let {
            holder.bind(it)
        }
    }
}