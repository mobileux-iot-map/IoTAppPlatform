package kr.ac.kaist.resl.cmsp.iotapp.thing.bundle.sensortag;

import android.bluetooth.*;
import android.content.Context;
import android.util.Log;
import kr.ac.kaist.resl.cmsp.iotapp.library.ThingServiceEndpoint;
import kr.ac.kaist.resl.cmsp.iotapp.thing.SensorTagService;

import java.text.DecimalFormat;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by shheo on 15. 7. 20.
 */
public class SensorTagImpl implements SensorTagService {
    private final static String TAG = SensorTagImpl.class.getSimpleName();
    private ThingServiceEndpoint endpoint;
    private Context context;

    private String AccelRateMeasurement = "0,0,0";
    private byte keyEvent = (byte) 0;

    private static final byte[] ENABLE_SENSOR = {0x01};
    private static final byte[] ACCEL_PERI_500MS = {50};
    private static final Queue<Object> sWriteQueue = new ConcurrentLinkedQueue<Object>();
    private static boolean sIsWriting = false;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    // This device goes available when gatt discovery finishes. mConnectionState is not.
    private boolean isConnected = false;
    private int mConnectionState = STATE_DISCONNECTED;
    private String mConnectedBluetoothDeviceAddress;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;

    private static final UUID UUID_KEY_SERV = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_KEY_DATA = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_ACC_SERV = UUID.fromString("f000aa10-0451-4000-b000-000000000000");
    private static final UUID UUID_ACC_DATA = UUID.fromString("f000aa11-0451-4000-b000-000000000000");
    private static final UUID UUID_ACC_CONF = UUID.fromString("f000aa12-0451-4000-b000-000000000000");
    private static final UUID UUID_ACC_PERI = UUID.fromString("f000aa13-0451-4000-b000-000000000000");
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private DecimalFormat decimal = new DecimalFormat("+0.00;-0.00");

    public SensorTagImpl(Context context) {
        this.context = context;
    }

