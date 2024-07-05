package com.example.eyesup_application

data class BoundingBox(
    val x1: Float,  // X-coordinate of the top-left corner
    val y1: Float,  // Y-coordinate of the top-left corner
    val x2: Float,  // X-coordinate of the bottom-right corner
    val y2: Float,  // Y-coordinate of the bottom-right corner
    val cx: Float,  // X-coordinate of the center
    val cy: Float,  // Y-coordinate of the center
    val w: Float,  // Width of the bounding box
    val h: Float,  // Height of the bounding box
    val cnf: Float,  // Confidence score of the detection
    val cls: Int,  // Class ID of the detected object
    val clsName: String  // Class name of the detected object
)
