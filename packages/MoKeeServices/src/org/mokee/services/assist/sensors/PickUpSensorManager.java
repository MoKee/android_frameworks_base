/*
 * Copyright (C) 2015 The MoKee OpenSource Project
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

package org.mokee.services.assist.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.FloatMath;

public class PickUpSensorManager {

    public interface PickUpSensorListener {
        public void onPickup();
    }

    private final PickUpSensorEventListener mPickUpSensorListener;

    private boolean mManagerEnabled;

    private class PickUpSensorEventListener implements SensorEventListener {

        private float[] mGravity;
        private float mAccel = 0.00f;
        private float mAccelCurrent = SensorManager.GRAVITY_EARTH;
        private float mAccelLast = SensorManager.GRAVITY_EARTH;

        private final SensorManager mSensorManager;
        private final Sensor mAcceleroMeter;
        private final PickUpSensorListener mListener;

        public PickUpSensorEventListener(SensorManager sensorManager, Sensor acceleroMeter,
                PickUpSensorListener listener) {
            mSensorManager = sensorManager;
            mAcceleroMeter = acceleroMeter;
            mListener = listener;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                mGravity = event.values.clone();
                // Shake detection
                float x = mGravity[0];
                float y = mGravity[1];
                float z = mGravity[2];
                mAccelLast = mAccelCurrent;
                mAccelCurrent = FloatMath.sqrt(x * x + y * y + z * z);
                float delta = mAccelCurrent - mAccelLast;
                mAccel = mAccel * 0.9f + delta;
                // Make this higher or lower according to how much
                // motion you want to detect
                if (mAccel > 3) {
                    mListener.onPickup();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void register() {
            registerSensorListener(mAcceleroMeter);
        }

        private void registerSensorListener(Sensor sensor) {
            if (sensor != null) {
                mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
            }
        }

        private void unregisterSensorListener(Sensor sensor) {
            if (sensor != null) {
                mSensorManager.unregisterListener(this, sensor);
            }
        }

        public void unregister() {
            unregisterSensorListener(mAcceleroMeter);
        }
    }

    public PickUpSensorManager(Context context, PickUpSensorListener listener) {
        SensorManager sensorManager =
                (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor acceleroMeter = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (acceleroMeter == null) {
            mPickUpSensorListener = null;
        } else {
            mPickUpSensorListener =
                    new PickUpSensorEventListener(sensorManager, acceleroMeter, listener);
        }
    }

    public void enable() {
        if (mPickUpSensorListener != null && !mManagerEnabled) {
            mPickUpSensorListener.register();
            mManagerEnabled = true;
        }
    }

    public void disable() {
        if (mPickUpSensorListener != null && mManagerEnabled) {
            mPickUpSensorListener.unregister();
            mManagerEnabled = false;
        }
    }
}
