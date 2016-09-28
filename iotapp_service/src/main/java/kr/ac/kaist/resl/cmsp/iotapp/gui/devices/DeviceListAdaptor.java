package kr.ac.kaist.resl.cmsp.iotapp.gui.devices;

import java.util.ArrayList;
import java.util.List;

import kr.ac.kaist.resl.cmsp.iotapp.library.ThingServiceEndpoint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import kr.ac.kaist.resl.cmsp.iotapp.platform.R;

// referred http://sunil-android.blogspot.kr/2013/09/custom-listview-alertdialog-with-filter.html

public class DeviceListAdaptor extends BaseAdapter{

	Context ctx=null;
	ArrayList<String> deviceNameList=null;
	ArrayList<String> deviceUuidList = null;
	private LayoutInflater mInflater=null;
	public DeviceListAdaptor(Activity activty)
	{
		this.ctx=activty;
		mInflater = activty.getLayoutInflater();
		this.deviceNameList = new ArrayList<String>();
		this.deviceUuidList = new ArrayList<String>();
	}
	@Override
	public int getCount() {

		return deviceNameList.size();
	}

	@Override
	public Object getItem(int arg0) {
		return null;
	}

	@Override
	public long getItemId(int arg0) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup arg2) {       
		final ViewHolder holder;
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = mInflater.inflate(R.layout.device_dialog_listrow, null);

			holder.deviceName = (TextView) convertView.findViewById(R.id.deviceNameTextView);
			holder.deviceUuid = (TextView) convertView.findViewById(R.id.deviceUuidTextView);
			convertView.setTag(holder);
		}
		else {
			holder = (ViewHolder) convertView.getTag();
		}
		String deviceName=deviceNameList.get(position);
		holder.deviceName.setText(deviceName);
		String deviceUuid=deviceUuidList.get(position);
		holder.deviceUuid.setText(deviceUuid);

		return convertView;
	}

	private static class ViewHolder {
		TextView deviceName;
		TextView deviceUuid;
	}

	public void addDevicesToList(List<ThingServiceEndpoint> scannedDevice) {
		for (ThingServiceEndpoint device : scannedDevice) {
			deviceNameList.add(device.getThingName());
			deviceUuidList.add(device.getThingId());
		}
	}
}
