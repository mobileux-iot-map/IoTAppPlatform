package kr.ac.kaist.resl.cmsp.iotapp.gui.devices;

import kr.ac.kaist.resl.cmsp.iotapp.library.ThingServiceEndpoint;

public interface DeviceListSelectCallback {
	public void onSelectedDevice(ThingServiceEndpoint device);
}
