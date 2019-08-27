package com.kernal.plateid

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import com.kernal.plateid.activity.PlateidCameraActivity
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.subjects.PublishSubject

class DelegateFragment : Fragment() {


    private lateinit var coreSetup: CoreSetup

    lateinit var resultSubject: PublishSubject<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retainInstance = true
    }

    fun start(code: String) {

        coreSetup = CoreSetup()
        coreSetup.Devcode = code
        coreSetup.takePicMode = false

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
                resultSubject.onNext(plate)
                resultSubject.onComplete()

            }
        }
    }

    companion object {
        const val REQUEST_CODE = 10002
    }
}