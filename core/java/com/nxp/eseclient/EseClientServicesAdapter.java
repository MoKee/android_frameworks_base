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
import com.nxp.intf.IeSEClientServicesAdapter;
import com.nxp.intf.ILoaderService;
import com.nxp.intf.IJcopService;
import com.nxp.intf.INxpExtrasService;
import com.nxp.eseclient.EseClientManager;
import android.os.RemoteException;
import java.io.IOException;
import android.util.Log;
/** {@hide} */
public final class EseClientServicesAdapter{
    private static final String TAG = "EseClientServicesAdapter";
    private EseClientManager mEseManager;
    private IeSEClientServicesAdapter mIEseClientServicesAdapter = null;
    private ILoaderService mILoaderService =null;
    private IJcopService mIJcopService = null;
    private INxpExtrasService mINxpExtrasService = null;
    public boolean initEseClientServicesAdapterInstance(int medium) throws IOException{
        boolean ret = false;
        mEseManager = EseClientManager.getInstance();
        switch(medium)
        {
            case EseClientManager.NFC:
                Log.e(TAG, "NFC interface selected");
                mIEseClientServicesAdapter = mEseManager.getNfcEseClientServicesAdapterInterface();
                ret = true;
                break;
            case EseClientManager.SPI:
                Log.e(TAG, "NFC interface selected");
                mIEseClientServicesAdapter = mEseManager.getSpiEseClientServicesAdapterInterface();
                ret = true;
                break;
            default:
                Log.e(TAG, "invalid interface selection");
                break;
        }
        if(mIEseClientServicesAdapter == null){
            ret  = false;
            new IOException("Interface not available");
        }
        return ret;
    }
    /**
    * @hide
    */
    public INxpExtrasService getNxpExtrasService() throws RemoteException{
        if(mIEseClientServicesAdapter != null){
            mINxpExtrasService = mIEseClientServicesAdapter.getNxpExtrasService();
        }
        if(mINxpExtrasService == null){
            throw new RemoteException("Interface not available");
        }
        return mINxpExtrasService;
    }
    /**
    * @hide
    */
    public ILoaderService getLoaderService() throws RemoteException{
        if(mIEseClientServicesAdapter != null){
            mILoaderService = mIEseClientServicesAdapter.getLoaderService();
        }
        if(mILoaderService == null){
            throw new RemoteException("Interface not available");
        }
        return mILoaderService;
    }
    /**
    * @hide
    */
    public IJcopService   getJcopService() throws RemoteException{
        if(mIEseClientServicesAdapter != null){
            mIJcopService = mIEseClientServicesAdapter.getJcopService();
        }
        if(mIJcopService == null){
            throw new RemoteException("Interface not available");
        }
        return mIJcopService;
    }
}