    @Override
    public String getAccelRateMeasurement() {
        return AccelRateMeasurement;
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

    @Override
    public void connect() {
        initBleAdapter();

        String addrToConnect = endpoint.getEndpoint();

        if (mBluetoothAdapter == null || addrToConnect == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return;
        }

        // Previously connected device.  Try to reconnect.
        if (mConnectedBluetoothDeviceAddress != null && addrToConnect.equals(mConnectedBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return;
            } else {
                return;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(addrToConnect);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(context, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mConnectedBluetoothDeviceAddress = addrToConnect;
        mConnectionState = STATE_CONNECTING;
        return;
    }

    @Override
    public void disconnect() {
        mBluetoothGatt.close();
    }

    @Override
    public Boolean isConnected() {
        return isConnected;
    }


    private void initBleAdapter() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
        }
    }
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            startService(gatt);
        }

        private void startService(BluetoothGatt gatt) {
            BluetoothGattService AccelService = gatt.getService(UUID_ACC_SERV);
            if(AccelService != null) {
                BluetoothGattCharacteristic AccelCharacteristic = AccelService.getCharacteristic(UUID_ACC_DATA);
                BluetoothGattCharacteristic AccelConf = AccelService.getCharacteristic(UUID_ACC_CONF);
                BluetoothGattCharacteristic AccelPeri = AccelService.getCharacteristic(UUID_ACC_PERI);
                if(AccelCharacteristic != null && AccelConf != null && AccelPeri != null) {
                    BluetoothGattDescriptor config_accel = AccelCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                    if(config_accel != null) {
                        try {
                            gatt.setCharacteristicNotification(AccelCharacteristic, true);
                            AccelConf.setValue(ENABLE_SENSOR);
                            write(AccelConf);
                            Thread.sleep(10);
                            config_accel.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            write(config_accel);
                            Thread.sleep(10);
                            AccelPeri.setValue(ACCEL_PERI_500MS);
                            write(AccelPeri);
                            Thread.sleep(10);

                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
            BluetoothGattService KeyService = gatt.getService(UUID_KEY_SERV);
            if(KeyService != null) {
                BluetoothGattCharacteristic KeyCharacteristic = KeyService.getCharacteristic(UUID_KEY_DATA);
                if(KeyCharacteristic != null) {
                    BluetoothGattDescriptor config_key = KeyCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);

                    if(config_key != null) {
                        try {
                            Thread.sleep(10);
                            gatt.setCharacteristicNotification(KeyCharacteristic, true);
                            config_key.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            write(config_key);
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }

            Log.d(TAG, "AccelMeasurement descriptor notification written");
            Log.d(TAG, "AccelMeasurement notification enabled ");
            isConnected = true;
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            super.onCharacteristicChanged(gatt, characteristic);
            updateFields(characteristic);

        }
        //

        private void updateFields(BluetoothGattCharacteristic characteristic) {
            byte  [] value = characteristic.getValue();
            String uuidStr = characteristic.getUuid().toString();

            Point3D v;
            String msg;

            if (uuidStr.equals(UUID_ACC_DATA.toString())) {
                v = convert_point3d(value);
                msg = decimal.format(v.x) + "," + decimal.format(v.y) + "," + decimal.format(v.z);
                AccelRateMeasurement = msg;
            } else if (uuidStr.equals(UUID_KEY_DATA.toString())) {
                Log.d(TAG, "====updateFields: keyFobDemo");
                for (byte b : value) {
                    Log.d(TAG, "==========data : " + (int)b);
                }
                for (int i = 0;i < 8;i++) { // length of byte

                }
                keyEvent |= value[0];
            }
        }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            updateFields(characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.v(TAG, "onCharacteristicWrite: " + status);
            sIsWriting = false;
            nextWrite();
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.v(TAG, "onDescriptorWrite: " + status);
            sIsWriting = false;
            nextWrite();
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState = STATE_CONNECTED;
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                isConnected = false;
            }
        }

    };

    private synchronized void write(Object o) {
        if(sWriteQueue.isEmpty() && !sIsWriting) {
            doWrite(o);
        } else {
            sWriteQueue.add(o);
        }
    }

    private synchronized void nextWrite() {
        if(!sWriteQueue.isEmpty() && !sIsWriting) {
            doWrite(sWriteQueue.poll());
        }
    }

    private synchronized void doWrite(Object o) {
        if(o instanceof BluetoothGattCharacteristic) {
            sIsWriting = true;
            mBluetoothGatt.writeCharacteristic(
                    (BluetoothGattCharacteristic)o);
        } else if(o instanceof BluetoothGattDescriptor) {
            sIsWriting = true;
            mBluetoothGatt.writeDescriptor((BluetoothGattDescriptor) o);
        } else {
            nextWrite();
        }
    }

    public Point3D convert_point3d(final byte[] value) {
		/*
		 * The accelerometer has the range [-2g, 2g] with unit (1/64)g.
		 *
		 * To convert from unit (1/64)g to unit g we divide by 64.
		 *
		 * (g = 9.81 m/s^2)
		 *
		 * The z value is multiplied with -1 to coincide with how we have arbitrarily defined the positive y direction. (illustrated by the apps accelerometer
		 * image)
		 */
        Integer x = (int) value[0];
        Integer y = (int) value[1];
        Integer z = (int) value[2] * -1;

        return new Point3D(x / 64.0, y / 64.0, z / 64.0);
    }

    @Override
    public int getButtonCount() {
        return 2;
    }

    @Override
    public boolean getButtonState(int index) {

        if ((keyEvent & (1 << index)) != 0) {
            keyEvent &= ~(1 << index);
            return true;
        } else {
            return false;
        }
    }

    private class Point3D {
        public double x, y, z;

        public Point3D(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            long temp;
            temp = Double.doubleToLongBits(x);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(y);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(z);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Point3D other = (Point3D) obj;
            if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
                return false;
            if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
                return false;
            if (Double.doubleToLongBits(z) != Double.doubleToLongBits(other.z))
                return false;
            return true;
        }
    }
}
