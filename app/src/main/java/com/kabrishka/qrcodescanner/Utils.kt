package com.kabrishka.qrcodescanner

import android.view.View

const val TAG = "QrCodeScanner"

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}