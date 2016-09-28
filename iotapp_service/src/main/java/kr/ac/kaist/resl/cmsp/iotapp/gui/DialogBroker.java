package kr.ac.kaist.resl.cmsp.iotapp.gui;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Handler;

public class DialogBroker{
	private static final Map<String, Dialog> dialogMap = new HashMap<String, Dialog>();
	
	public static Activity getActivity() {
		return ViewBroker.getActivity();
	}
	
	// FIXME: dialog should be instantiated in the main ui thread!!!
	public static void putDialog (String key, Dialog dialog) {
		dialogMap.put(key, dialog);
	}
	
	public static void putNewProgressDialog (final String key) {
		ViewBroker.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				dialogMap.put(key, new ProgressDialog(ViewBroker.getActivity()));
			}	    		
		});
	}
	
	
	public static Dialog getDialog(String key) {
		return dialogMap.get(key);
	}
	
	public static void showDialog(final String key) {
		ViewBroker.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Dialog dialog = dialogMap.get(key);
				if (dialog != null)					
					dialog.show();
			}	    		
		});
	}
	
	public static void dismissDialog(final String key) {
		ViewBroker.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Dialog dialog = dialogMap.get(key);
				if (dialog != null)					
					dialog.dismiss();
				dialogMap.remove(key);
			}	    		
		});
	}
	
	// Setting text is only available for AlertDialog
	public static void setDialogText(final String key, final String msg) {
		ViewBroker.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Dialog dialog = dialogMap.get(key);
				if (dialog != null && dialog instanceof AlertDialog)
					((AlertDialog)dialog).setMessage(msg);
			}			
		});
	}

	public static void setCancelable(final String key, final boolean isCancelable) {
		ViewBroker.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Dialog dialog = dialogMap.get(key);
				if (dialog != null)					
					dialog.setCancelable(isCancelable);
			}			
		});
	}
}
