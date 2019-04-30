package com.mobilefirst.symbios

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.graphics.Color

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.telephony.TelephonyManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlin.properties.Delegates


class MainActivity : AppCompatActivity() {

    private lateinit var qrImageView: ImageView

    private lateinit var instructionTextView: TextView

    private lateinit var shareTextView: TextView

    private lateinit var fullNameField: EditText

    private var fullName: String by Delegates.observable("") { prop, old, new ->
        number?.let {
            updateUI(it, new)
        }
    }

    private var number: String? = null

    companion object {
        const val REQUEST_CODE = 99
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)
        qrImageView = findViewById(R.id.qr_image_view)
        instructionTextView = findViewById(R.id.instructions_textview)
        shareTextView = findViewById(R.id.contact_sharing_textview)
        fullNameField = findViewById(R.id.full_name_field)
        fullNameField.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let {
                    fullName = it.toString()
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }
        })
        grabPhoneContact()
    }

    @SuppressLint("HardwareIds")
    private fun grabPhoneContact() {
        val canReadContacts = if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.M) ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) else 0
        val canReadSIM = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
        val canReadState = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
        if (canReadContacts == PERMISSION_GRANTED && canReadSIM == PERMISSION_GRANTED && canReadState == PERMISSION_GRANTED) {
            val service = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            if(service.line1Number != null) {
                number = service.line1Number
            } else {
                showErrorDialog(getString(R.string.no_cellular_number_title), getString(R.string.no_cellular_number_available))
            }

        } else {
            if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.M) {
                requestPermission(true)
                return
            }
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_NUMBERS) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_SMS) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE)
            ) {
                showPermissionRequestRationale()
            } else {
                requestPermission(false)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE -> {
                grabPhoneContact()
            }
        }
    }
    @TargetApi(26)
    private fun requestPermission(isOlderDevice: Boolean) {
        var args = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_SMS
        )
        if (!isOlderDevice) args.add(Manifest.permission.READ_PHONE_NUMBERS)

        ActivityCompat.requestPermissions(
            this, args.toTypedArray(), REQUEST_CODE
        )
    }

    private fun updateUI(number: String, name: String) {
        qrImageView.setImageBitmap(generateImage(vcardFormat(number, name)))
        qrImageView.scaleType = ImageView.ScaleType.FIT_CENTER
    }

    private fun vcardFormat(number: String, name: String): String {
        return """
            BEGIN:VCARD\n
            VERSION:3.0\n
            FN:$name\n
            TEL;TYPE=Mobile:$number\n
            END:VCARD\n
        """.trimIndent()
    }

    private fun showPermissionRequestRationale() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle(R.string.app_permission_request_title)
        alertDialog.setMessage(R.string.app_permission_request_body)
        alertDialog.setPositiveButton(R.string.okay) { dialog, _ ->
            dialog.dismiss()
        }
    }

    private fun showErrorDialog(title: String, message: String) {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle(title)
        alertDialog.setMessage(message)
        alertDialog.setPositiveButton(R.string.okay) { dialog, _ ->  dialog.dismiss() }
    }

    private fun generateImage(str: String): Bitmap? {
        val scale = 800
        val writer = QRCodeWriter()
        val matrix = writer.encode(str, BarcodeFormat.QR_CODE, scale, scale)
        val bitmap = Bitmap.createBitmap(scale, scale, Bitmap.Config.RGB_565)
        for (w in 0 until matrix.width) {
            for (h in 0 until matrix.height) {
                bitmap.setPixel(w, h, if (matrix.get(w, h)) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
