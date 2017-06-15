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
import com.nxp.eseclient.EseClientServicesAdapter;
import android.os.RemoteException;
import java.io.IOException;
import android.util.Log;
/** {@hide} */
public final class EseClientServicesAdapterBuilder{

    private static final String TAG = "EseClientServicesAdapterBuilder";
    private static EseClientServicesAdapter mNfcEseClientServicesAdapter;
    private static EseClientServicesAdapter mSpiEseClientServicesAdapter;
    static{
        mNfcEseClientServicesAdapter = new EseClientServicesAdapter();
        mSpiEseClientServicesAdapter = new EseClientServicesAdapter();
    }
    public static EseClientServicesAdapter getEseClientServicesAdapterInstance(int seMedium) throws IOException{
        EseClientServicesAdapter mEseClientServicesAdapterNullObject = null;
        boolean retVal = false;
        switch(seMedium)
        {
            case EseClientManager.NFC:
                retVal = mNfcEseClientServicesAdapter.initEseClientServicesAdapterInstance(seMedium);
                if(retVal){
                    return mNfcEseClientServicesAdapter;
                }
                break;
            case EseClientManager.SPI:
                retVal = mSpiEseClientServicesAdapter.initEseClientServicesAdapterInstance(seMedium);
                if(retVal){
                    return mSpiEseClientServicesAdapter;
                }
                break;
            default:
                Log.e(TAG, "invalid interface selection");
                break;
        }
        if(retVal == false)
            new IOException("Interface not available");
        return mEseClientServicesAdapterNullObject;
    }
}
