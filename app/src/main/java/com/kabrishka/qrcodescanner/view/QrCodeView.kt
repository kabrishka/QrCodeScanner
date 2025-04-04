package com.kabrishka.qrcodescanner.view

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import com.journeyapps.barcodescanner.BarcodeView
import com.journeyapps.barcodescanner.camera.CameraSettings

/**
 * Реализация View для сканирования и обработки QR-кодов.
 * Предоставляет функционал управления вспышкой и переключения между камерами устройства.
 */
class QrCodeView : BarcodeView {

    private var torchOn: Boolean = false
    private var frontCamera: Boolean = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr)

    /**
     * Сохраняет текущее состояние компонента
     * @return Сохраненное состояние с параметрами фонарика и выбранной камеры
     */
    override fun onSaveInstanceState(): Parcelable = super.onSaveInstanceState().let { superState ->
        Bundle().apply {
            putBoolean(TORCH_STATE_KEY, torchOn)
            putBoolean(FRONT_CAMERA_STATE_KEY, frontCamera)
            putParcelable("superState", superState)
        }
    }

    @SuppressLint("NewApi")
    override fun onRestoreInstanceState(state: Parcelable?) {
        when (state) {
            is Bundle -> {
                torchOn = state.getBoolean(TORCH_STATE_KEY, false)
                setTorch(torchOn)

                frontCamera = state.getBoolean(FRONT_CAMERA_STATE_KEY, false)
                updateCameraSettings()

                val superState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    state.getParcelable("superState", Parcelable::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    state.getParcelable("superState")
                }
                super.onRestoreInstanceState(superState)
            }
            else -> super.onRestoreInstanceState(state)
        }
    }

    /**
     * Инвертирует текущее состояние вспышки
     */
    fun switchTorch() {
        torchOn = !torchOn
        setTorch(torchOn)
    }

    /**
     * Выполняет переключение между фронтальной и основной камерами
     * @return Флаг успешного выполнения операции
     */
    fun flip(): Boolean {
        if (!hasFrontCamera()) {
            return false
        }

        frontCamera = !frontCamera
        return updateCameraSettings()
    }

    /**
     * Применяет текущие настройки камеры
     * @return Статус успешности обновления параметров
     */
    private fun updateCameraSettings(): Boolean {
        val cameraId = findCameraId(frontCamera) ?: return false

        return try {
            cameraSettings = CameraSettings().apply {
                requestedCameraId = cameraId
            }
            restartCamera()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Определяет идентификатор камеры по ее типу
     * @param frontCamera Требуется ли фронтальная камера
     * @return Идентификатор камеры или null при отсутствии
     */
    private fun findCameraId(frontCamera: Boolean): Int? {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return cameraManager.cameraIdList.firstOrNull { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            when {
                frontCamera -> lensFacing == CameraCharacteristics.LENS_FACING_FRONT
                else -> lensFacing == CameraCharacteristics.LENS_FACING_BACK
            }
        }?.toIntOrNull()
    }

    /**
     * Проверяет доступность фронтальной камеры
     * @return Наличие доступной фронтальной камеры
     */
    private fun hasFrontCamera(): Boolean {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return cameraManager.cameraIdList.any { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        }
    }

    /**
     * Выполняет перезапуск камеры с текущими параметрами
     */
    private fun restartCamera() {
        pause()
        resume()
    }

    companion object {
        private const val TORCH_STATE_KEY = "torch_state"
        private const val FRONT_CAMERA_STATE_KEY = "front_camera_state"
    }
}