package com.example.eyesup_application

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class Detector (
    private val context: Context,  // Application context
    private val modelPath: String,  // Path to the model file
    private val labelPath: String,  // Path to the labels file
    private val detectorListener: DetectorListener  // Listener for detection results
) {

    private var interpreter: Interpreter? = null  // TFLite interpreter
    private var labels = mutableListOf<String>()  // List of labels

    private var tensorWidth = 0  // Width of the input tensor
    private var tensorHeight = 0  // Height of the input tensor
    private var numElements = 0  // Number of output elements

    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))  // Normalize image
        .add(CastOp(INPUT_IMAGE_TYPE))  // Cast image type
        .build()

    private val tracker = Sort()  // Instantiate SORT tracker

    fun setup() {
        val model = FileUtil.loadMappedFile(context, modelPath)  // Load TFLite model
        val options = Interpreter.Options()
        options.numThreads = 4  // Set number of threads for inference
        interpreter = Interpreter(model, options)  // Initialize interpreter

        val inputShape = interpreter?.getInputTensor(0)?.shape() ?: return  // Get input tensor shape
        val outputShape = interpreter?.getOutputTensor(0)?.shape() ?: return  // Get output tensor shape

        tensorWidth = inputShape[1]  // Set tensor width
        tensorHeight = inputShape[2]  // Set tensor height
        numElements = outputShape[1]  // Set number of elements in output tensor

        try {
            val inputStream: InputStream = context.assets.open(labelPath)  // Open labels file
            val reader = BufferedReader(InputStreamReader(inputStream))

            var line: String? = reader.readLine()
            while (line != null && line != "") {
                labels.add(line)  // Read and add labels to list
                line = reader.readLine()
            }

            reader.close()
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()  // Handle IOException
        }
    }

    fun clear() {
        interpreter?.close()  // Close interpreter
        interpreter = null
    }

    fun detect(frame: Bitmap) {
        interpreter ?: return  // Return if interpreter is null
        if (tensorWidth == 0) return  // Return if tensor width is 0
        if (tensorHeight == 0) return  // Return if tensor height is 0
        if (numElements == 0) return  // Return if numElements is 0

        var inferenceTime = SystemClock.uptimeMillis()  // Start timing inference

        val resizedBitmap = Bitmap.createScaledBitmap(frame, tensorWidth, tensorHeight, false)  // Resize frame

        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(resizedBitmap)  // Load bitmap into TensorImage
        val processedImage = imageProcessor.process(tensorImage)  // Process image
        val imageBuffer = processedImage.buffer  // Get image buffer

        val output = TensorBuffer.createFixedSize(intArrayOf(1, numElements, 8), OUTPUT_IMAGE_TYPE)  // Create output buffer
        interpreter?.run(imageBuffer, output.buffer)  // Run model inference

        val bestBoxes = bestBox(output.floatArray)  // Get best bounding boxes
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime  // Calculate inference time

        if (bestBoxes == null) {
            detectorListener.onEmptyDetect()  // Notify listener if no boxes detected
            return
        }

        // Track detections
        val detections = bestBoxes.map {
            Sort.Detection(it.x1, it.y1, it.x2, it.y2, it.cnf, it.cls)
        }
        val trackedBoxes = tracker.update(detections)

        // Convert tracked boxes back to BoundingBox with trackId
        val trackedBoundingBoxes = trackedBoxes.map {
            BoundingBox(it.x1, it.y1, it.x2, it.y2, it.x1 + (it.x2 - it.x1) / 2, it.y1 + (it.y2 - it.y1) / 2, it.x2 - it.x1, it.y2 - it.y1, it.score, it.cls, labels[it.cls], it.id)
        }

        detectorListener.onDetect(trackedBoundingBoxes, inferenceTime)  // Notify listener with detection results
    }

    private fun bestBox(array: FloatArray): List<BoundingBox>? {
        val boundingBoxes = mutableListOf<BoundingBox>()

        for (i in array.indices step 8) {
            val cx = array[i]
            val cy = array[i + 1]
            val w = array[i + 2]
            val h = array[i + 3]
            val conf = array[i + 4]
            val classes = array.copyOfRange(i + 5, i + 8)

            if (conf > CONFIDENCE_THRESHOLD) {
                val clsIdx = classes.indices.maxByOrNull { classes[it] } ?: continue
                val clsName = labels[clsIdx]

                val x1 = cx - w / 2
                val y1 = cy - h / 2
                val x2 = cx + w / 2
                val y2 = cy + h / 2

                if (x1 < 0 || y1 < 0 || x2 > 1 || y2 > 1) continue

                boundingBoxes.add(
                    BoundingBox(
                        x1 = x1,
                        y1 = y1,
                        x2 = x2,
                        y2 = y2,
                        cx = cx,
                        cy = cy,
                        w = w,
                        h = h,
                        cnf = conf,
                        cls = clsIdx,
                        clsName = clsName,
                        trackId = -1  // Temporary trackId placeholder
                    )
                )
            }
        }

        if (boundingBoxes.isEmpty()) return null

        return applyNMS(boundingBoxes)  // Apply Non-Maximum Suppression
    }

    private fun applyNMS(boxes: List<BoundingBox>): MutableList<BoundingBox> {
        val sortedBoxes = boxes.sortedByDescending { it.cnf }.toMutableList()  // Sort boxes by confidence
        val selectedBoxes = mutableListOf<BoundingBox>()

        while (sortedBoxes.isNotEmpty()) {
            val first = sortedBoxes.first()
            selectedBoxes.add(first)  // Select the highest confidence box
            sortedBoxes.remove(first)

            val iterator = sortedBoxes.iterator()
            while (iterator.hasNext()) {
                val nextBox = iterator.next()
                val iou = calculateIoU(first, nextBox)  // Calculate Intersection over Union
                if (iou >= IOU_THRESHOLD) {
                    iterator.remove()  // Remove boxes with high overlap
                }
            }
        }

        return selectedBoxes
    }

    private fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
        val x1 = maxOf(box1.x1, box2.x1)
        val y1 = maxOf(box1.y1, box2.y1)
        val x2 = minOf(box1.x2, box2.x2)
        val y2 = minOf(box1.y2, box2.y2)
        val intersectionArea = maxOf(0F, x2 - x1) * maxOf(0F, y2 - y1)  // Calculate intersection area
        val box1Area = box1.w * box1.h
        val box2Area = box2.w * box2.h
        return intersectionArea / (box1Area + box2Area - intersectionArea)  // Calculate IoU
    }

    interface DetectorListener {
        fun onEmptyDetect()  // Callback for empty detection
        fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long)  // Callback for detection results
    }

    companion object {
        private const val INPUT_MEAN = 0f  // Input normalization mean
        private const val INPUT_STANDARD_DEVIATION = 255f  // Input normalization standard deviation
        private val INPUT_IMAGE_TYPE = DataType.FLOAT32  // Input image data type
        private val OUTPUT_IMAGE_TYPE = DataType.FLOAT32  // Output image data type
        private const val CONFIDENCE_THRESHOLD = 0.3F  // Confidence threshold for detection
        private const val IOU_THRESHOLD = 0.5F  // IoU threshold for NMS
    }
}
