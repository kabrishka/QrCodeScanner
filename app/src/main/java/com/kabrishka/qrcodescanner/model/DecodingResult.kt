package com.kabrishka.qrcodescanner.model

sealed class DecodingResult

data object Loading: DecodingResult()
data class Error(val message: String): DecodingResult()
data class Success(val result: String): DecodingResult()