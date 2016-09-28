package kr.ac.kaist.resl.cmsp.iotapp.platform;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PlatformMain extends ActionBarActivity {
    final Logger logger = LoggerFactory.getLogger(PlatformMain.class.getSimpleName());
    Button startBtn;
    Button stopBtn;
    Button testBtn;
    Button testBtn2;
    //SensorTagService sensorTag;
    //MioService mio;
    //HueService hue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_platform_main);
        startBtn = (Button) findViewById(R.id.startBtn);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logger.info("Starting IoT-App Platform service");
                PlatformBroadcastReceiver.startService(getApplicationContext());

            }
        });
        stopBtn = (Button) findViewById(R.id.stopBtn);
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlatformBroadcastReceiver.stopService(getApplicationContext());
            }
        });


        testBtn = (Button) findViewById(R.id.testBtn);
        testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Sensor Tag Test
                sensorTag = new SensorTagImpl(getApplicationContext());
                String[] services = {ThingService.class.getSimpleName(), AccelService.class.getSimpleName()};
                ThingServiceEndpoint endpoint = new ThingServiceEndpoint(ThingServiceInfo.DEVICEFRAMEWORK_BLE,
                        Build.MODEL, "34:B1:F7:D5:0C:01", "SensorTag", services);
                endpoint.setEndpoint("34:B1:F7:D5:0C:01");
                sensorTag.setEndpoint(endpoint);
                sensorTag.connect();
                */
                /* Mio Link Test
                mio = new MioImpl(getApplicationContext());
                // need delay to search
                String[] services = {ThingService.class.getSimpleName(), HeartRateService.class.getSimpleName()};
                ThingServiceEndpoint endpoint = new ThingServiceEndpoint(ThingServiceInfo.DEVICEFRAMEWORK_ANTPLUS,
                        Build.MODEL, "MIO56P0009M2IK", "MioLink", services);
                endpoint.setEndpoint("MIO56P0009M2IK");
                mio.setEndpoint(endpoint);
                mio.connect();
                */
                /* Hue Test
                hue = new HueImpl(getApplicationContext());
                String[] services = {ThingService.class.getSimpleName(), OnOffService.class.getSimpleName()};
                ThingServiceEndpoint endpoint = new ThingServiceEndpoint(ThingServiceInfo.DEVICEFRAMEWORK_UPNP,
                        Build.MODEL, "00:17:88:16:07:ED", "Hue", services);
                endpoint.setEndpoint("192.168.0.7");
                hue.setEndpoint(endpoint);
                hue.connect();
                */
            }
        });
        testBtn2 = (Button) findViewById(R.id.testBtn2);
        testBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Sensor Tag Test
                Toast.makeText(getApplicationContext(), sensorTag.getAccelRateMeasurement(), Toast.LENGTH_SHORT).show();
                */
                /* Mio Link Test
                Toast.makeText(getApplicationContext(), mio.getHeartRateMeasurement(), Toast.LENGTH_SHORT).show();
                */
                /* Hue Test
                try {
                    hue.toggle();
                } catch (IoTAppException e) {
                    e.printStackTrace();
                }
                */
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_platform_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
