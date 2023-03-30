package com.saket.productscanner.fragment

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.saket.productscanner.ProductApplication
import com.saket.productscanner.R
import com.saket.productscanner.adpater.ProductAdapter
import com.saket.productscanner.databinding.FragmentHomeBinding
import com.saket.productscanner.models.Product
import com.saket.productscanner.utils.Constants.TAG
import com.saket.productscanner.viewmodel.HomeViewModel
import com.saket.productscanner.viewmodel.HomeViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding
    lateinit var homeViewModel: HomeViewModel
    private lateinit var adapter: ProductAdapter
    val totalAmountLiveData = MutableLiveData<Double>()

    //Google Signin
    var gso: GoogleSignInOptions? = null
    var gsc: GoogleSignInClient? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        adapter = ProductAdapter(::productLongPressed, ::itemCounter)


        return _binding!!.root
    }

    private fun productLongPressed(product: Product) {
        lifecycleScope.launch(Dispatchers.Default) {
            homeViewModel.deleteProduct(product)
            withContext(Dispatchers.Main) {
                adapter.notifyDataSetChanged()
                delay(1000)
                getAmount()
            }

        }


    }

    private fun itemCounter(product: Product) {
        lifecycleScope.launch(Dispatchers.Default) {
            homeViewModel.updateProductQuantity(product)

            withContext(Dispatchers.Main) {
                adapter.notifyDataSetChanged()
                delay(1000)
                getAmount()
            }


        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val repository = (requireActivity().application as ProductApplication).productRepository
        homeViewModel = ViewModelProvider(
            requireActivity(),
            HomeViewModelFactory(repository)
        ).get(HomeViewModel::class.java)

        checkPermission()

        binding?.recyclerView?.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        binding?.recyclerView?.adapter = adapter

        binding?.btnCreateCart?.setOnClickListener {

            findNavController().navigate(R.id.action_homeFragment_to_scanFragment)

        }


        homeViewModel.productList.observe(viewLifecycleOwner, Observer {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                adapter.submitList(it.distinctBy { product: Product ->  product.productId})
                delay(1000)
                getAmount()
            }


        })

        totalAmountLiveData.observe(viewLifecycleOwner, Observer {
            binding?.totalPrice?.text = "Total Rs.${String.format("%.2f", it)}"
        })

        binding?.btnCheckout?.setOnClickListener {
            if (adapter.currentList.isEmpty()){
                Toast.makeText(requireActivity(), "Please add items in the cart.", Toast.LENGTH_SHORT).show()
            } else {
                val bundle = Bundle().apply {
                    putParcelableArrayList("productList", ArrayList(adapter.currentList))
                }
                findNavController().navigate(R.id.action_homeFragment_to_phonePayQRFragment, bundle)
            }

        }

        gso =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build()
        gsc = GoogleSignIn.getClient(requireActivity(), gso!!)

        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(requireActivity())
        if (account != null) {
            Log.d(
                TAG,
                "Account details ${account.email}  ${account.displayName} ${account.idToken} ${account.id}"
            )
        } else {
            signIN()
        }


    }

    private fun getAmount() {
        var totalAmount = 0.0
        for (product in adapter.currentList) {
            totalAmount += (product.productCost * product.quantity)
        }
        totalAmountLiveData.postValue(totalAmount)
    }

    private fun signOutAccount() {
        gsc?.signOut()?.addOnCompleteListener {
            if (it.isComplete) {
                Log.d(TAG, "Google Account Signed Out")
            }
        }
    }

    private fun signIN() {
        val signInIntent = gsc?.signInIntent
        startActivityForResult(signInIntent, 1000)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1000) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                task.getResult(ApiException::class.java)
                Log.d(TAG, "Account signin status ${task.isSuccessful}")
            } catch (e: ApiException) {
                e.printStackTrace()
            }
        }
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE
                )
            )
        } else {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            )
        }
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        }

    override fun onDestroyView() {
        super.onDestroyView()
        // clear the adapter data
        adapter.submitList(null)
        // detach the adapter from the recycler view
        binding?.recyclerView?.adapter = null
        _binding = null
        viewModelStore.clear()
    }
}