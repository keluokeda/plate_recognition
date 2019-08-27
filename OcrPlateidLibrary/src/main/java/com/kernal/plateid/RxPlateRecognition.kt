package com.kernal.plateid

import android.support.v4.app.FragmentActivity
import io.reactivex.Observable

class RxPlateRecognition(activity: FragmentActivity) {

    private val tag = BuildConfig.LIBRARY_PACKAGE_NAME + RxPlateRecognition::class.java.name


    private val delegateFragment: DelegateFragment

    init {
        val fragment = activity.supportFragmentManager.findFragmentByTag(tag)

        if (fragment == null) {
            delegateFragment = DelegateFragment()
            activity.supportFragmentManager.beginTransaction().add(delegateFragment, tag).commitNow()

        } else {
            delegateFragment = fragment as DelegateFragment
        }
    }


    fun start(code: String): Observable<String> {


        delegateFragment.start(code)

        return Observable.just(1)
                .flatMap {
                    delegateFragment.resultSubject
                }
    }
}