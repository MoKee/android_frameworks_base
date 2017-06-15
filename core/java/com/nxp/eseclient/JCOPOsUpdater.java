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
import com.nxp.intf.IJcopService;
import java.io.IOException;
import com.nxp.eseclient.EseClientManager;
import com.nxp.eseclient.EseClientServicesAdapterBuilder;
import com.nxp.eseclient.EseClientServicesAdapter;
import java.lang.reflect.Method;

/**
 * This class provides API for secure Jcop download operation.
*/
/** {@hide} */
public final class JCOPOsUpdater{
    private static final String TAG = "EseSpiJCOPService";
    private IJcopService IJcpService;
    private static EseClientManager mEseManager;
    private static EseClientServicesAdapter mEseClientServicesAdapter;
    private static EseClientServicesAdapterBuilder mEseClientServicesAdapterBuilder;
    private String setMedium;
    private static JCOPOsUpdater mJcpService = null;


    public JCOPOsUpdater(IJcopService mJcopService) {
        IJcpService = mJcopService;
    }

    /**
     * Helper to fetch user preferred interface's JcopOsUpdater service object.
     * @return the JCOPOsUpdater Service object, or null if user preferred interface
     * is not availble
    */
    public static synchronized JCOPOsUpdater createJCOPInterface() throws IOException, RemoteException{

        IJcopService JcpServiceIntf = null;
        mEseManager = EseClientManager.getInstance();
        mEseManager.initialize();
        Integer seMedium = mEseManager.getSeInterface(EseClientManager.JCPSERVICE);
        Log.e("TAG", "Selected P61 interface ="+seMedium.intValue());
        mEseClientServicesAdapterBuilder = new EseClientServicesAdapterBuilder();
        mEseClientServicesAdapter = mEseClientServicesAdapterBuilder.getEseClientServicesAdapterInstance(seMedium.intValue());
        JcpServiceIntf = mEseClientServicesAdapter.getJcopService();
        if(JcpServiceIntf != null) {
            mJcpService = new JCOPOsUpdater(JcpServiceIntf);
            Log.e("TAG", "JcpServiceIntf is retrived");
        }
        if(mJcpService == null)
            new IOException("Interface not available");

        return mJcpService;
    }

    /**
     * This API performs secure JCOP OS update/upgrade operation over
     * user preferred interface through JCOPOsUpdater Service object.
     * Caller's application package needs to register with NFC Service
     * for its signature verification.Otherwise JCOPOsUpdate opertion
     * would not be allowed.
     * @param pkg Caller's package name
     * @return int :- SUCCESS returns 0 or Not Supported returns 0x0F otherwise non-zero.
     * <p>Requires {@link android.Manifest.permission#NFC} permission.
     * @throws IOException If a failure occurred during appletLoadApplet
    */

public synchronized int jcopOsDownload(String pkg) throws IOException{
    int status;
    try {
        status = IJcpService.jcopOsDownload(pkg);
        if(status == 0x00) {
            Log.e("TAG", "Jcop Download success" + status);
        } else if(status == 0x0F){
            Log.e("TAG", "Feature is Not Supported" + status);
        }
     } catch (RemoteException e) {
           Log.e(TAG, "RemoteException in jcopOsDownload(): " + e);
           throw new IOException("RemoteException in jcopOsDownload()");
     }
     return status;
}
}
