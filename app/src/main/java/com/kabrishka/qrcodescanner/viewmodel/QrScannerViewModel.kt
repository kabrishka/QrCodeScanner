package com.kabrishka.qrcodescanner.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ResultPoint
import com.google.zxing.common.GlobalHistogramBinarizer
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.kabrishka.qrcodescanner.model.DecodingResult
import com.kabrishka.qrcodescanner.model.Error
import com.kabrishka.qrcodescanner.model.Loading
import com.kabrishka.qrcodescanner.model.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel для сканирования QR-кодов, обрабатывающий логику декодирования
 * и предоставляющий результаты через LiveData.
 *
 * @property decodingResult LiveData с результатом декодирования QR-кода
 */
class QrScannerViewModel(application: Application) : AndroidViewModel(application) {


    private val _decodingResult = MutableLiveData<DecodingResult>()
    val decodingResult: LiveData<DecodingResult> = _decodingResult

    private val contentResolver: ContentResolver = application.contentResolver

    /**
     * Callback для обработки результатов сканирования через камеру.
     */
    val barcodeCallback = object : BarcodeCallback {
        /**
         * Вызывается при успешном распознании QR-кода.
         * @param result Результат сканирования с текстом QR-кода
         */
        override fun barcodeResult(result: BarcodeResult) {
            _decodingResult.postValue(Success(result.text))
        }

        /**
         * Вызывается при обнаружении возможных точек QR-кода (не используется).
         * @param resultPoints Список возможных точек QR-кода
         */
        override fun possibleResultPoints(resultPoints: MutableList<ResultPoint?>?) {
            // Не требуется реализация для базового сценария
        }
    }

    /**
     * Декодирует QR-код из изображения по URI.
     *
     * Процесс включает:
     * 1. Оптимизацию размера изображения
     * 2. Преобразование в бинарный формат
     * 3. Непосредственное декодирование QR-кода
     *
     * Результат публикуется в [decodingResult].
     *
     * @param uri URI изображения для декодирования
     */
    fun decodeImageFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _decodingResult.postValue(Loading)

                // 1. Уменьшаем размер изображения
                val inputStream = contentResolver.openInputStream(uri)
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream?.close()

                val scaleFactor = calculateInSampleSize(options, 1000, 1000)
                val newOptions = BitmapFactory.Options().apply { inSampleSize = scaleFactor }

                val newInputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(newInputStream, null, newOptions)
                newInputStream?.close()

                if (bitmap == null) {
                    _decodingResult.postValue(Error("Failed to decode bitmap"))
                    return@launch
                }

                val intArray = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

                // 3. Используем более быстрый бинаризатор
                val source = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
                val binaryBitmap = BinaryBitmap(GlobalHistogramBinarizer(source))

                // 4. Декодируем
                val reader = MultiFormatReader()
                val hints = mapOf<DecodeHintType, Any>(
                    DecodeHintType.TRY_HARDER to true,
                    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)
                )

                val result = reader.decode(binaryBitmap, hints)
                _decodingResult.postValue(Success(result.text))

            } catch (e: NotFoundException) {
                _decodingResult.postValue(Error("QR code not found"))
            } catch (e: Exception) {
                _decodingResult.postValue(Error("Error: ${e.message}"))
            }
        }
    }

    /**
     * Вычисляет оптимальный коэффициент масштабирования для уменьшения изображения.
     *
     * @param options Параметры исходного изображения
     * @param reqWidth Требуемая ширина
     * @param reqHeight Требуемая высота
     * @return Коэффициент масштабирования (степень двойки)
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Вычисляем наибольший inSampleSize, который является степенью 2 и
            // сохраняет размеры больше требуемых
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}