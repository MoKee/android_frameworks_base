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
import com.nxp.eseclient.EseClientManager;
import com.nxp.eseclient.EseClientServicesAdapterBuilder;
import com.nxp.eseclient.EseClientServicesAdapter;
import com.nxp.intf.INxpExtrasService;
import android.os.RemoteException;
import android.util.Log;
import android.os.IBinder;
import android.os.Bundle;
import java.io.IOException;

/**
*This class provide APIs for M4M LTSM client operations
*/
/** {@hide} */
public final class LtsmService{
    private static final String TAG ="LtsmService";
    private static EseClientManager mEseManager;
    private static EseClientServicesAdapter mEseClientServicesAdapter;
    private static EseClientServicesAdapterBuilder mEseClientServicesAdapterBuilder;
    private static INxpExtrasService mINxpExtrasService;
    private static LtsmService mLtsmService = null;
    public LtsmService(INxpExtrasService NxpExtrasServiceIntf) {
        mINxpExtrasService = NxpExtrasServiceIntf;
    }
    /**
     * Helper to create an LtsmService object.
     * @return the LtsmService, or null if no LtsmService exists
    */
    public static synchronized LtsmService createLtsmServiceInterface() throws IOException,RemoteException{
        INxpExtrasService NxpExtrasServiceIntf = null;
        mEseManager = EseClientManager.getInstance();
        mEseManager.initialize();
        Integer seMedium = mEseManager.getSeInterface(EseClientManager.LTSMSERVICE);
        Log.e(TAG, "Selected P61 interface ="+seMedium.intValue());
        mEseClientServicesAdapterBuilder = new EseClientServicesAdapterBuilder();
        mEseClientServicesAdapter = mEseClientServicesAdapterBuilder.getEseClientServicesAdapterInstance(seMedium.intValue());
        NxpExtrasServiceIntf = mEseClientServicesAdapter.getNxpExtrasService();
        if(NxpExtrasServiceIntf != null) {
            mLtsmService = new LtsmService(NxpExtrasServiceIntf);
            Log.e(TAG, "LtsmService is retrived");
        }
        if(mLtsmService == null){
            new IOException("Interface not available");
        }
        return mLtsmService;
    }
    /**
     * This API calls the secure element open connection
     * @return Bundle :- Exception type and message if there is exception else returns bundle with value 0
     * @throws Exception If a failure occurred during secure element open connection
     */
    public static synchronized Bundle open(String pkg, IBinder b) throws IOException{

        try{
            Bundle result = mINxpExtrasService.open(pkg, b);
            if(result.getInt("e") == 0x00){
                Log.e(TAG, "LTSM Open secure element successful");
                return result;
            }else {
                throw new IOException("LTSM open secure elemnt failed");
            }
        }catch(Exception e){
            Log.e(TAG, "Exception in LTSM secure element open connection", e);
            throw new IOException("Exception in LTSM  open connection()");
        }
    }
    /**
     * This API calls the secure element close  connection
     * @return Bundle :- Exception type and message if there is exception else returns bundle with value 0
     * @throws Exception If a failure occurred during  secure element close connection
     */
    public static synchronized Bundle close(String pkg, IBinder binder) throws IOException{
        try{
            Bundle result = mINxpExtrasService.close(pkg, binder);
            if(result.getInt("e") == 0x00){
                Log.e(TAG, "LTSM close secure element successful");
                return result;
            }else {
                throw new IOException("LTSM close secure elemnt failed");
            }
        }catch(Exception e){
            Log.e(TAG, "Exception in LTSM secure element close connection", e);
            throw new IOException("Exception in LTSM  close connection()");
        }
    }
    /**
     * This API calls the secure element transceive api
     * @return Bundle :- Exception type and message if there is exception else returns bundle with value 0
     * @throws Exception If a failure occurred during secure element transceive operation
     */
    public static synchronized Bundle transceive(String pkg, byte[] in) throws IOException{
        try{
            Bundle result = mINxpExtrasService.transceive(pkg, in);
            if(result.getInt("e") == 0x00){
                Log.e(TAG, "LTSM transceive secure element successful");
                return result;
            }else {
                throw new IOException("LTSM transceive secure elemnt failed");
            }
        }catch(Exception e){
            Log.e(TAG, "Exception in LTSM secure element transceive connection", e);
            throw new IOException("RemoteException in LTSM  transceive connection()");
        }
    }
}
