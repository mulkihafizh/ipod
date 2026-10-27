#include <jni.h>
#include <oboe/Oboe.h>
#include <android/log.h>
#include <memory>

#define TAG "RetroIpodAudio"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static std::shared_ptr<oboe::AudioStream> sAudioStream = nullptr;

extern "C" JNIEXPORT jstring JNICALL
Java_com_retro_ipod_playback_audio_NativeAudioBridge_getOboeVersion(JNIEnv *env, jobject /* this */) {
    return env->NewStringUTF(oboe::getVersionText());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_retro_ipod_playback_audio_NativeAudioBridge_initAudioStream(
    JNIEnv *env, jobject /* this */,
    jint sampleRate, jint channelCount, jint bitDepth) {

    if (sAudioStream != nullptr) {
        sAudioStream->close();
        sAudioStream = nullptr;
    }

    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
           ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
           ->setSharingMode(oboe::SharingMode::Exclusive) // Exclusive Bit-Perfect mode bypasses AudioFlinger
           ->setSampleRate(sampleRate)
           ->setChannelCount(channelCount);

    if (bitDepth == 24 || bitDepth == 32) {
        builder.setFormat(oboe::AudioFormat::Float);
    } else {
        builder.setFormat(oboe::AudioFormat::I16);
    }

    oboe::Result result = builder.openStream(sAudioStream);
    if (result != oboe::Result::OK) {
        LOGE("Failed to open Oboe audio stream: %s", oboe::convertToText(result));
        return JNI_FALSE;
    }

    result = sAudioStream->requestStart();
    if (result != oboe::Result::OK) {
        LOGE("Failed to start Oboe audio stream: %s", oboe::convertToText(result));
        sAudioStream->close();
        sAudioStream = nullptr;
        return JNI_FALSE;
    }

    LOGI("Exclusive Bit-Perfect stream started at %d Hz, %d channels", sampleRate, channelCount);
    return JNI_TRUE;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_retro_ipod_playback_audio_NativeAudioBridge_writePcmBuffer(
    JNIEnv *env, jobject /* this */,
    jbyteArray buffer, jint sizeInBytes) {

    if (!sAudioStream) return -1;

    jbyte *bytes = env->GetByteArrayElements(buffer, nullptr);
    int32_t bytesWritten = 0;

    if (sAudioStream->getFormat() == oboe::AudioFormat::I16) {
        int32_t framesToWrite = sizeInBytes / (sAudioStream->getChannelCount() * sizeof(int16_t));
        auto result = sAudioStream->write(bytes, framesToWrite, 0);
        if (result.hasValue()) {
            bytesWritten = result.value() * (sAudioStream->getChannelCount() * sizeof(int16_t));
        }
    }

    env->ReleaseByteArrayElements(buffer, bytes, JNI_ABORT);
    return bytesWritten;
}

extern "C" JNIEXPORT void JNICALL
Java_com_retro_ipod_playback_audio_NativeAudioBridge_closeAudioStream(JNIEnv *env, jobject /* this */) {
    if (sAudioStream) {
        sAudioStream->stop();
        sAudioStream->close();
        sAudioStream = nullptr;
        LOGI("Oboe audio stream closed");
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_retro_ipod_playback_audio_NativeAudioBridge_getHardwareSampleRate(JNIEnv *env, jobject /* this */) {
    if (sAudioStream) {
        return sAudioStream->getSampleRate();
    }
    return 0;
}
