package com.saket.productscanner.fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.FileContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.FontFactory
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import com.saket.productscanner.ProductApplication
import com.saket.productscanner.R
import com.saket.productscanner.databinding.FragmentPhonePayQRBinding
import com.saket.productscanner.models.Product
import com.saket.productscanner.utils.Constants
import com.saket.productscanner.utils.Constants.TAG
import com.saket.productscanner.utils.Utility
import com.saket.productscanner.viewmodel.PhonePayViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

class PhonePayQRFragment : Fragment() {


    private var _binding: FragmentPhonePayQRBinding? = null
    private val binding get() = _binding
    private lateinit var phonePayViewModel: PhonePayViewModel
    var totalSum: Double = 0.0
    var cgst_total: Double = 0.0
    private var productList: List<Product>? = null
    private lateinit var mDrive: Drive

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentPhonePayQRBinding.inflate(inflater, container,false)
        productList = arguments?.getParcelableArrayList<Product>("productList")
        totalSum = 0.0
        mDrive = getDriveService(requireActivity())
        return _binding!!.root
    }

    private fun getDriveService(context: Context): Drive{
        GoogleSignIn.getLastSignedInAccount(context).let { googleAccount ->
            val credential = GoogleAccountCredential.usingOAuth2(
                context, listOf(DriveScopes.DRIVE_FILE)
            )
            credential.selectedAccount = googleAccount!!.account!!
            return Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                JacksonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName(getString(R.string.app_name))
                .build()
        }
        var tempDrive: Drive
        return tempDrive
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        phonePayViewModel = ViewModelProvider(requireActivity())[PhonePayViewModel::class.java]

        val itemList: List<Product>? = productList

        // Add the table data
        itemList?.forEach {
            totalSum += (it.quantity * it.productCost)
        }

        cgst_total = (totalSum * 0.09)

        if (binding?.checkbox?.isChecked == true){
            binding?.price?.text = "Total Price : Rs. ${String.format("%.2f", (totalSum + (2 * cgst_total)))}"
        } else {
            binding?.price?.text = "Total Price : Rs. ${String.format("%.2f", (totalSum))}"
        }


        val transactionNote = "Test payment"
        val payeeName = "Your Shop Name"
        val phoneNumber = "Your mobile number"
        binding?.phonePayQr?.let {
            generateAndSetPhonePeQRCodeToImageView(totalSum, transactionNote, payeeName, phoneNumber,
                it
            )
        }

        binding?.btnCheckout?.setOnClickListener {
            binding?.checkbox?.isChecked?.let { it1 -> createPDF(it1) }

        }

        binding?.btnCreateCart?.setOnClickListener {
            binding?.checkbox?.isChecked?.let { it1 -> createPDF(it1) }
        }

        binding?.backButton?.setOnClickListener {
            findNavController().popBackStack()
        }

        binding?.checkbox?.setOnCheckedChangeListener{buttonView, isChecked ->
            var amountTotal = 0.0
            amountTotal = if (isChecked){
                totalSum + (2 * cgst_total)
            } else {
                totalSum
            }
            binding?.price?.text = "Total Price : Rs. ${String.format("%.2f", amountTotal)}"
            binding?.phonePayQr?.let {
                generateAndSetPhonePeQRCodeToImageView(amountTotal, transactionNote, payeeName, phoneNumber,
                    it
                )
            }
        }

    }

    private fun createPDF(isGst: Boolean) {
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                // Create a new PDF document
                val document = Document()

                //Get random alphanumeric characters for invoice
                val sdf = SimpleDateFormat("hhmmddMMyyyy")
                val currentDate = sdf.format(Date())
                val randomString = generateRandomString(10) // set the length of the string here
                val alphanumericString = "$currentDate-$randomString"

                // Create a new file to save the PDF document
                var file: File? = null
                var outputStream:FileOutputStream? = null


                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                    // Use WRITE_EXTERNAL_STORAGE permission for Android 10 and earlier
                    if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        // Permission is granted, so proceed with file access
                        file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "$alphanumericString.pdf")
                        outputStream = FileOutputStream(file)
                    } else {
                        // Permission is not granted, so request it
                        ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
                    }
                } else {
                    file = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                        "$alphanumericString.pdf"
                    )
                    outputStream = FileOutputStream(file)
                }





                // Create a new PDF writer using the PdfWriter class and attach it to the document
                PdfWriter.getInstance(document, outputStream)

                // Open the document
                document.open()

                // Set the font for the document
                val font = FontFactory.getFont(FontFactory.TIMES_ROMAN, 12f)

                // Add the company details
                val companyDetails =
                    "Your Shop details"
                val companyDetailsParagraph = Paragraph(companyDetails, font)
                companyDetailsParagraph.spacingAfter = 10f
                companyDetailsParagraph.alignment = Element.ALIGN_CENTER
                document.add(companyDetailsParagraph)


                // Add the invoice details
                val invoiceDetails = "INVOICE NO : $alphanumericString"
                val invoiceDetailsParagraph = Paragraph(invoiceDetails, font)
                invoiceDetailsParagraph.alignment = Element.ALIGN_LEFT
                document.add(invoiceDetailsParagraph)

