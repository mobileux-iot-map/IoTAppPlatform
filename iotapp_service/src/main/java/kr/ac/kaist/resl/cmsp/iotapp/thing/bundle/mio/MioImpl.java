package kr.ac.kaist.resl.cmsp.iotapp.thing.bundle.mio;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.dsi.ant.plugins.antplus.pccbase.AsyncScanController;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;
import kr.ac.kaist.resl.cmsp.iotapp.library.ThingServiceEndpoint;
import kr.ac.kaist.resl.cmsp.iotapp.thing.MioService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Created by shheo on 15. 7. 20.
 */
public class MioImpl implements MioService {
    private static final String TAG = MioImpl.class.getSimpleName();
    ThingServiceEndpoint endpoint = null;
    ArrayList<AsyncScanController.AsyncScanResultDeviceInfo> mAlreadyConnectedDeviceInfos;
    ArrayList<AsyncScanController.AsyncScanResultDeviceInfo> mScannedDeviceInfos;
    protected PccReleaseHandle<AntPlusHeartRatePcc> releaseHandle = null;
    AntPlusHeartRatePcc hrPcc = null;

    AsyncScanController<AntPlusHeartRatePcc> hrScanCtrl;
    int computedHeartRate = 0;
    boolean isConnected = false;
    Context context;

    public MioImpl(Context context) {
        this.context = context;
        mAlreadyConnectedDeviceInfos = new ArrayList<>();
        mScannedDeviceInfos = new ArrayList<>();
        requestAccessToPcc();
    }

    @Override
    public String getHeartRateMeasurement() {
        return this.computedHeartRate + "";
    }

    @Override
    public String getEnergyExpended() {
        return null;
    }

    @Override
    public String getRrInterval() {
        return null;
    }

    @Override
    public String getBodySensorLocation() {
        return null;
    }

    @Override
    public void setEnergyExpendedResetted() {

    }

    @Override
    public String getThingId() {
        return endpoint.getThingId();
    }

    @Override
    public String getThingName() {
        return endpoint.getThingName();
    }

