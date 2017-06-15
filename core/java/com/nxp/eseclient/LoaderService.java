 /*
  * Copyright (C) 2015 NXP Semiconductors
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

package com.nxp.eseclient;

import android.os.RemoteException;
import android.util.Log;
import android.os.IBinder;
import com.nxp.intf.ILoaderService;
import java.io.IOException;
import com.nxp.eseclient.EseClientManager;
import com.nxp.eseclient.EseClientServicesAdapterBuilder;
import com.nxp.eseclient.EseClientServicesAdapter;

/**
 * This class provides the primary API for managing Applet load applet.
*/
/** {@hide} */
public final class LoaderService{
    private static final String TAG = "EseSpiLoaderService";
    private ILoaderService ILdrService;
    private static EseClientManager mEseManager;
    private static EseClientServicesAdapter mEseClientServicesAdapter;
    private static EseClientServicesAdapterBuilder mEseClientServicesAdapterBuilder;
    private String setMedium;
    private static LoaderService mLdrService = null;

    public LoaderService(ILoaderService mAlaService) {
        ILdrService = mAlaService;
    }

    /**
     * Helper to create an Loader Service object.
     * @return the LoaderService, or null if no LoaderService exists
    */
    public static synchronized LoaderService createLSInterface() throws IOException, RemoteException{
        ILoaderService LdrServiceIntf = null;
        mEseManager = EseClientManager.getInstance();
        mEseManager.initialize();
        Integer seMedium = mEseManager.getSeInterface(EseClientManager.LDRSERVICE);
        Log.e("TAG", "Selected P61 interface ="+seMedium.intValue());
        mEseClientServicesAdapterBuilder = new EseClientServicesAdapterBuilder();
        mEseClientServicesAdapter = mEseClientServicesAdapterBuilder.getEseClientServicesAdapterInstance(seMedium.intValue());
        LdrServiceIntf = mEseClientServicesAdapter.getLoaderService();
        if(LdrServiceIntf != null) {
            mLdrService = new LoaderService(LdrServiceIntf);
            Log.e("TAG", "LdrServiceIntf is retrived");
        }
        if(mLdrService == null)
            new IOException("Interface not available");

        return mLdrService;
    }
    /**
     * This API performs Applet load Applet operation it fetches the
     * secure script from the path choice and string pkg is used for SHA verification
     * of callers context's package name by ALA applet.
     * @param pkg Callers package name
     * @param choice Secure script path
     * @return int :- SUCCESS returns 0 or Not Supported returns 0x0F otherwise non-zero.
     * <p>Requires {@link android.Manifest.permission#NFC} permission.
     * @throws IOException If a failure occurred during appletLoadApplet
    */
    public synchronized int appletLoadApplet(String pkg, String choice) throws IOException {
        try {
            int status = ILdrService.appletLoadApplet(pkg, choice);
            // Handle potential errors
            if (status == 0x00) {
                return status;
            } else if(status == 0x0F) {
                throw new UnsupportedOperationException("Api not supported");
            } else {
                throw new IOException("Unable to Load applet");
            }
         } catch (RemoteException e) {
               Log.e(TAG, "RemoteException in AppletLoadApplet(): ", e);
               throw new IOException("RemoteException in AppletLoadApplet()");
         }
    }
    /**
     * This API lists all the applets loaded through ALA module.
     * @param pkg Callers package name
     * @param name List of all applet
     * @return int :- SUCCESS returns 0 or Not Supported returns FF otherwise non-zero.
     * @throws IOException If a failure occurred during getListofApplets
     */
    public int getListofApplets(String pkg, String[] name) throws IOException {
        try {
            int num = ILdrService.getListofApplets(pkg, name);
            if(num != -1) {
                return num;
            } else {
                throw new UnsupportedOperationException("Api not supported");
            }
         } catch (RemoteException e) {
               Log.e(TAG, "RemoteException in GetListofApplets(): ", e);
               throw new IOException("RemoteException in GetListofApplets()");
         }
    }
    /**
     * This API returns the certificate key of the ALA applet present
     * @return byte[] :- Returns certificate key byte array.
     * @throws IOException If a failure occurred during getKeyCertificate
     */
    public byte[] getKeyCertificate() throws IOException {
        try{
            byte[] data = ILdrService.getKeyCertificate();
            if((data != null) && (data.length != 0x00)) {
                return data;
            } else if((data != null) && (data.length == 0x00)) {
                throw new UnsupportedOperationException("Api not supported");
            } else {
                throw new IOException("invalid data received");
            }
        } catch (RemoteException e) {
              Log.e(TAG, "RemoteException in getKeyCertificate(): ", e);
              throw new IOException("RemoteException in getKeyCertificate()");
        }
    }
    /**
     * This API triggers the execution of Loader service secure script
     * and store the responses in the given rspout file path.
     * @param srcIn Loader service secure input script filepath
     * @param rspOut Response ouput filepath where executed commands response are stored
     * @return byte[] :- Contains the last executed LS commands response status word
     * @throws RemoteException If a failure occurred during Loader service operation
     * @throws IOException If unable to perform Loader service operation
     */
    public synchronized byte[] lsExecuteScript(String srcIn, String rspOut) throws IOException {
        try {
            byte[] status = ILdrService.lsExecuteScript(srcIn, rspOut);
            // Handle potential errors
            if (status != null) {
                return status;
            } else if((status != null) && (status.length == 0x00)) {
                throw new UnsupportedOperationException("Api not supported");
            } else {
                throw new IOException("Unable to perfrom lsExecuteScript");
            }
         } catch (RemoteException e) {
               Log.e(TAG, "RemoteException in lsExecuteScript(): ", e);
               throw new IOException("RemoteException in lsExecuteScript()");
         }
    }
    /**
     * This API returns the Loader service client and applet version
     * @return byte[] :- Contains the LS client's and applet's major,minor version respectively
     * 0,1 index contain client's major,minor version and 2,3 index contain applet's major,minor version
     * @throws RemoteException If a failure occurred during get version operation
     * @throws IOException If unable to perform get version operation
     */
    public byte[] lsGetVersion() throws IOException {
        try{
            byte[] data = ILdrService.lsGetVersion();
            if((data != null) && (data.length != 0x00)) {
                return data;
            } else if((data != null) && (data.length == 0x00)) {
                throw new UnsupportedOperationException("Api not supported");
            } else {
                throw new IOException("invalid data received");
            }
        } catch (RemoteException e) {
              Log.e(TAG, "RemoteException in getKeyCertificate(): ", e);
              throw new IOException("RemoteException in getKeyCertificate()");
        }
    }
}
