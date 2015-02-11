/*
 * Copyright (C) 2014-2015 The MoKee OpenSource Project
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

package org.mokee.services.assist.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;

public class MediaPlayerController {
    private static MediaPlayer mMediaPlayer;

    // 音频播放控制
    public static void startSound(Context mContext) {
        cleanupMediaPlayer();
        AudioManager mAudioManager = (AudioManager) mContext
                .getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
        mMediaPlayer = createMediaPlayer(mContext);
        if (!mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
        }
    }

    private static MediaPlayer createMediaPlayer(Context mContext) {
        String filePath = "file:///system/media/audio/ringtones/Umbriel.ogg";
        return MediaPlayer.create(mContext, Uri.parse(filePath));
    }

    public static void cleanupMediaPlayer() {
        if (mMediaPlayer != null) {
            try {
                mMediaPlayer.stop();
                mMediaPlayer.release();
            } finally {
                mMediaPlayer = null;
            }
        }
    }
}
