package app.romanmarinov.dochintkmp.data.ocr.paddle

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import app.romanmarinov.dochintkmp.data.ocr.OcrEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min

private data class TextBox(val rect: Rect)

/**
 * Полностью нейросетевой OCR (PP-OCRv5): Detection + Recognition через ONNX Runtime.
 */
class PaddleOcrEngine(private val context: Context) : OcrEngine {

    private var env: OrtEnvironment? = null
    private var detSession: OrtSession? = null
    private var recSession: OrtSession? = null
    private var dictionary: List<String> = emptyList()

    override suspend fun recognizeText(uri: Uri): String = withContext(Dispatchers.IO) {
        val bitmap = loadBitmap(uri)
        try {
            recognizeBitmap(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    suspend fun recognizeBitmap(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        ensureInitialized()

        val rawBoxes = detectText(bitmap)
        if (rawBoxes.isEmpty()) {
            return@withContext ""
        }
        val boxes = splitTallBoxes(rawBoxes)

        val rows = groupIntoRows(boxes)

        val lines = recognizeRows(bitmap, rows)
        lines.joinToString("\n")
    }

    @Synchronized
    private fun ensureInitialized() {
        if (env != null) return

        val ortEnv = OrtEnvironment.getEnvironment()
        env = ortEnv
        detSession = ortEnv.createSession(loadAsset("paddle/det.onnx"))

        recSession = ortEnv.createSession(loadAsset("paddle/rec.onnx"))

        dictionary = loadDictionary()
    }

    private fun loadDictionary(): List<String> {
        return context.assets.open("paddle/dict.txt").bufferedReader()
            .readLines()
            .filter { it.isNotEmpty() }
    }

    private fun loadAsset(path: String): ByteArray {
        return try {
            context.assets.open(path).readBytes()
        } catch (e: Exception) {
            throw IllegalStateException("PaddleOCR: файл $path не найден в assets.", e)
        }
    }

    private fun detectText(bitmap: Bitmap): List<TextBox> {
        val maxSideLen = 960
        val ratio = min(
            maxSideLen.toFloat() / bitmap.width,
            maxSideLen.toFloat() / bitmap.height
        ).coerceAtMost(1f)

        var newW = ((bitmap.width * ratio).toInt() / 32) * 32
        var newH = ((bitmap.height * ratio).toInt() / 32) * 32
        if (newW <= 0) newW = 32
        if (newH <= 0) newH = 32

        val resized = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        val inputData = detImageToChw(resized, newW, newH)
        if (resized !== bitmap) resized.recycle()

        val inputName = detSession!!.inputNames.first()
        val shape = longArrayOf(1, 3, newH.toLong(), newW.toLong())
        val tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputData), shape)

        val result = detSession!!.run(mapOf(inputName to tensor))
        val outTensor = result.get(0) as OnnxTensor
        val outShape = outTensor.info.shape

        val mapH: Int
        val mapW: Int
        when (outShape.size) {
            4 -> { mapH = outShape[2].toInt(); mapW = outShape[3].toInt() }
            3 -> { mapH = outShape[1].toInt(); mapW = outShape[2].toInt() }
            else -> { tensor.close(); result.close(); return emptyList() }
        }

        val buf = outTensor.byteBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer()
        val boxes = dbPostProcess(buf, mapW, mapH, bitmap.width, bitmap.height)

        tensor.close()
        result.close()
        return boxes
    }

    private fun dbPostProcess(
        buf: FloatBuffer, mapW: Int, mapH: Int, origW: Int, origH: Int
    ): List<TextBox> {
        val threshold = 0.3f
        val totalPixels = mapW * mapH

        var mask = BooleanArray(totalPixels)
        for (i in 0 until totalPixels) {
            mask[i] = buf.get(i) > threshold
        }

        mask = dilate(mask, mapW, mapH)
        mask = dilate(mask, mapW, mapH)

        val rects = findConnectedComponents(mask, mapW, mapH, minArea = 10)

        val scaleX = origW.toFloat() / mapW
        val scaleY = origH.toFloat() / mapH

        return rects
            .map { r ->
                val bw = r.right - r.left
                val bh = r.bottom - r.top
                val ex = max((bw * 0.1f).toInt(), 2)
                val ey = max((bh * 0.1f).toInt(), 1)
                TextBox(
                    Rect(
                        max(0, ((r.left - ex) * scaleX).toInt()),
                        max(0, ((r.top - ey) * scaleY).toInt()),
                        min(origW, ((r.right + ex) * scaleX).toInt()),
                        min(origH, ((r.bottom + ey) * scaleY).toInt())
                    )
                )
            }
            .filter { it.rect.width() > 5 && it.rect.height() > 5 }
            .sortedWith(compareBy({ it.rect.top }, { it.rect.left }))
    }

    private fun dilate(mask: BooleanArray, w: Int, h: Int): BooleanArray {
        val result = mask.copyOf()
        for (y in 0 until h) {
            for (x in 0 until w) {
                if (!mask[y * w + x]) continue
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val ny = y + dy; val nx = x + dx
                        if (ny in 0 until h && nx in 0 until w) {
                            result[ny * w + nx] = true
                        }
                    }
                }
            }
        }
        return result
    }

    private fun findConnectedComponents(
        mask: BooleanArray, w: Int, h: Int, minArea: Int
    ): List<Rect> {
        val visited = BooleanArray(w * h)
        val results = mutableListOf<Rect>()

        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                if (!mask[idx] || visited[idx]) continue

                var minX = x; var maxX = x; var minY = y; var maxY = y
                val queue = ArrayDeque<Int>(256)
                queue.add(idx)
                visited[idx] = true
                var area = 0

                while (queue.isNotEmpty()) {
                    val cur = queue.removeFirst()
                    val cy = cur / w
                    val cx = cur % w
                    if (cx < minX) minX = cx
                    if (cx > maxX) maxX = cx
                    if (cy < minY) minY = cy
                    if (cy > maxY) maxY = cy
                    area++

                    for ((ddx, ddy) in DIRS) {
                        val nx = cx + ddx; val ny = cy + ddy
                        if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue
                        val ni = ny * w + nx
                        if (!visited[ni] && mask[ni]) {
                            visited[ni] = true
                            queue.add(ni)
                        }
                    }
                }

                if (area >= minArea) {
                    results.add(Rect(minX, minY, maxX + 1, maxY + 1))
                }
            }
        }
        return results
    }

    private fun splitTallBoxes(boxes: List<TextBox>): List<TextBox> {
        if (boxes.size < 3) return boxes

        val heights = boxes.map { it.rect.height() }.sorted()
        val medianHeight = heights[heights.size / 2]
        if (medianHeight < 5) return boxes

        val maxLineHeight = (medianHeight * 1.7f).toInt()

        val result = mutableListOf<TextBox>()
        for (box in boxes) {
            val h = box.rect.height()
            if (h > maxLineHeight) {
                val numLines = Math.round(h.toFloat() / medianHeight).coerceIn(2, 5)
                val lineH = h / numLines
                for (i in 0 until numLines) {
                    result.add(
                        TextBox(
                            Rect(
                                box.rect.left,
                                box.rect.top + i * lineH,
                                box.rect.right,
                                box.rect.top + (i + 1) * lineH
                            )
                        )
                    )
                }
            } else {
                result.add(box)
            }
        }
        return result
    }

    private fun groupIntoRows(boxes: List<TextBox>): List<List<TextBox>> {
        val tolerance = 12
        val rowMap = mutableMapOf<Int, MutableList<TextBox>>()

        for (box in boxes.sortedBy { it.rect.top }) {
            val cy = box.rect.centerY()
            val existing = rowMap.entries.find { kotlin.math.abs(it.key - cy) <= tolerance }
            if (existing != null) {
                existing.value.add(box)
            } else {
                rowMap[cy] = mutableListOf(box)
            }
        }

        return rowMap.entries
            .sortedBy { it.key }
            .map { (_, row) -> row.sortedBy { it.rect.left } }
    }

    private fun recognizeRows(bitmap: Bitmap, rows: List<List<TextBox>>): List<String> {
        val results = mutableListOf<String>()

        for ((idx, row) in rows.withIndex()) {
            val rowTexts = mutableListOf<String>()
            for (box in row) {
                val crop = cropBox(bitmap, box.rect)
                if (crop.width < 3 || crop.height < 3) {
                    crop.recycle()
                    continue
                }
                val text = recognizeBox(crop)
                crop.recycle()
                if (text.isNotBlank()) rowTexts.add(text)
            }
            if (rowTexts.isNotEmpty()) {
                results.add(rowTexts.joinToString(" "))
            }
        }
        return results
    }

    private fun cropBox(bitmap: Bitmap, rect: Rect): Bitmap {
        val left = rect.left.coerceIn(0, bitmap.width - 1)
        val top = rect.top.coerceIn(0, bitmap.height - 1)
        val right = rect.right.coerceIn(left + 1, bitmap.width)
        val bottom = rect.bottom.coerceIn(top + 1, bitmap.height)
        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
    }

    private fun recognizeBox(crop: Bitmap): String {
        val ratio = crop.width.toFloat() / crop.height.toFloat()
        var resizedW = Math.ceil((REC_IMG_H * ratio).toDouble()).toInt()
        resizedW = min(resizedW, REC_IMG_W).coerceAtLeast(1)

        val resized = Bitmap.createScaledBitmap(crop, resizedW, REC_IMG_H, true)
        val data = recImageToChw(resized, resizedW)
        if (resized !== crop) resized.recycle()

        val shape = longArrayOf(1, 3, REC_IMG_H.toLong(), REC_IMG_W.toLong())
        val tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(data), shape)

        val inputName = recSession!!.inputNames.first()
        val result = recSession!!.run(mapOf(inputName to tensor))
        val outTensor = result.get(0) as OnnxTensor

        val outShape = outTensor.info.shape
        val seqLen = outShape[1].toInt()
        val numClasses = outShape[2].toInt()

        val buf = outTensor.byteBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer()
        val text = ctcGreedyDecode(buf, seqLen, numClasses)

        tensor.close()
        result.close()
        return text
    }

    private fun recImageToChw(bitmap: Bitmap, actualW: Int): FloatArray {
        val pixels = IntArray(actualW * REC_IMG_H)
        bitmap.getPixels(pixels, 0, actualW, 0, 0, actualW, REC_IMG_H)

        val hw = REC_IMG_H * REC_IMG_W
        val data = FloatArray(3 * hw)

        for (y in 0 until REC_IMG_H) {
            for (x in 0 until actualW) {
                val px = pixels[y * actualW + x]
                val r = ((px shr 16) and 0xFF) / 255f
                val g = ((px shr 8) and 0xFF) / 255f
                val b = (px and 0xFF) / 255f

                val idx = y * REC_IMG_W + x
                data[idx]          = (b - 0.5f) / 0.5f
                data[hw + idx]     = (g - 0.5f) / 0.5f
                data[2 * hw + idx] = (r - 0.5f) / 0.5f
            }
        }
        return data
    }

    private fun ctcGreedyDecode(buf: FloatBuffer, seqLen: Int, numClasses: Int): String {
        val sb = StringBuilder()
        var prevIdx = 0

        for (t in 0 until seqLen) {
            var maxIdx = 0
            var maxVal = buf.get(t * numClasses)
            for (c in 1 until numClasses) {
                val v = buf.get(t * numClasses + c)
                if (v > maxVal) {
                    maxVal = v
                    maxIdx = c
                }
            }
            if (maxIdx != 0 && maxIdx != prevIdx) {
                val charIdx = maxIdx - 1
                if (charIdx < dictionary.size) {
                    sb.append(dictionary[charIdx])
                } else {
                    sb.append(' ')
                }
            }
            prevIdx = maxIdx
        }
        return sb.toString().trim()
    }

    private fun detImageToChw(bitmap: Bitmap, w: Int, h: Int): FloatArray {
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val hw = h * w
        val data = FloatArray(3 * hw)

        for (i in pixels.indices) {
            val px = pixels[i]
            val r = ((px shr 16) and 0xFF) / 255f
            val g = ((px shr 8) and 0xFF) / 255f
            val b = (px and 0xFF) / 255f
            data[i]          = (b - DET_MEAN_B) / DET_STD_B
            data[hw + i]     = (g - DET_MEAN_G) / DET_STD_G
            data[2 * hw + i] = (r - DET_MEAN_R) / DET_STD_R
        }
        return data
    }

    private fun loadBitmap(uri: Uri): Bitmap {
        return context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: throw IllegalStateException("Не удалось прочитать изображение")
    }

    override fun release() {
        detSession?.close()
        recSession?.close()
        env?.close()
        detSession = null
        recSession = null
        env = null
        dictionary = emptyList()
    }

    companion object {
        private const val DET_MEAN_B = 0.485f
        private const val DET_MEAN_G = 0.456f
        private const val DET_MEAN_R = 0.406f
        private const val DET_STD_B = 0.229f
        private const val DET_STD_G = 0.224f
        private const val DET_STD_R = 0.225f

        private const val REC_IMG_H = 48
        private const val REC_IMG_W = 320

        private val DIRS = arrayOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
    }
}

