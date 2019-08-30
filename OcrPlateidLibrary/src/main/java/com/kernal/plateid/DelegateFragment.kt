package com.kernal.plateid

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Base64
import com.kernal.plateid.activity.PlateidCameraActivity
import com.kernal.plateid.model.PlateRecognitionResult
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.subjects.PublishSubject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class DelegateFragment : Fragment() {


    private lateinit var coreSetup: CoreSetup

    lateinit var resultSubject: PublishSubject<PlateRecognitionResult>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retainInstance = true
    }

    fun start(code: String) {

        coreSetup = CoreSetup()
        coreSetup.Devcode = code
        coreSetup.takePicMode = false
        coreSetup.savePicturePath = File(requireActivity().filesDir, "plate.jpg").absolutePath

        resultSubject = PublishSubject.create()

        requestPermissions()

    }


    private fun requestPermissions() {
        RxPermissions(this)
                .request(Manifest.permission.CAMERA,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe {
                    if (it) {
                        startPlateRecognition()

                    } else {
                        resultSubject.onError(RuntimeException("没有权限"))
                    }
                }
    }

    private fun startPlateRecognition() {
        val intent = Intent(requireContext(), PlateidCameraActivity::class.java)
        intent.putExtra("coreSetup", coreSetup)
        startActivityForResult(intent, REQUEST_CODE)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            val plate = data?.getStringExtra("plate")

            if (plate == null) {
                resultSubject.onError(RuntimeException("识别取消"))
            } else {
                val path = data.getStringExtra("savePicturePath")
                val recogResult = data.getStringArrayExtra("RecogResult")

                val left = Integer.valueOf(recogResult[7])
                val top = Integer.valueOf(recogResult[8])
                val width = Integer.valueOf(recogResult[9]) - Integer.valueOf(recogResult[7])
                val height = Integer.valueOf(recogResult[10]) - Integer.valueOf(recogResult[8])

                val bitmap = BitmapFactory.decodeFile(path)
                val plateBitmap = Bitmap.createBitmap(bitmap, left, top, width, height)


                resultSubject.onNext(PlateRecognitionResult(plate, bitmapToBase64(plateBitmap)))
                resultSubject.onComplete()

            }
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {

        var result: String? = null
        var byteArrayOutputStream: ByteArrayOutputStream? = null
        try {
            byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)

            byteArrayOutputStream.flush()
            byteArrayOutputStream.close()

            val bitmapBytes = byteArrayOutputStream.toByteArray()
            result = Base64.encodeToString(bitmapBytes, Base64.DEFAULT)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                if (byteArrayOutputStream != null) {
                    byteArrayOutputStream.flush()
                    byteArrayOutputStream.close()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
        return result ?: ""
    }

    companion object {
        const val REQUEST_CODE = 10002
    }
}