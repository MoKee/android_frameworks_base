/*
 * Copyright (C) 2014 NXP Semiconductors
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

/**
 * \addtogroup spi_framework
 *
 * @{ */
package com.nxp.ese.spi;

import android.content.pm.IPackageManager;
import android.app.Activity;
import android.app.ActivityThread;
import java.util.HashMap;
import android.content.Context;
import java.io.IOException;
import android.os.IBinder;
import android.util.Log;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;

/**
 * Represents the local ESE-SPI adapter.
 * <p>
 * Use the helper {@link #getSpiAdapter(Context)} to get the default ESE-SPI
 * adapter for this Android device.
 *
 */
/** @hide */
public final class EseSpiAdapter {

    static final String TAG = "SPI";
    final Context mContext;

    // Guarded by SpiAdapter.class
    static boolean sIsInitialized = false;

    /*Initial/Default SPI service adapter state OFF*/
    public static final int STATE_OFF = 1;
    /*When SPI service adapter state is ON*/
    public static final int STATE_ON = 3;
    /*When SPI service adapter state is TURNING_ON*/
    public static final int STATE_TURNING_ON = 2;
    /*When SPI service adapter state is TURNING_OFF*/
    public static final int STATE_TURNING_OFF = 4;


    static IEseSpiAdapter eseSpiService;
    static EseSpiAdapter sNullContextSpiAdapter;

    /**
     * Broadcast Action: The state of the local SPI adapter has been
     * changed.
     * <p>For example, SPI has been turned on or off.
     * <p>Always contains the extra field {@link #EXTRA_ADAPTER_STATE}
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_ADAPTER_STATE_CHANGED = "android.ese.spi.action.ADAPTER_STATE_CHANGED";

    /**
     * Used as an int extra field in {@link #ACTION_ADAPTER_STATE_CHANGED}
     * intents to request the current power state. Possible values are:
     * Initial/Default SPI service adapter state OFF=1  {@link #STATE_OFF},
     * When SPI service adapter state is ON=3 {@link #STATE_TURNING_ON},
     * When SPI service adapter state is TURNING_ON=2 {@link #STATE_ON},
     * When SPI service adapter state is TURNING_OFF=4 {@link #STATE_TURNING_OFF}
     */
    public static final String EXTRA_ADAPTER_STATE = "android.ese.spi.extra.ADAPTER_STATE";


    static HashMap<Context, EseSpiAdapter> sSpiAdapters = new HashMap();

    EseSpiAdapter(Context context) {
        mContext = context;
    }

    /** get handle to SPI service interface */
    private static IEseSpiAdapter getServiceInterface() {
        /* get a handle to SPI service */
        IBinder b = ServiceManager.getService("spi");
        if (b == null) {
            return null;
        }
        return IEseSpiAdapter.Stub.asInterface(b);
    }

    public void attemptDeadServiceRecovery(Exception e) {
        Log.e(TAG, "SPI service dead - attempting to recover", e);
        IEseSpiAdapter service = getServiceInterface();
        if (service == null) {
            Log.e(TAG, "could not retrieve SPI service during service recovery");
            // nothing more can be done now, sService is still stale, we'll hit
            // this recovery path again later
            return;
        }
        // assigning to sService is not thread-safe, but this is best-effort code
        // and on a well-behaved system should never happen
        eseSpiService = service;

        return;
    }

    /**
     * Helper to check if this device has FEATURE_SPI, but without using
     * a context.
     * Equivalent to
     * context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SPI)
     */
   /**
    private static boolean hasSpiFeature() {
        IPackageManager pm = ActivityThread.getPackageManager();
        if (pm == null) {
            Log.e(TAG, "Cannot get package manager, assuming no SPI feature");
            return false;
        }
        try {
            return pm.hasSystemFeature(PackageManager.FEATURE_SPI);
        } catch (RemoteException e) {
            Log.e(TAG, "Package manager query failed, assuming no SPI feature", e);
            return false;
        }
    }
    */

    /**
     * Helper to get the default ESE-SPI Adapter.
     * <p>
     * Most Android devices will only have one ESE-SPI Adapter.
     * <p>
     * This helper is the equivalent of:
     *
     * @param context the calling application's context
     *
     * @return the default ESE-SPI adapter, or null if no ESE-SPI adapter exists
     */
    public static synchronized EseSpiAdapter getSpiAdapter(Context context) {
        if (!sIsInitialized) {

            //if (!hasSpiFeature()) {
            //      Log.v(TAG, "this device does not have SPI support");
            //  throw new UnsupportedOperationException();
            //  }

            eseSpiService = getServiceInterface();
            if (eseSpiService == null) {
                Log.e(TAG, "could not retrieve SPI service");
                throw new UnsupportedOperationException();
            }

            sIsInitialized = true;
        }
        if (context == null) {
            if (sNullContextSpiAdapter == null) {
                //sNullContextSpiAdapter = new c(null);
            }
            return sNullContextSpiAdapter;
        }
        EseSpiAdapter adapter = sSpiAdapters.get(context);
        if (adapter == null) {
            adapter = new EseSpiAdapter(context);
            sSpiAdapters.put(context, adapter);
        }
        return adapter;

    }

