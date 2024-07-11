package com.example.eyesup_application

import android.graphics.Color  // Import Color class
import kotlin.math.max
import kotlin.math.min

class Sort {
    private var tracks = mutableListOf<Track>()
    private var trackId = 0

    data class Detection(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val score: Float, val cls: Int)

    data class Track(val id: Int, var x1: Float, var y1: Float, var x2: Float, var y2: Float, var cls: Int, var score: Float)

    private fun iou(det: Detection, trk: Track): Float {
        val xx1 = max(det.x1, trk.x1)
        val yy1 = max(det.y1, trk.y1)
        val xx2 = min(det.x2, trk.x2)
        val yy2 = min(det.y2, trk.y2)
        val w = max(0f, xx2 - xx1)
        val h = max(0f, yy2 - yy1)
        val intersection = w * h
        val union = (det.x2 - det.x1) * (det.y2 - det.y1) + (trk.x2 - trk.x1) * (trk.y2 - trk.y1) - intersection
        return intersection / union
    }

    private fun isPhoneNearHead(phone: Detection, head: Detection): Boolean {
        val headCenterX = (head.x1 + head.x2) / 2
        val headCenterY = (head.y1 + head.y2) / 2
        val phoneCenterX = (phone.x1 + phone.x2) / 2
        val phoneCenterY = (phone.y1 + phone.y2) / 2

        // Calculate distance between the center of the phone and the head
        val distance = Math.sqrt(((phoneCenterX - headCenterX) * (phoneCenterX - headCenterX) + (phoneCenterY - headCenterY) * (phoneCenterY - headCenterY)).toDouble())
        val headHeight = head.y2 - head.y1  // Height of the head bounding box

        // Check if the distance is less than the height of the head bounding box
        return distance < headHeight
    }

    fun update(detections: List<Detection>): List<Track> {
        val newTracks = mutableListOf<Track>()
        for (det in detections) {
            var matched = false
            for (trk in tracks) {
                if (iou(det, trk) > 0.3) {  // IoU threshold for matching
                    trk.x1 = det.x1
                    trk.y1 = det.y1
                    trk.x2 = det.x2
                    trk.y2 = det.y2
                    trk.score = det.score
                    trk.cls = det.cls
                    matched = true
                    newTracks.add(trk)
                    break
                }
            }
            if (!matched) {
                newTracks.add(Track(trackId++, det.x1, det.y1, det.x2, det.y2, det.cls, det.score))
            }
        }
        tracks = newTracks
        return tracks
    }

    // Method to check if a phone is inside or near a head bounding box
    fun checkProximity(detections: List<BoundingBox>): List<BoundingBox> {
        detections.forEach { bbox ->
            if (bbox.clsName == "cellphone") {
                detections.forEach { headBox ->
                    if (headBox.clsName == "head" &&
                        (iou(Detection(bbox.x1, bbox.y1, bbox.x2, bbox.y2, bbox.cnf, bbox.cls), Track(0, headBox.x1, headBox.y1, headBox.x2, headBox.y2, headBox.cls, headBox.cnf)) > 0.3 ||
                                isPhoneNearHead(Detection(bbox.x1, bbox.y1, bbox.x2, bbox.y2, bbox.cnf, bbox.cls), Detection(headBox.x1, headBox.y1, headBox.x2, headBox.y2, headBox.cnf, headBox.cls)))
                    ) {
                        bbox.color = Color.RED  // Change color to red if phone is inside or near head
                        headBox.color = Color.RED  // Change color to red if phone is inside or near head
                    }
                }
            }
        }
        return detections
    }
}
