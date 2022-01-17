package org.ludvin.cartographe;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

public class WiFiScanReceiver extends BroadcastReceiver {

  UIRepere wifiDemo;

  public WiFiScanReceiver(UIRepere uiRepere) {
    super();
    this.wifiDemo = uiRepere;
  }

  @Override
  public void onReceive(Context c, Intent intent) {
    List<ScanResult> results = wifiDemo.wifi.getScanResults();
    ScanResult bestSignal = null;
    for (ScanResult result : results) {
      if (bestSignal == null
          || WifiManager.compareSignalLevel(bestSignal.level, result.level) < 0)
        bestSignal = result;
    }
    //String message = String.format("%s networks found. %s is the strongest.", results.size(), bestSignal.SSID+" (level:"+bestSignal.level+")");	//debug
  }

}
