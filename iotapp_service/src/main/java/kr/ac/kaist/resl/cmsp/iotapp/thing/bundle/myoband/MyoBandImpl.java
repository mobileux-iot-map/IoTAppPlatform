package kr.ac.kaist.resl.cmsp.iotapp.thing.bundle.myoband;

import android.content.Context;
import com.thalmic.myo.*;
import kr.ac.kaist.resl.cmsp.iotapp.library.ThingServiceEndpoint;
import kr.ac.kaist.resl.cmsp.iotapp.thing.MyoBandService;

/**
 * Created by shheo on 15. 8. 24.
 */
public class MyoBandImpl implements MyoBandService {
    private final static String TAG = MyoBandImpl.class.getSimpleName();
    private ThingServiceEndpoint endpoint;
    private boolean isConnected = false;
    private static final int MYO_GESTURE_NO_GESTURE = 0;
    private static final int MYO_GESTURE_BIG_TWIST = 1;
    private int lastGesture;

    public MyoBandImpl(Context context) {
        MyoHub = Hub.getInstance();

        //need to perform hub.init!! using context in android.
    	if (!MyoHub.init(context, context.getPackageName())) {
            // We can't do anything with the Myo device if the Hub can't be initialized, so exit.
            System.out.println("initialization is failed");
            return;
        }

        DeviceListener mListener = new AbstractDeviceListener() {

            // onConnect() is called whenever a Myo has been connected.
            @Override
            public void onConnect(Myo myo, long timestamp) {
                myo.unlock(Myo.UnlockType.HOLD);
                MyoDevice = myo;
                startCheckingGesture();
                isConnected = true;
                // do something when it connected
            }

            // onDisconnect() is called whenever a Myo has been disconnected.
            @Override
            public void onDisconnect(Myo myo, long timestamp) {
                // do something when it disconnected
                MyoDevice = null;
                isConnected = false;
            }

            // onArmSync() is called whenever Myo has recognized a Sync Gesture after someone has put it on their
            // arm. This lets Myo know which arm it's on and which way it's facing.
            @Override
            public void onArmSync(Myo myo, long timestamp, Arm arm, XDirection xDirection) {
                //do something when sync procedure is performed
            }

            // onArmUnsync() is called whenever Myo has detected that it was moved from a stable position on a person's arm after
            // it recognized the arm. Typically this happens when someone takes Myo off of their arm, but it can also happen
            // when Myo is moved around on the arm.
            @Override
            public void onArmUnsync(Myo myo, long timestamp) {
                //mTextView.setText(R.string.hello_world);
            }

            // onUnlock() is called whenever a synced Myo has been unlocked. Under the standard locking
            // policy, that means poses will now be delivered to the listener.
            @Override
            public void onUnlock(Myo myo, long timestamp) {
                //mLockStateView.setText(R.string.unlocked);
            }

            // onLock() is called whenever a synced Myo has been locked. Under the standard locking
            // policy, that means poses will no longer be delivered to the listener.
            @Override
            public void onLock(Myo myo, long timestamp) {
                //mLockStateView.setText(R.string.locked);
            }

            // onOrientationData() is called whenever a Myo provides its current orientation,
            // represented as a quaternion.
            @Override
            public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
                // Calculate Euler angles (roll, pitch, and yaw) from the quaternion.
                roll = (float) Math.toDegrees(Quaternion.roll(rotation));
                pitch = (float) Math.toDegrees(Quaternion.pitch(rotation));
                yaw = (float) Math.toDegrees(Quaternion.yaw(rotation));

                // Adjust roll and pitch for the orientation of the Myo on the arm.
                if (myo.getXDirection() == XDirection.TOWARD_ELBOW) {
                    roll *= -1;
                    pitch *= -1;
                }

                // Next, we apply a rotation to the text view using the roll, pitch, and yaw.
                //mTextView.setRotation(roll);
                //mTextView.setRotationX(pitch);
                //mTextView.setRotationY(yaw);

                // we will use this part for airplain
            }

            // onPose() is called whenever a Myo provides a new pose.
            @Override
            public void onPose(Myo myo, long timestamp, Pose pose) {
                // Handle the cases of the Pose enumeration, and change the text of the text view
                // based on the pose we receive.
                switch (pose) {
                    case UNKNOWN:
                        //mTextView.setText(getString(R.string.hello_world));
                        break;
                    case REST:
                    case DOUBLE_TAP:
                        //int restTextId = R.string.hello_world;
                        switch (myo.getArm()) {
                            case LEFT:
                                //restTextId = R.string.arm_left;
                                break;
                            case RIGHT:
                                //restTextId = R.string.arm_right;
                                break;
                        }
                        //mTextView.setText(getString(restTextId));
                        break;
                    case FIST:
                        //mTextView.setText(getString(R.string.pose_fist));
                        break;
                    case WAVE_IN:
                        //mTextView.setText(getString(R.string.pose_wavein));
                        break;
                    case WAVE_OUT:
                        //mTextView.setText(getString(R.string.pose_waveout));
                        break;
                    case FINGERS_SPREAD:
                        //mTextView.setText(getString(R.string.pose_fingersspread));
                        break;
                }

                if (pose != Pose.UNKNOWN && pose != Pose.REST) {
                    // Tell the Myo to stay unlocked until told otherwise. We do that here so you can
                    // hold the poses without the Myo becoming locked.
                    //myo.unlock(Myo.UnlockType.HOLD);

                    // Notify the Myo that the pose has resulted in an action, in this case changing
                    // the text on the screen. The Myo will vibrate.
                    myo.notifyUserAction();
                } else {
                    // Tell the Myo to stay unlocked only for a short period. This allows the Myo to
                    // stay unlocked while poses are being performed, but lock after inactivity.
                    //myo.unlock(Myo.UnlockType.TIMED);
                }
            }
        };
        MyoHub.addListener(mListener);
    }

    @Override
    public int getCurrentGesture() {
        int toReturn = lastGesture;
        // reset last gesture
        lastGesture = MYO_GESTURE_NO_GESTURE;
        return toReturn;
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
        MyoHub.attachByMacAddress(endpoint.getEndpoint());
    }

    @Override
    public void disconnect() {
        MyoHub.detach(endpoint.getEndpoint());
    }

    @Override
    public Boolean isConnected() {
        return isConnected;
    }


    private static Hub MyoHub;
    private static Myo MyoDevice;
    float roll = -1;
    float pitch = -1;
    float yaw = -1;


    //* you can make this as a whole *//
    public float getRoll(){
        return this.roll;
    }

    public float getPitch(){
        return this.pitch;
    }

    public float getYaw(){
        return this.yaw;

    }

    public void startCheckingGesture() {
        (new Thread(new Runnable() {
            @Override
            public void run() {
                float movingRollAverage = 0.0f;
                while (isConnected) {
                    if (Math.abs(MyoBandImpl.this.roll - movingRollAverage) > 50) {
                        // find outlier roll
                        MyoBandImpl.this.lastGesture = MYO_GESTURE_BIG_TWIST;
                        MyoDevice.vibrate(Myo.VibrationType.SHORT);
                    }
                    // update moving average
                    movingRollAverage = (movingRollAverage * 4 + MyoBandImpl.this.roll) / 5;

                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        })).start();
    }
}
