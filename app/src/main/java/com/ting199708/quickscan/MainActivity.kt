package com.ting199708.quickscan

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.telephony.SmsManager
import android.util.Log
import android.view.SurfaceHolder
import android.widget.NumberPicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private lateinit var cameraSource: CameraSource
    private lateinit var detector: BarcodeDetector
    private lateinit var smsManager: SmsManager
    private var errorToast: Toast? = null
    var peopleNum = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(android.Manifest.permission.CAMERA), 1)
        }

        var data = ""

        val taskHandler = Handler()
        @SuppressLint("MissingPermission")
        val runnable = Runnable {
            if (validData(data)) {
                var smsData = data.substring(5)
                if (peopleNum > 1) smsData += " +$peopleNum"
                cameraSource.stop()
                Toast.makeText(this@MainActivity, "已完成實名登記", Toast.LENGTH_SHORT).show()
                sendSMS(smsData)
                val openSMS = Intent(Intent.ACTION_VIEW)
                openSMS.data = Uri.parse("sms:1922")
                startActivity(openSMS)
                finish()
            } else {
                if (errorToast != null) {
                    errorToast?.cancel()
                }
                errorToast = Toast.makeText(this@MainActivity, "非實聯制QR code", Toast.LENGTH_SHORT)
                errorToast?.show()
            }
            taskHandler.removeCallbacksAndMessages(null)
        }

        numberPicker.setOnValueChangedListener { picker, oldVal, newVal ->
            peopleNum = newVal
        }
        numberPicker.setOnScrollListener { view, scrollState ->  }
        numberPicker.maxValue = 10
        numberPicker.minValue = 1
        numberPicker.value = 1
        numberPicker.displayedValues = arrayOf("選擇同行人數(預設: 1)", "2", "3", "4", "5", "6", "7", "8", "9", "10")
        numberPicker.wrapSelectorWheel = false
        numberPicker.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS

        smsManager = SmsManager.getDefault()

        detector = BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.ALL_FORMATS).build()
        detector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {}
            @SuppressLint("MissingPermission")
            override fun receiveDetections(p0: Detector.Detections<Barcode>) {
                val barcodes = p0.detectedItems
                if (barcodes.size() > 0) {
                    data = barcodes.valueAt(0).displayValue

                    taskHandler.post(runnable)
                }
            }
        })
        cameraSource = CameraSource.Builder(this, detector).setRequestedPreviewSize(1024, 768)
                .setRequestedFps(30f).setAutoFocusEnabled(true).build()
        initSurfaceHolder()

    }

    fun initSurfaceHolder() {
        scanner.holder.addCallback(object: SurfaceHolder.Callback2 {
            override fun surfaceCreated(holder: SurfaceHolder) {
                if (ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                    cameraSource.start(scanner.holder)
                else ActivityCompat.requestPermissions(this@MainActivity, arrayOf(android.Manifest.permission.CAMERA), 1)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {

            }

            override fun surfaceRedrawNeeded(holder: SurfaceHolder) {

            }

        })
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                cameraSource.start(scanner.holder)
            if (ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(android.Manifest.permission.SEND_SMS), 2)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e("onDestory", "onDestory")
        detector.release()
        cameraSource.stop()
        cameraSource.release()
    }

    override fun onPause() {
        Log.e("onPause", "onPause")
        cameraSource.stop()
        super.onPause()
    }

    @SuppressLint("MissingPermission")
    override fun onRestart() {
        super.onRestart()
        Log.e("onRestart", "onRestart")
        initSurfaceHolder()
//        cameraSource.start(scanner.holder)
    }

    fun validData(data: String): Boolean {
        return data.startsWith("1922")
    }

    fun sendSMS(data: String) {
        try {
            smsManager.sendTextMessage("1922", null, data, PendingIntent.getBroadcast(applicationContext, 0, Intent(), 0), null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}