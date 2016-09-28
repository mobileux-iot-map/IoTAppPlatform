package kr.ac.kaist.resl.cmsp.iotapp.gui;

import android.app.Activity;
import kr.ac.kaist.resl.cmsp.iotapp.engine.abstraction.scan.DeviceScanStrategy;
import kr.ac.kaist.resl.cmsp.iotapp.engine.abstraction.scan.ScanFinishedCallback;
import kr.ac.kaist.resl.cmsp.iotapp.gui.devices.DeviceListDialog;
import kr.ac.kaist.resl.cmsp.iotapp.gui.devices.DeviceListSelectCallback;
import kr.ac.kaist.resl.cmsp.iotapp.library.ThingServiceEndpoint;
import kr.ac.kaist.resl.cmsp.iotapp.library.service.general.ThingService;

import java.util.ArrayList;
import java.util.List;

public class DeviceListBroker implements ScanFinishedCallback, DeviceListSelectCallback {
	private DeviceListDialog deviceListDialog = null;
	private boolean scanFinished = false;
	private ThingServiceEndpoint selectedDevice = null;
	private static ArrayList<ThingServiceEndpoint> cachedScannedDevice = null;
	private ArrayList<ThingServiceEndpoint> scannedDevice = null;
	private Activity activity;
	private String service;

	public DeviceListBroker(Activity activity) {
		this.activity = activity;
	}

	public void createDialog() {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				deviceListDialog = new DeviceListDialog(activity, DeviceListBroker.this);				
			}	    		
		});
	}
	
	public List<ThingServiceEndpoint> getCachedScannedDevice() {
		return this.cachedScannedDevice;
	}
	
	public void startScan(boolean useCachedScannedDevice) {
		scanFinished = false;

	}

	public void showDialog() {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (deviceListDialog == null)
					return;
				selectedDevice = null;
				deviceListDialog.show();
			}
		});
	}
	
	public void setRequiredService(String service) {
		this.service = service;
	}

	public ThingServiceEndpoint getSelectedDevice() {
		while (selectedDevice == null) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
		}
		return selectedDevice;
	}

	@Override
	public void onSelectedDevice(ThingServiceEndpoint device) {
		selectedDevice = device;		
	}

    @Override
    public void scanFinished(DeviceScanStrategy finishedStrategy) {
        this.cachedScannedDevice = new ArrayList<ThingServiceEndpoint>(scannedDevice);
        this.scannedDevice = new ArrayList<ThingServiceEndpoint>();
        for (ThingServiceEndpoint device : cachedScannedDevice) {
            if (device.getServices().contains(this.service)) {
                this.scannedDevice.add(device);
            }
        }
        deviceListDialog.addDevicesToList(this.scannedDevice);
        scanFinished = true;
    }
}