//            // Add the customer details
//            val customerDetails =
//                "PARTY'S NAME: -\nM/S ADLIFE MARKETING\nSARTIA VIHAR, NEW DELHI-110076\nGSTIN: 07AAFD8457JU3"
//            val customerDetailsParagraph = Paragraph(customerDetails, font)
//            customerDetailsParagraph.spacingBefore = 10f
//            document.add(customerDetailsParagraph)

                // Add the table of products
                val table = PdfPTable(5)
                table.setWidths(floatArrayOf(5f, 1.5f, 1f, 1f, 1f))
                table.spacingBefore = 20f
                table.spacingAfter = 20f

                // Add the table header
                val header1 =
                    PdfPCell(Paragraph("Particulars (Descriptions & Specifications)", font))
                header1.horizontalAlignment = Element.ALIGN_CENTER
                header1.verticalAlignment = Element.ALIGN_MIDDLE
                val header2 = PdfPCell(Paragraph("HSN Code", font))
                header2.horizontalAlignment = Element.ALIGN_CENTER
                header2.verticalAlignment = Element.ALIGN_MIDDLE
                val header3 = PdfPCell(Paragraph("Qty", font))
                header3.horizontalAlignment = Element.ALIGN_CENTER
                header3.verticalAlignment = Element.ALIGN_MIDDLE
                val header4 = PdfPCell(Paragraph("Rate", font))
                header4.horizontalAlignment = Element.ALIGN_CENTER
                header4.verticalAlignment = Element.ALIGN_MIDDLE
                val header5 = PdfPCell(Paragraph("Amount", font))
                header5.horizontalAlignment = Element.ALIGN_CENTER
                header5.verticalAlignment = Element.ALIGN_MIDDLE

                table.addCell(header1)
                table.addCell(header2)
                table.addCell(header3)
                table.addCell(header4)
                table.addCell(header5)


                val itemList: List<Product>? = productList
                var amount: Double = 0.0
                // Add the table data
                itemList?.forEach {
                    val cell1 = PdfPCell(Paragraph(" ${it.productName.uppercase()} ", font))
                    cell1.horizontalAlignment = Element.ALIGN_LEFT
                    cell1.verticalAlignment = Element.ALIGN_MIDDLE
                    table.addCell(cell1)

                    val cell2 = PdfPCell(Paragraph("${it.productId}", font))
                    cell2.horizontalAlignment = Element.ALIGN_CENTER
                    cell2.verticalAlignment = Element.ALIGN_MIDDLE
                    table.addCell(cell2)

                    val cell3 = PdfPCell(Paragraph("${it.quantity}", font))
                    cell3.horizontalAlignment = Element.ALIGN_CENTER
                    cell3.verticalAlignment = Element.ALIGN_MIDDLE
                    table.addCell(cell3)

                    val cell4 =
                        PdfPCell(Paragraph("${String.format("%.2f", it.productCost)}", font))
                    cell4.horizontalAlignment = Element.ALIGN_CENTER
                    cell4.verticalAlignment = Element.ALIGN_MIDDLE
                    table.addCell(cell4)

                    val cell5 = PdfPCell(
                        Paragraph(
                            "${
                                String.format(
                                    "%.2f",
                                    (it.quantity * it.productCost)
                                )
                            }", font
                        )
                    )
                    cell5.horizontalAlignment = Element.ALIGN_CENTER
                    cell5.verticalAlignment = Element.ALIGN_MIDDLE
                    table.addCell(cell5)

                    amount += it.quantity * it.productCost
                }

                var cgstTotal = 0.0
                if (isGst){
                    cgstTotal  = amount * 0.09
                }

                // Add the CGST and SGST details
                val cgst = PdfPCell(Paragraph("Add : CGST @ 9%", font))
                cgst.colspan = 4
                cgst.horizontalAlignment = Element.ALIGN_RIGHT
                cgst.verticalAlignment = Element.ALIGN_MIDDLE
                table.addCell(cgst)

                val cgstAmount = PdfPCell(Paragraph("${String.format("%.2f", cgstTotal)}", font))
                cgstAmount.horizontalAlignment = Element.ALIGN_CENTER
                cgstAmount.verticalAlignment = Element.ALIGN_MIDDLE
                table.addCell(cgstAmount)

                val sgst = PdfPCell(Paragraph("Add : SGST @ 9%", font))
                sgst.colspan = 4
                sgst.horizontalAlignment = Element.ALIGN_RIGHT
                sgst.verticalAlignment = Element.ALIGN_MIDDLE
                table.addCell(sgst)

                val sgstAmount = PdfPCell(Paragraph("${String.format("%.2f", cgstTotal)}", font))
                sgstAmount.horizontalAlignment = Element.ALIGN_CENTER
                sgstAmount.verticalAlignment = Element.ALIGN_MIDDLE
                table.addCell(sgstAmount)

                // Add the grand total
                val grandTotal = PdfPCell(Paragraph("Grand Total", font))
                grandTotal.colspan = 4
                grandTotal.horizontalAlignment = Element.ALIGN_RIGHT
                grandTotal.verticalAlignment = Element.ALIGN_MIDDLE
                table.addCell(grandTotal)

                val grandTotalAmount = PdfPCell(
                    Paragraph(
                        "${String.format("%.2f", (amount + cgstTotal + cgstTotal))}",
                        font
                    )
                )
                grandTotalAmount.horizontalAlignment = Element.ALIGN_CENTER
                grandTotalAmount.verticalAlignment = Element.ALIGN_MIDDLE
                table.addCell(grandTotalAmount)

                // Add the table to the document
                document.add(table)


                // Add the terms and conditions
                val termsAndConditions =
                    "Warranty related Terms & conditions\n1. An Invoice Must accompany products returned for warranty.\n2. Goods damaged During transit voids warranty.\n3. 90 days limited warranty unless otherwise stated.\n4. 30 days limited warranty on OEM processor ( an internal parts of the product) exchange the same items only.\n5. All items carry MFG Warranty only No return or exchange."
                val termsAndConditionsParagraph = Paragraph(termsAndConditions, font)
                termsAndConditionsParagraph.spacingBefore = 20f
                termsAndConditionsParagraph.spacingAfter = 20f
                document.add(termsAndConditionsParagraph)


                // Close the document
                document.close()

                withContext(Dispatchers.Main){
                    Toast.makeText(
                        requireActivity(),
                        "PDF file has been saved to Documents folder.",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
                // Show a toast message to indicate that the PDF file has been saved





                uploadToGoogleDrive(file!!)

            } catch (e: Exception) {
                Log.d(TAG, "${e.localizedMessage}")
            }

        }
    }

    private fun deleteCart(){
        lifecycleScope.launch(Dispatchers.Default){
            val repository = (requireActivity().application as ProductApplication).productRepository
            val itemList: List<Product>? = productList
            // Add the table data
            itemList?.forEach {
                repository.deleteProductsFromCart(it)
            }
            withContext(Dispatchers.Main){
                findNavController().popBackStack()
            }
        }

    }

    private fun uploadToGoogleDrive(file: File){

        mDrive.let { googleDriveService ->
            lifecycleScope.launch {
                try {
                    val raunit = file
                    val gFile = com.google.api.services.drive.model.File()
                    gFile.name = "${raunit.name}"
                    val mimetype = "application/pdf"
                    val fileContent = FileContent(mimetype, raunit)
                    var fileId = ""

                    withContext(Dispatchers.Main){
                        withContext(Dispatchers.IO){
                            launch {
                                var mFile = googleDriveService.Files().create(gFile, fileContent).execute()
                            }
                        }
                    }
                } catch (userAuthEx: UserRecoverableAuthIOException){
                    startActivity(
                        userAuthEx.intent
                    )
                } catch (e: Exception){
                    e.printStackTrace()
                    Log.e(TAG, e.toString())
                    lifecycleScope.launch(Dispatchers.Main){
                        Toast.makeText(requireActivity(), "Some error occurred in uploading files", Toast.LENGTH_SHORT).show()
                    }

                }
                deleteCart()
            }
        }
    }






    private fun generateRandomString(length: Int): String {
        val allowedChars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        val random = Random(System.currentTimeMillis())
        return (1..length)
            .map { allowedChars[random.nextInt(allowedChars.length)] }
            .joinToString("")
    }


    private fun generateAndSetPhonePeQRCodeToImageView(amount: Double, transactionNote: String?, payeeName: String?, phoneNumber: String?, imageView: ImageView) {
        //Get random alphanumeric characters for invoice
        val randomString = generateRandomString(20) // set the length of the string here
        val alphanumericString = "$randomString"

        Utility.instance?.writeLogSpp(TAG, "$amount, $payeeName, $phoneNumber")


        val paymentUrl = "upi://pay?pa=$phoneNumber@ybl&pn=$payeeName&tr=$alphanumericString&am=${String.format("%.2f", amount)}&cu=INR&tn=this is the test transaction."

        // Generate the QR code from the payment URL
        val qrCodeBitmap = generateQRCodeBitmap(paymentUrl, 300, 300)

        // Set the QR code bitmap to the ImageView
        imageView.setImageBitmap(qrCodeBitmap)
    }

    private fun generateQRCodeBitmap(data: String, width: Int, height: Int): Bitmap? {
        try {
            // Set up hints for generating the QR code
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
            hints[EncodeHintType.MARGIN] = 1

            // Generate the QR code matrix
            val bitMatrix = MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, width, height, hints)

            // Compute the size of each module (square) in the QR code
            val moduleSize = min(width / bitMatrix.width, height / bitMatrix.height)

            // Create a bitmap with the desired size and draw the QR code onto it
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            val canvas = android.graphics.Canvas(bitmap)
            for (y in 0 until bitMatrix.height) {
                for (x in 0 until bitMatrix.width) {
                    if (bitMatrix[x, y]) {
                        val moduleLeft = x * moduleSize
                        val moduleTop = y * moduleSize
                        canvas.drawRect(moduleLeft.toFloat(), moduleTop.toFloat(), (moduleLeft + moduleSize).toFloat(), (moduleTop + moduleSize).toFloat(), android.graphics.Paint().apply { color = Color.BLACK })
                    }
                }
            }
            return bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
        }
        return null
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        viewModelStore.clear()
    }
}