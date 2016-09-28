package kr.ac.kaist.resl.cmsp.iotapp.gui;

import android.app.Activity;

public class ViewBroker {
	private static Activity activity;
	

	public static void setActivity(Activity activity) {
		ViewBroker.activity = activity;
	}

	public static Activity getActivity() {		
		return activity;
	}
}
