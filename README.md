**QR Scanner Module**

Модуль для сканирования и обработки QR-кодов, включающий:
- Сканирование через камеру с поддержкой вспышки и переключения камер
- Декодирование QR-кодов из изображений галереи
- Копирование результата в буфер обмена
- Обработку состояний (Loading, Error, Success)

**Технологии**

- Camera API (ZXing)
- ViewModel + LiveData
- ActivityResult API для запроса разрешений


**Ограничения**
- Требуется Android API 23+ (для ActivityResultContracts).
- Декодирование больших изображений (>5MB) может занимать до 1-2 секунд.

<img src="https://github.com/kabrishka/QrCodeScanner/blob/master/ex.gif" width="300">