    @Override
    public void setEndpoint(ThingServiceEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public List<String> getScannedAntDevices() {
        List<String> toReturn = new ArrayList<>();
        for (AsyncScanController.AsyncScanResultDeviceInfo mScannedDeviceInfo : mScannedDeviceInfos) {
            toReturn.add(mScannedDeviceInfo.getDeviceDisplayName());
        }
        return toReturn;
    }

    @Override
    public void connect() {
        Log.d(TAG, "connect()");
        for (AsyncScanController.AsyncScanResultDeviceInfo mScannedDeviceInfo : mScannedDeviceInfos) {
            Log.d(TAG, "mScannedDeviceInfo.getDeviceDisplayName(): " + mScannedDeviceInfo.getDeviceDisplayName());
            if (mScannedDeviceInfo.getDeviceDisplayName().equalsIgnoreCase(endpoint.getEndpoint())) {
                requestConnectToResult(mScannedDeviceInfo);
            }

        }
    }

    @Override
    public void disconnect() {
        isConnected = false;
    }

    @Override
    public Boolean isConnected() {
        return isConnected;
    }


    public void Set_heartrate(int rate) {

        this.computedHeartRate = rate;
        //return this.computedHeartRate;
    }

    protected AntPluginPcc.IPluginAccessResultReceiver<AntPlusHeartRatePcc> base_IPluginAccessResultReceiver =
            new AntPluginPcc.IPluginAccessResultReceiver<AntPlusHeartRatePcc>() {
                //Handle the result, connecting to events on success or reporting failure to user.
                @Override
                public void onResultReceived(AntPlusHeartRatePcc result, RequestAccessResult resultCode,
                                             DeviceState initialDeviceState) {
                    Log.d(TAG, "ShowData " + " Connecting...");
                    //Log.d("ShowData", "Connecting...");
                    switch (resultCode) {
                        case SUCCESS:
                            isConnected = true;
                            hrPcc = result;
                            Log.d(TAG, "tv_status : " + result.getDeviceName());
                            //Log.d("tv_status : " + result.getDeviceName(), initialDeviceState.toString());
                            subscribeToHrEvents();
                            break;
                        case CHANNEL_NOT_AVAILABLE:
                            Log.d(TAG, "channel_not_available : " + "Channel Not Available");
                            //Log.d("channel_not_available : ", "Channel Not Available");
                            break;
                        case ADAPTER_NOT_DETECTED:
                            Log.d(TAG, "ADAPTER_NOT_DETECTED : " + " ADAPTER_NOT_DETECTED");
                            //Log.d("ADAPTER_NOT_DETECTED : ", "ADAPTER_NOT_DETECTED");
                            break;
                        case BAD_PARAMS:
                            Log.d(TAG, "Bad : " + "Bad");
                            //Log.d("Bad : ", "Bad");
                            break;
                        case OTHER_FAILURE:
                            Log.d(TAG, "OTHER_FAILURE : ");
                            //Log.d("OTHER_FAILURE : ", "OTHER_FAILURE");
                            break;
                        case USER_CANCELLED:
                            Log.d(TAG, "USER_CANCELLED : ");
                            //Log.d("USER_CANCELLED : ", "USER_CANCELLED");
                            break;
                        case UNRECOGNIZED:
                            Log.d(TAG, "UNRECOGNIZED : ");
                            //Log.d("UNRECOGNIZED : ", "UNRECOGNIZED");
                            break;
                        default:
                            Log.d(TAG, "default : ");
                            //Log.d("default : ", "default");
                            break;
                    }
                }
            };

    public void subscribeToHrEvents() {
        hrPcc.subscribeHeartRateDataEvent(new AntPlusHeartRatePcc.IHeartRateDataReceiver() {
            @Override
            public void onNewHeartRateData(final long estTimestamp, EnumSet<EventFlag> eventFlags,
                                           final int computedHeartRate, final long heartBeatCount,
                                           final BigDecimal heartBeatEventTime, final AntPlusHeartRatePcc.DataState dataState) {
                // Mark heart rate with asterisk if zero detected
                final String textHeartRate = String.valueOf(computedHeartRate)
                        + ((AntPlusHeartRatePcc.DataState.ZERO_DETECTED.equals(dataState)) ? "*" : "");

                Set_heartrate(Integer.parseInt(textHeartRate));
                //Log.d("textHeartRate : ", textHeartRate);


            }
        });
    }

    protected void requestAccessToPcc() {
        //AntPlusHeartRatePcc.requestAsyncScanController()
        hrScanCtrl = AntPlusHeartRatePcc.requestAsyncScanController(context, 0,
                new AsyncScanController.IAsyncScanResultReceiver() {
                    @Override
                    public void onSearchStopped(RequestAccessResult reasonStopped) {
                        //The triggers calling this function use the same codes and require the same actions as those received by the standard access result receiver
                        base_IPluginAccessResultReceiver.onResultReceived(null, reasonStopped, DeviceState.DEAD);
                    }

                    @Override
                    public void onSearchResult(final AsyncScanController.AsyncScanResultDeviceInfo deviceFound) {
                        Log.d(TAG, "onSearchResult: " + deviceFound.getDeviceDisplayName());
                        for (AsyncScanController.AsyncScanResultDeviceInfo i : mScannedDeviceInfos) {
                            //The current implementation of the async scan will reset it's ignore list every 30s,
                            //So we have to handle checking for duplicates in our list if we run longer than that
                            if (i.getAntDeviceNumber() == deviceFound.getAntDeviceNumber()) {
                                //Found already connected device, ignore
                                return;
                            }
                        }

                        //We split up devices already connected to the plugin from un-connected devices to make this information more visible to the user,
                        //since the user most likely wants to be aware of which device they are already using in another app
                        if (deviceFound.isAlreadyConnected()) {
                            mAlreadyConnectedDeviceInfos.add(deviceFound);
                            //Log.d("mAlreadyConnectedDeviceInfos if", deviceFound.toString());
                            Log.d(TAG, "mAlreadyConnectedDeviceInfos if");
                        } else {
                            mScannedDeviceInfos.add(deviceFound);
                            Log.d(TAG, "mScannedDeviceInfos else");
                            //Log.d("mScannedDeviceInfos else", deviceFound.toString());
                        }
                    }
                });
    }


    protected AntPluginPcc.IDeviceStateChangeReceiver base_IDeviceStateChangeReceiver =
            new AntPluginPcc.IDeviceStateChangeReceiver() {
                @Override
                public void onDeviceStateChange(final DeviceState newDeviceState) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "IDeviceStateChangeReceiver");
                            //Log.d("IDeviceStateChangeReceiver", "IDeviceStateChangeReceiver");
                        }
                    });


                }
            };


    protected void requestConnectToResult(final AsyncScanController.AsyncScanResultDeviceInfo asyncScanResultDeviceInfo) {
        //Inform the user we are connecting
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                //Log.d("Connecting to " , asyncScanResultDeviceInfo.getDeviceDisplayName());
                releaseHandle = hrScanCtrl.requestDeviceAccess(asyncScanResultDeviceInfo,
                        new AntPluginPcc.IPluginAccessResultReceiver<AntPlusHeartRatePcc>() {
                            @Override
                            public void onResultReceived(AntPlusHeartRatePcc result,
                                                         RequestAccessResult resultCode, DeviceState initialDeviceState) {
                                if (resultCode == RequestAccessResult.SEARCH_TIMEOUT) {

                                    Log.d(TAG, "Timeout");
                                    //Log.d("Timeout", "Timeout");

                                } else {
                                    //Otherwise the results, including SUCCESS, behave the same as
                                    base_IPluginAccessResultReceiver.onResultReceived(result, resultCode, initialDeviceState);
                                    hrScanCtrl = null;
                                }
                            }
                        }, base_IDeviceStateChangeReceiver);
            }
        });
    }
}
