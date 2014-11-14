/*
 * Copyright (C) 2014 ParanoidAndroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <jni/com_android_systemui_statusbar_phone_BarBackgroundUpdaterNative.h>

#define LOG_TAG "BarBackgroundUpdaterNative"
#define DEBUG_FLOOD false

#define SHOT_SCALE 1
#define ROTATION_0 0
#define ROTATION_90 1
#define ROTATION_180 2
#define ROTATION_270 3

#include <gui/ISurfaceComposer.h>
#include <gui/SurfaceComposerClient.h>
#include <utils/Log.h>

using namespace android;

int screenRotation;

void const * shotBase;

uint32_t shotWidth;
uint32_t shotHeight;
uint32_t shotStride;
PixelFormat shotFormat;

uint32_t requestedShotWidth = 0;
uint32_t requestedShotHeight = 0;

uint32_t sampleColors(int n, uint32_t sources[])
{
    float red = 0;
    float green = 0;
    float blue = 0;

    int i;
    for (i = 0; i < n; i++)
    {
        uint32_t color = sources[i];
        red += ((color & 0x00FF0000) >> 16) / n;
        green += ((color & 0x0000FF00) >> 8) / n;
        blue += (color & 0x000000FF) / n;
    }

    return (255 << 24) | (((char) red) << 16) | (((char) green) << 8) | ((char) blue);
}

uint32_t getPixel(int32_t dx, int32_t dy)
{
    dx = (uint32_t) (dx * SHOT_SCALE);
    dy = (uint32_t) (dy * SHOT_SCALE);

    uint32_t x = 0;
    uint32_t y = 0;

    switch (screenRotation)
    {
    case ROTATION_90:
        // turned counter-clockwise;  invert some of the things
        x = (dy >= 0) ? (shotWidth - 1 - dy) : -dy;
        y = (dx >= 0) ? dx : (shotHeight - 1 + dx);
        break;
    case ROTATION_180:
        // turned upside down; invert all the things
        x = (dx >= 0) ? (shotWidth - 1 - dx) : -dx;
        y = (dy >= 0) ? (shotHeight - 1 - dy) : -dy;
        break;
    case ROTATION_270:
        // turned clockwise; invert some of the things
        x = (dy >= 0) ? dy : (shotWidth - 1 + dy);
        y = (dx >= 0) ? (shotHeight - 1 - dx) : -dx;
        break;
    case ROTATION_0:
    default: // Just smile and wave, boys. Smile and wave.
        // natural orientation; don't invert anything
        x = (dx >= 0) ? dx : (shotWidth - 1 + dx);
        y = (dy >= 0) ? dy : (shotHeight - 1 + dy);
        break;
    }

    if (x >= shotWidth)
    {
        x = shotWidth - 1;
    }

    if (y >= shotHeight)
    {
        y = shotHeight - 1;
    }

    if (shotFormat == PIXEL_FORMAT_RGBA_8888)
    {
        // this is stored as BGRA behind the scenes, it seems
        // wonders of interacting with the lower level

        uint32_t color = * (uint32_t *) (((char *) shotBase) + y * shotStride * 4 + x * 4);

        char blue = (color & 0x00FF0000) >> 16;
        char green = (color & 0x0000FF00) >> 8;
        char red = color & 0x000000FF;

        return (255 << 24) | (red << 16) | (green << 8) | blue;
    }
    else if (shotFormat == PIXEL_FORMAT_RGB_565)
    {
        uint16_t color = * (uint16_t *) (((char *) shotBase) + y * shotStride * 2 + x * 2);

        uint32_t red = ((color & 0xF800) >> 11) * 255 / 31;
        uint32_t green = ((color & 0x07E0) >> 5) * 255 / 63;
        uint32_t blue = (color & 0x001F) * 255 / 31;

        return (255 << 24) | (red << 16) | (green << 8) | blue;
    }

    return 0;
}

JNIEXPORT void JNICALL Java_com_android_systemui_statusbar_phone_BarBackgroundUpdaterNative_setScreenSize
        (JNIEnv * je, jclass jc, jint rotation, jint width, jint height)
{
    screenRotation = rotation;

    bool isNatural = rotation != ROTATION_90 && rotation != ROTATION_270;
    requestedShotWidth = (isNatural ? width : height) * SHOT_SCALE;
    requestedShotHeight = (isNatural ? height : width) * SHOT_SCALE;
}

JNIEXPORT jintArray JNICALL Java_com_android_systemui_statusbar_phone_BarBackgroundUpdaterNative_getColors
        (JNIEnv * je, jclass jc, jint rotation, jint statusBarHeight, jint navigationBarHeight, jint xFromRightSide)
{
    jint response[4] = { 0, 0, 0, 0 };

    sp<IBinder> display = SurfaceComposerClient::getBuiltInDisplay(ISurfaceComposer::eDisplayIdMain);
    ScreenshotClient screenshot;

    if (display == NULL)
    {
        jintArray arr = je->NewIntArray(4);
        je->SetIntArrayRegion(arr, 0, 4, response);
        return arr;
    }

    screenRotation = rotation;

    if (screenshot.update(display, SHOT_SCALE == 1 ? 0 : requestedShotWidth, SHOT_SCALE == 1 ? 0 : requestedShotHeight, 0, -1UL) != NO_ERROR)
    {
        jintArray arr = je->NewIntArray(4);
        je->SetIntArrayRegion(arr, 0, 4, response);
        return arr;
    }

    shotBase = screenshot.getPixels();

    if (shotBase == NULL)
    {
        jintArray arr = je->NewIntArray(4);
        je->SetIntArrayRegion(arr, 0, 4, response);
        return arr;
    }

    shotWidth = screenshot.getWidth();
    shotHeight = screenshot.getHeight();
    shotStride = screenshot.getStride();
    shotFormat = screenshot.getFormat();

    int fsbh = 2 + statusBarHeight;
    uint32_t colorTopLeft = getPixel(1, fsbh);
    uint32_t colorTopLeftPadding = getPixel(1 + 10, fsbh);
    uint32_t colorTopRight = getPixel(-1 - xFromRightSide, fsbh);
    uint32_t colorTopRightPadding = getPixel(-1 - xFromRightSide - 10, fsbh);

    if (colorTopLeft == colorTopRight)
    {
        // status bar appears to be completely uniform
        response[0] = colorTopLeft;
    }
    else if (colorTopRightPadding == colorTopRight)
    {
        // the right side of the status bar appears to be uniform
        response[0] = colorTopRight;
    }
    else if (colorTopLeftPadding == colorTopLeft)
    {
        // the left side of the status bar appears to be uniform
        response[0] = colorTopLeft;
    }
    else
    {
        // status bar does not appear to be uniform at all
        uint32_t colorsTop[4] = { colorTopLeft, colorTopLeftPadding, colorTopRight, colorTopRightPadding };
        response[0] = sampleColors(4, colorsTop);
    }

    response[1] = getPixel(1, 1) == getPixel(1, 5) ? 1 : 0;

    int fnbh = -2 - navigationBarHeight;
    uint32_t colorBotLeft = getPixel(1, fnbh);
    uint32_t colorBotLeftPadding = getPixel(1 + 10, fnbh);
    uint32_t colorBotRight = getPixel(-1 - xFromRightSide, fnbh);
    uint32_t colorBotRightPadding = getPixel(-1 - xFromRightSide - 10, fnbh);

    if (colorBotLeft == colorBotRight)
    {
        // navigation bar appears to be completely uniform
        response[2] = colorBotLeft;
    }
    else if (colorBotRightPadding == colorBotRight)
    {
        // the right side of the navigation bar appears to be uniform
        response[2] = colorBotRight;
    }
    else if (colorBotLeftPadding == colorBotLeft)
    {
        // the left side of the navigation bar appears to be uniform
        response[2] = colorBotLeft;
    }
    else
    {
        // navigation bar does not appear to be uniform at all
        uint32_t colorsBot[4] = { colorBotLeft, colorBotLeftPadding, colorBotRight, colorBotRightPadding };
        response[2] = sampleColors(4, colorsBot);
    }

    response[3] = getPixel(-1, -1) == getPixel(-1, -5) ? 1 : 0;

    if (DEBUG_FLOOD) {
        ALOGD("width=%d height=%d tl=%d tlp=%d tr=%d trp=%d bl=%d blp=%d br=%d brp=%d",
                shotWidth, shotHeight, colorTopLeft, colorTopLeftPadding, colorTopRight, colorTopRightPadding,
                colorBotLeft, colorBotLeftPadding, colorBotRight, colorBotRightPadding);
    }

    jintArray arr = je->NewIntArray(4);
    je->SetIntArrayRegion(arr, 0, 4, response);

    return arr;
}
