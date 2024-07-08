package com.example.eyesup_application

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

    fun update(detections: List<Detection>): List<Track> {
        val newTracks = mutableListOf<Track>()
        for (det in detections) {
            var matched = false
            for (trk in tracks) {
                if (iou(det, trk) > 0.3) { // IoU threshold for matching
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
}
