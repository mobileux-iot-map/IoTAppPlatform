package kr.ac.kaist.resl.cmsp.iotapp.gui.devices;

import java.util.List;

import kr.ac.kaist.resl.cmsp.iotapp.gui.DeviceListBroker;
import kr.ac.kaist.resl.cmsp.iotapp.library.ThingServiceEndpoint;
import android.R.attr;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class DeviceListDialog implements OnClickListener, OnItemClickListener {
	private Activity storedActivity = null;
	private AlertDialog.Builder storedDialogBuilder = null;
	private AlertDialog storedDialog = null;
	private DeviceListAdaptor arrayAdapter;
	LinearLayout deviceListLayout;
	private TextView titleTextView;
	private ProgressBar progressBar;
	private ListView deviceListView;
	private DeviceListSelectCallback selectCallback;
	private List<ThingServiceEndpoint> scannedDevice;
	
	public DeviceListDialog(Activity activity, DeviceListBroker broker) {
		storedActivity = activity;
		selectCallback = broker;
		storedDialogBuilder = new AlertDialog.Builder(storedActivity);
		titleTextView = new TextView(storedActivity);
		titleTextView.setText("연결하실 사물을 선택해주세요");
		titleTextView.setGravity(Gravity.CENTER_HORIZONTAL);
		//titleTextView.setTextAppearance(storedActivity, attr.textAppearanceLarge);
		progressBar = new ProgressBar(storedActivity, null, attr.progressBarStyleLarge);
		deviceListView = new ListView(storedActivity);
		deviceListLayout = new LinearLayout(storedActivity);
		deviceListLayout.setOrientation(LinearLayout.VERTICAL);
		deviceListLayout.addView(titleTextView);
		deviceListLayout.addView(progressBar);
		deviceListLayout.addView(deviceListView);
		storedDialogBuilder.setView(deviceListLayout);
		arrayAdapter=new DeviceListAdaptor(storedActivity);
		deviceListView.setAdapter(arrayAdapter);
		deviceListView.setOnItemClickListener(this);
		
		storedDialogBuilder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				dialog.dismiss();
			}
		});
		
		storedDialog = storedDialogBuilder.create();
		storedDialog.setTitle("사물 검색");
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		selectCallback.onSelectedDevice(scannedDevice.get(position));
		storedDialog.dismiss();
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}
	
	public void addDevicesToList(List<ThingServiceEndpoint> scannedDevice) {
		this.scannedDevice = scannedDevice;
		arrayAdapter.addDevicesToList(scannedDevice);
		progressBar.setVisibility(View.GONE);
		arrayAdapter.notifyDataSetChanged();
	}
	
	public void show() {
		//storedDialogBuilder.show();
		storedDialog.show();
	}
}
