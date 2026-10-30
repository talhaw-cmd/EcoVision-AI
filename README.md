# 🌿 EcoVision AI

### AI-Powered Object Detection for Android

EcoVision AI is an Android application that brings computer vision directly to your smartphone. Powered by a pretrained **YOLOv8n model** and **TensorFlow Lite**, it detects and identifies objects in images and live camera frames without requiring a cloud-based inference API.

The project demonstrates how modern deep learning models can be integrated into native Android applications for efficient, on-device AI inference.

---

## ✨ Features

- **Image-Based Detection:** Select images from your device gallery and detect objects.
- **Live Camera Detection:** Use your device camera for real-time object detection.
- **AI-Powered Recognition:** Identify objects using a pretrained YOLOv8n model.
- **Bounding Box Visualization:** Display detected objects with bounding boxes, labels, and confidence scores.
- **On-Device Inference:** Run the AI model locally using TensorFlow Lite.
- **Offline Inference:** No internet connection or external inference API is required after installation.
- **Optimized Processing:** Apply image preprocessing, confidence filtering, and Non-Maximum Suppression (NMS).

---

## 🧠 AI Model

EcoVision AI uses **YOLOv8n (You Only Look Once — Nano)**, a lightweight object detection model from Ultralytics.

The pretrained model is exported to TensorFlow Lite format and integrated into the Android application.

| Model Specification | Details |
|---|---|
| Model | YOLOv8n |
| Framework | Ultralytics |
| Deployment Format | TensorFlow Lite |
| Input Shape | `1 × 3 × 640 × 640` |
| Input Data Type | Float32 |
| Output Shape | `1 × 84 × 8400` |
| Number of Classes | 80 |
| Inference | On-device |

### Supported Object Categories

The pretrained COCO model recognizes 80 general object categories, including:

- People and animals
- Cars, buses, motorcycles, and bicycles
- Bottles, cups, and other household objects
- Fruits and food items
- Chairs, tables, laptops, and electronic devices
- Sports equipment and everyday objects

> **Note:** EcoVision AI currently uses a general-purpose COCO-trained object detection model. It is not a specialized waste classification system and does not independently classify materials such as plastic, paper, metal, or organic waste.

---

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| Java | Android application development |
| XML | User interface design |
| Android Studio | Development environment |
| TensorFlow Lite | On-device model inference |
| YOLOv8n | Object detection |
| CameraX | Live camera integration |
| Gradle | Dependency and build management |

---

## 🏗️ Project Architecture

```text
EcoVision-AI/
│
├── app/
│   └── src/
│       └── main/
│           │
│           ├── assets/
│           │   └── yolov8n_float32.tflite
│           │
│           ├── java/
│           │   └── com/example/ecovisionai/
│           │       ├── MainActivity.java
│           │       ├── YoloDetector.java
│           │       └── DetectionOverlay.java
│           │
│           ├── res/
│           │   ├── layout/
│           │   │   └── activity_main.xml
│           │   │
│           │   └── values/
│           │       └── strings.xml
│           │
│           └── AndroidManifest.xml
│
└── README.md
```

---

## ⚙️ Getting Started

Follow these instructions to run EcoVision AI locally.

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR-USERNAME/EcoVision-AI.git
```

### 2. Open in Android Studio

1. Launch Android Studio.
2. Select **Open**.
3. Choose the cloned project directory.
4. Allow Gradle to synchronize dependencies.

### 3. Add the TensorFlow Lite Model

The model file is required for inference.

Place the exported model inside:

```text
app/src/main/assets/yolov8n_float32.tflite
```

If the model is not included in the repository, export YOLOv8n to TensorFlow Lite format using Ultralytics and place the generated model at the specified location.

### 4. Build and Run

1. Connect an Android device or launch an emulator.
2. Enable USB debugging if using a physical device.
3. Click **Run** in Android Studio.
4. Grant camera permission when prompted to use live camera detection.

---

## 🔍 How It Works

EcoVision AI follows a computer vision pipeline:

```text
Image / Camera Frame
        |
        v
Image Preprocessing
        |
        v
640 × 640 Input Tensor
        |
        v
TensorFlow Lite Interpreter
        |
        v
YOLOv8n Inference
        |
        v
Output Tensor Decoding
        |
        v
Confidence Filtering
        |
        v
Non-Maximum Suppression
        |
        v
Bounding Boxes + Labels
        |
        v
Detection Results
```

### Detection Pipeline

1. **Image Acquisition:** An image is selected from the gallery or captured through the live camera.
2. **Preprocessing:** The image is resized using letterboxing, converted to RGB, and normalized.
3. **Inference:** TensorFlow Lite executes the YOLOv8n model directly on the device.
4. **Output Decoding:** Model predictions are processed to extract object coordinates and class confidence scores.
5. **Postprocessing:** Confidence thresholding and Non-Maximum Suppression remove low-confidence and overlapping detections.
6. **Visualization:** Detected objects are displayed with bounding boxes and labels.

---

## 🔐 Privacy & Performance

- Image inference is performed locally on the Android device.
- No external AI inference server is required.
- Camera access requires user permission.
- Processing speed depends on the device's hardware and available resources.

---

## 🚀 Future Improvements

- Custom waste classification using a dedicated waste detection dataset.
- GPU acceleration and model optimization.
- Detection history and result summaries.
- Object counting and category-based analytics.
- Improved camera preview and detection alignment.
- Support for additional custom-trained detection models.

---

## 🎯 Project Objectives

This project explores the practical implementation of deep learning in mobile applications.

Key learning objectives include:

- Understanding on-device machine learning.
- Converting pretrained models into TensorFlow Lite format.
- Integrating machine learning models into Android applications.
- Implementing image preprocessing and inference.
- Understanding object detection outputs and postprocessing.
- Working with Android camera APIs.

---

## 👨‍💻 Developer

**Muhammad Talha Rauf**

BS Computer Science Student | Android Developer | AI & Machine Learning Enthusiast

- GitHub: [@talhaw-cmd](https://github.com/talhaw-cmd)

---

## 📌 Disclaimer

EcoVision AI is an educational and practical implementation of on-device object detection. Detection accuracy and performance may vary depending on image quality, lighting conditions, device hardware, and the pretrained model's capabilities.

The project uses a pretrained YOLOv8n model and does not claim custom model training.

---

**Built with curiosity, code, and AI. 🌿**