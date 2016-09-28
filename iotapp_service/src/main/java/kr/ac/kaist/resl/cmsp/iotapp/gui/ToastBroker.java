package kr.ac.kaist.resl.cmsp.iotapp.gui;

import android.app.Activity;
import android.widget.Toast;

public class ToastBroker {
	public static Activity getActivity() {
		return ViewBroker.getActivity();
	}
	
	public static void makeToast(String msg) {
		Toast.makeText(ViewBroker.getActivity(), msg, Toast.LENGTH_SHORT).show();
	}
}
