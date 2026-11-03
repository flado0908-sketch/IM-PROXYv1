#include <jni.h>
#include <opencv2/imgproc.hpp>
#include <opencv2/core.hpp>
#include <vector>

using namespace cv;

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_im_proxy_Detector_detect(JNIEnv* env, jobject, jbyteArray arr, jint w, jint h) {
    jbyte* p = env->GetByteArrayElements(arr, nullptr);
    Mat rgba(h, w, CV_8UC4, (void*)p);
    Mat bgr; cvtColor(rgba, bgr, COLOR_RGBA2BGR);
    Mat hsv; cvtColor(bgr, hsv, COLOR_BGR2HSV);

    Mat mask;
    inRange(hsv, Scalar(0, 40, 40), Scalar(30, 255, 255), mask);
    erode(mask, mask, getStructuringElement(MORPH_RECT, Size(3,3)));
    dilate(mask, mask, getStructuringElement(MORPH_RECT, Size(5,5)));

    std::vector<std::vector<Point>> cnts;
    findContours(mask, cnts, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

    std::vector<float> out;
    for (auto& c : cnts) {
        double a = contourArea(c);
        if (a < 800) continue;
        Rect r = boundingRect(c);
        float ar = (float)r.height / r.width;
        if (r.height < h * 0.04f || r.height > h * 0.5f) continue;
        if (ar < 1.2f) continue;

        out.push_back(r.x);
        out.push_back(r.y);
        out.push_back(r.width);
        out.push_back(r.height);
        out.push_back(0);

        float hh = r.height * 0.30f;
        out.push_back(r.x + r.width * 0.25f);
        out.push_back(r.y);
        out.push_back(r.width * 0.5f);
        out.push_back(hh);
        out.push_back(1);
    }
    env->ReleaseByteArrayElements(arr, p, JNI_ABORT);

    int n = (int)(out.size() / 5);
    jfloatArray result = env->NewFloatArray(1 + out.size());
    std::vector<float> fin;
    fin.push_back((float)n);
    for (float f : out) fin.push_back(f);
    env->SetFloatArrayRegion(result, 0, fin.size(), fin.data());
    return result;
}