    /**
     * Send ISO7816-3 APDU frame to the ESE over SPI interface and receive the response.
     *
     * <p>Applications must send the entire APDU frame. Applications do not need to
     * fragment the payload, it will be automatically fragmented and reassembled by
     * lower layer.
     *
     * <p>This is an I/O operation and will block until complete. It must
     * not be called from the main application thread.
     *
     * @param data command bytes to send, must not be null
     * @return response bytes received, will not be null
     * @throws IOException if there is an I/O failure, or this operation is canceled.
     */
    public byte[] exchangeData(String pkg, byte[] data) throws IOException {

        try {
            if(eseSpiService !=null) {
                Log.d(TAG,"spiService present  " + eseSpiService);
            }
            byte[] result = eseSpiService.transceive(pkg,data);
            return result;
        } catch (RemoteException e) {
            Log.e(TAG, "tranceive failed", e);
            throw new IOException("tranceive failed");
        }
    }

    /**
     * Return true if this ESE-SPI Adapter has any features enabled.
     *
     * <p>If this method returns false, the ESE-SPI hardware is guaranteed not to
     * generate or respond to any SPI communication over ESE.
     * <p>Applications can use this to check if ESE-SPI is enabled.
     *
     * @return true if this ESE-SPI Adapter has any features enabled
     */
    public boolean isEnabled() {
        try {
            if(eseSpiService.getState() == 3) {
                return true;
            } else {
                return false;
            }
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            Log.e(TAG, "Enable failed", e);
            return false;
        }
    }

    /**
     * Reset ESE-SPI hardware.
     *
     * <p>This call is asynchronous. Listen for
     * {@link #ACTION_ADAPTER_STATE_CHANGED} broadcasts to find out when the
     * operation is complete.
     *
     * <p>If this returns true, then either ESE-SPI is already on, or
     * a {@link #ACTION_ADAPTER_STATE_CHANGED} broadcast will be sent
     * to indicate a state transition. If this returns false, then
     * there is some problem that prevents an attempt to turn
     * ESE-SPI.
     *
     */
    public boolean reset() {
        try {
            return eseSpiService.reset();
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            return false;
        }
    }

    /**
     * Reset ESE hardware.
     *
     * <p>This call is synchronous.
     *
     * <p>If this returns true, then ESE is reset .
     * If this returns false, then
     * there is some problem that prevents an attempt to reset ESE.
     *
     */
    public boolean eseChipReset() {
        try {
            return eseSpiService.eseChipReset();
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            return false;
        }
    }
    /**
     * Enable ESE-SPI hardware.
     *
     * <p>This call is asynchronous. Listen for
     * {@link #ACTION_ADAPTER_STATE_CHANGED} broadcasts to find out when the
     * operation is complete.
     *
     * <p>If this returns true, then either ESE-SPI is already on, or
     * a {@link #ACTION_ADAPTER_STATE_CHANGED} broadcast will be sent
     * to indicate a state transition. If this returns false, then
     * there is some problem that prevents an attempt to turn
     * ESE-SPI.
     *
     */
    public boolean enable(int timeout, IBinder b) {
        try {
            return eseSpiService.enable(timeout,b);
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            return false;
        }
    }

    /**
     * Disable ESE-SPI hardware.
     *
     * <p>No ESE-SPI features will work after this call, and the hardware
     * will not perform or respond to any ESE-SPI communication.
     *
     * <p>This call is asynchronous. Listen for
     * {@link #ACTION_ADAPTER_STATE_CHANGED} broadcasts to find out when the
     * operation is complete.
     *
     * <p>If this returns true, then either ESE-SPI is already off, or
     * a {@link #ACTION_ADAPTER_STATE_CHANGED} broadcast will be sent
     * to indicate a state transition. If this returns false, then
     * there is some problem that prevents an attempt to turn
     * ESE-SPI off.
     *
     */
    public boolean disable() {
        try {
            return eseSpiService.disable(true);
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            return false;
        }
    }

     /**
      * It is used to upgrade the jcop os only if the securement is present
      * <p>Requires {@link android.Manifest.permission#NFC} permission.
      *
      * @throws IOException If a failure occurred during Jcop Os update.

     public int doJcopOsUpdate_spi() throws IOException
     {
         try {
             return eseSpiService.jcopOsUpdate_spi();
         } catch (RemoteException e) {
             Log.e(TAG, "RemoteException in doJcopOsUpdate(): ", e);
             throw new IOException("RemoteException in doJcopOsUpdate_spi()");
         }
    }
    */

}

/** @} */
