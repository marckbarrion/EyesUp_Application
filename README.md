# EyesUp Application

EyesUp is an Android application that uses YOLOv5 for real-time object detection to detect people, heads, and cellphones. The app warns people when they are using their cellphones while walking in hallways by turning the bounding boxes red if a cellphone is detected inside or near a head bounding box.

## Features

- Real-time object detection using YOLOv5n
- Detects and tracks people, heads, and cellphones
- Turns bounding boxes red when a cellphone is inside or near a head bounding box
- Displays inference time on the screen

## Installation

1. Clone the repository:
    ```sh
    git clone https://github.com/marckbarrion/EyesUp_Application.git
    ```

2. Open the project in Android Studio.

3. Sync the project with Gradle files.

4. Build and run the project on your Android device or emulator.

## Usage

1. Grant the necessary permissions (Camera).
2. The app will start the camera and begin real-time object detection.
3. If a cellphone is detected inside or near a head bounding box, both bounding boxes will turn red to alert the user.

## Project Structure

- `MainActivity.kt`: Main activity handling camera setup, image analysis, and detection.
- `BoundingBox.kt`: Data class representing the bounding boxes for detected objects.
- `Detector.kt`: Class for setting up and running the YOLOv5 object detector.
- `OverlayView.kt`: Custom view to draw bounding boxes on the camera preview.
- `Sort.kt`: Implementation of the SORT tracking algorithm and proximity check logic.

## Dependencies

- [CameraX](https://developer.android.com/training/camerax) for camera operations
- [YOLOv5](https://github.com/ultralytics/yolov5) for object detection
- Android SDK


## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contact

For questions or suggestions, feel free to contact the project maintainer
