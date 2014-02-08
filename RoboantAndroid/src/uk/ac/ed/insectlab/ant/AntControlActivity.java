package uk.ac.ed.insectlab.ant;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import uk.ac.ed.insectlab.ant.RouteSelectionDialogFragment.RouteSelectedListener;
import uk.co.ed.insectlab.ant.R;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

public class AntControlActivity extends Activity implements RouteSelectedListener {

	private static final int CAMERA_NUMBER = 1;

	private static final long CAMERA_TIMEOUT = 1000;
	private final String TAG = AntControlActivity.class.getSimpleName();

	private final String SERVER_IP = "192.168.1.7";

	/**
	 * Driver instance, passed in statically via
	 * {@link #show(Context, UsbSerialDriver)}.
	 *
	 * <p/>
	 * This is a devious hack; it'd be cleaner to re-create the driver using
	 * arguments passed in with the {@link #startActivity(Intent)} intent. We
	 * can get away with it because both activities will run in the same
	 * process, and this is a simple demo.
	 */
	private static UsbSerialDriver sDriver = null;

	Pattern mPattern = Pattern.compile("(l|r)\\s(-?\\d+)");

	private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

	private SerialInputOutputManager mSerialIoManager;

	private final SerialInputOutputManager.Listener mListener =
			new SerialInputOutputManager.Listener() {

		@Override
		public void onRunError(Exception e) {
			Log.d(TAG, "Runner stopped.");
		}

		@Override
		public void onNewData(final byte[] data) {
			AntControlActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					AntControlActivity.this.updateReceivedData(data);
				}
			});
		}
	};

	private EditText mInputField;

	private int mRightSpeed = 0;

	private int mLeftSpeed = 0;
	private EditText mLeftSpeedIndicator;
	private EditText mRightSpeedIndicator;
	private EditText mServerIP;
	private EditText mServerPort;

	private ArduinoZumoControl mRoboAntControl;

	private Pattern mSpeedPattern = Pattern.compile("l(-?\\d+)r(-?\\d+)");

	private Handler handler;

	private Runnable mConnectorRunnable;

	private WakeLock mWakeLock;

	private SeekBar mLeftSeek;

	private SeekBar mRightSeek;

	private Button mCaptureButton;

	private MediaRecorder mMediaRecorder;

	private long mCamMessageRecievedAt = System.currentTimeMillis();

	private TextView mAIMessage;

	private TextView mRecordingText;

	private ProgressBar mTrackProgressBar;

	private OpenCVCamera mCamera;

	private CameraBridgeViewBase mOpenCvCameraView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.serial_console);
		
		mRoboAntControl = new ArduinoZumoControl() {

			@Override
			public void setSpeeds(int turnSpeed, int i) {
				Log.i(TAG, "Dummy setSpeeds");
			}

			@Override
			public void simpleTurnInPlaceBlocking(int i, int turnTime) {
				Log.i(TAG, "Dummy simpleTurnInPlaceBlocking");
				
			}

			@Override
			public void doGoTowards(LookAroundListener aiControlTask,
					int minStep) {
				Log.i(TAG, "Dummy doGoTowards");
				
			}

			@Override
			public void doLookAroundStep(LookAroundListener aiControlTask) {
				Log.i(TAG, "Dummy doLookAroundStep");
				
			}

			@Override
			public void setLeftSpeed(int speed) {
				Log.i(TAG, "Dummy setLeftSpeed");
				
			}

			@Override
			public void setRightSpeed(int speed) {
				Log.i(TAG, "Dummy setRightSpeed");
			}

			@Override
			public void calibrate() {
				Log.i(TAG, "Dummy calibrate");
				
			}

			@Override
			public void lookAroundDone() {
				Log.i(TAG, "Dummy lookAroundDone");
				
			}

			@Override
			public void goTowardsDone() {
				Log.i(TAG, "Dummy goTowardsDone");
				// TODO Auto-generated method stub
				
			}

			@Override
			public void lookAroundStepDone(int parseInt) {
				Log.i(TAG, "Dummy lookAroundStepDone");
				
			}
			
		};
		mCurrentStepPic = (ImageView)findViewById(R.id.pic_current_step);
		mStepTowardsPic = (ImageView)findViewById(R.id.pic_step_towards);

		handler = new Handler();
		mTrackProgressBar = (ProgressBar)findViewById(R.id.track_proress);
		mTrackProgressBar.setVisibility(View.GONE);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		mLeftSpeedIndicator = (EditText)findViewById(R.id.leftValue);
		mRightSpeedIndicator = (EditText)findViewById(R.id.rightValue);

		Button capture = (Button)findViewById(R.id.btn_take_picture);
		capture.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				takePicture();
			}

		});

		final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
		this.mWakeLock.acquire();

		mAIMessage = (TextView)findViewById(R.id.ai_message);

		mLeftSeek = (SeekBar)findViewById(R.id.left);
		mRightSeek = (SeekBar)findViewById(R.id.right);
		OnSeekBarChangeListener seeker = new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				switch (seekBar.getId()) {
				case R.id.left:
					//					mRoboAntControl.setLeftSpeed(progress);
					//					Log.i(TAG, "Setting left speed to " + progress);
					break;
				case R.id.right:
					//					mRoboAntControl.setRightSpeed(progress);
					//					Log.i(TAG, "Setting right speed to " + progress);
					break;

				default:
					break;
				}

			}


		};

		mRecordingText = (TextView)findViewById(R.id.path_recording);
		mRecordingText.setVisibility(View.GONE);

		mLeftSeek.setOnSeekBarChangeListener(seeker);
		mRightSeek.setOnSeekBarChangeListener(seeker);

		mServerIP = (EditText)findViewById(R.id.serverIP);
		mServerPort = (EditText)findViewById(R.id.serverPort);

		mServerIP.setText(SERVER_IP);
		mServerPort.setText("1234");

		postConnectorRunnable();

		Camera camera =  Util.getCameraInstance(CAMERA_NUMBER);
		Util.setCameraDisplayOrientation(this, CAMERA_NUMBER, camera);
		camera.release();

		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_preview);
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mCamera = new OpenCVCamera();


		mOpenCvCameraView.setCvCameraViewListener(mCamera);
	}

	private void takePicture() {
		if (mCamera != null) {
			if (mAIControl != null) {
				takeAIPicture();
			}
			else {
//				mCamera.getPicture(new OpenCVCamera.PictureListener() {
//					@Override
//					public void pictureReceived(final Bitmap picture) {
//						handler.post(new Runnable() {
//
//							@Override
//							public void run() {
//								mStepTowardsPic.setImageBitmap(picture);
//
//							}
//						});
//					}
//				});
				mStepTowardsPic.setImageBitmap(getCameraPicture());
			}
		}
	}

	private Bitmap getCameraPicture() {
		return mCamera.getPicture();
	}

	private void showRouteSelectionDialog() {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag("dialog");
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);

		// Create and show the dialog.
		DialogFragment newFragment = new RouteSelectionDialogFragment();
		newFragment.show(ft, "dialog");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_activity_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
		case R.id.action_motor_test:
			new MotorTestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mRoboAntControl);
			return true;
		case R.id.action_ai_toggle:
			toggleAI();
			return true;
		case R.id.action_calibrate: 
			mRoboAntControl.calibrate();
			return true;
		case R.id.action_select_route:
			showRouteSelectionDialog();
			return true;
		case R.id.action_forget_lens:
			mCamera.forgetLens();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void takeAIPicture() {
		mAIControl.getHandler().post(new Runnable() {

			@Override
			public void run() {
				mAIControl.stepTowards();
			}
		});

	}

	private boolean isRecording = false;


	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS:
			{
				Log.i(TAG, "OpenCV loaded successfully");
				mOpenCvCameraView.enableView();
			} break;
			default:
			{
				super.onManagerConnected(status);
			} break;
			}
		}
	};



	private TcpClient mTcpClient;

	private AIControlTask mAIControl;

	private String messageBuffer;

	private Pattern mLookAroundPattern = Pattern.compile("la\\s(g|d|\\d+)");

	private boolean mRecordingRoute;

	private ImageView mCurrentStepPic;

	private ImageView mStepTowardsPic;


	public class ConnectTask extends AsyncTask<Void, String, TcpClient> {

		@Override
		protected TcpClient doInBackground(Void... nothing) {
			if (mTcpClient != null) {
				return null;
			}
			//we create a TCPClient object and
			mTcpClient = new TcpClient(new TcpClient.OnMessageReceived() {

				@Override
				//here the messageReceived method is implemented
				public void messageReceived(String message) {
					//this method calls the onProgressUpdate
					publishProgress(message);
				}

				@Override
				public void disconnected() {
					//					Log.i(TAG, "Disconnected");
					mTcpClient = null;

					AntControlActivity.this.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							mServerIP.setEnabled(true);
							mServerPort.setEnabled(true);
						}
					});

					postConnectorRunnable();
				}



				@Override
				public void connected() {
					AntControlActivity.this.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							mServerIP.setEnabled(false);
							mServerPort.setEnabled(false);
						}
					});
					if (mAIControl != null) {
						mAIControl.setNetworkControl(mTcpClient);
					}
					handler.removeCallbacks(mConnectorRunnable);
				}
			});

			mTcpClient.run(mServerIP.getText().toString(), Integer.parseInt(mServerPort.getText().toString()), handler);
			return null;
		}

		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
			handleMessage(values[0]);
		}

	}

	private void postConnectorRunnable() {
		final int period = 2000;

		mConnectorRunnable = new Runnable() {

			public void run() {
				if (mTcpClient != null) {
					return;
				}
				//				Log.i(TAG, "Trying to connect to server");
				new ConnectTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				handler.postDelayed(this, period);
			}
		};

		handler.postDelayed(mConnectorRunnable, period);
	}

	private void handleMessage(String message) {
		Matcher matcher = mPattern.matcher(message);
		if (matcher.find()) {
			int speed = Integer.parseInt(matcher.group(2));
			if (mAIControl != null) {
				return;
			}
			if (mRoboAntControl == null) {
				Log.i(TAG, "mRoboAntControl is null message is " + message);
				return;
			}
			if (matcher.group(1).equals("l")) {
				mRoboAntControl.setLeftSpeed(speed);
			}
			else {
				mRoboAntControl.setRightSpeed(speed);
			}
		}
		else if (message.startsWith("cam")) {
			Log.i(TAG, "cam message!");
			if (mCamMessageRecievedAt + CAMERA_TIMEOUT > System.currentTimeMillis()) {
				Log.i(TAG, "ignoring cam message");
				return;
			}
			mCamMessageRecievedAt = System.currentTimeMillis();
		}
		else if (message.startsWith("ai")) {
			Log.i(TAG, "ai message!");
			toggleAI();

		}
		else if (message.startsWith("pic")) {
			Log.i(TAG, "pic message!");
			takePicture();
		}
		else if (message.startsWith("rec")) {
			Log.i(TAG, "rec message!");
			if (!mRecordingRoute) {
				postRecordingRunnable();
				mRecordingRoute = true;
				mRecordingText.setVisibility(View.VISIBLE);
			}
			else {
				mRecordingRoute = false;
				handler.removeCallbacks(mRecordingRunnable);
				synchronized (mRoutePictures) {
					new SaveRecordedRouteTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mRoutePictures);
				}
				mRecordingText.setVisibility(View.INVISIBLE);
			}
		}
		else if (message.startsWith("calibrate")) {
			Log.i(TAG, "Calibrate message");
			if (mRoboAntControl != null) {
				mRoboAntControl.calibrate();
			}
		}
		else if (message.startsWith("go")) {
			if (mAIControl != null) {
				mAIControl.releaseLock();
			}
		}
	}

	private void toggleAI() {
		if (mAIControl != null) {
			mAIControl.stop();
			mAIMessage.setText("");
			mAIControl = null;
		}
		else {
			TextView currentStepNum = (TextView)findViewById(R.id.current_step_num);
			TextView stepTowardsNum = (TextView)findViewById(R.id.step_towards_num);
			if (mTcpClient == null) {
				NetworkControl dummy = new NetworkControl() {
					@Override
					public void sendPicture(RoboPicture roboPicture) {
						Log.i(TAG, "Ignore sendPicture " + roboPicture.pictureNum);
					}

					@Override
					public void sendMessage(String message) {
						Log.i(TAG, "Ignore sendMessage " + message);
					}
				};
				mAIControl = new AIControlTask(mCamera, dummy, mAIMessage, mCurrentStepPic, mStepTowardsPic, currentStepNum, stepTowardsNum, mTrackProgressBar, mRoutePictures);
			}
			else {
				mAIControl = new AIControlTask(mCamera, mTcpClient, mAIMessage, mCurrentStepPic, mStepTowardsPic, currentStepNum, stepTowardsNum, mTrackProgressBar, mRoutePictures);
			}
			mAIControl.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mRoboAntControl);
		}

	}

	private long ROUTE_PICTURES_DELAY = 1000;
	List<Bitmap> mRoutePictures = new LinkedList<Bitmap>();
	private Runnable mRecordingRunnable;
	private void postRecordingRunnable() {
		for (Bitmap bmp : mRoutePictures) {
			bmp.recycle();
		}
		mRoutePictures.clear();

		handler.post(new Runnable() {

			@Override
			public void run() {
				synchronized (mRoutePictures) {
					if (!mRecordingRoute) {
						return;
					}

					mRecordingRunnable = this;
					mRoutePictures.add(getCameraPicture());
//					mCamera.getPicture(new OpenCVCamera.PictureListener() {
//						@Override
//						public void pictureReceived(Bitmap picture) {
//							mRoutePictures.add(picture);
//						}
//					});
					try {
						handler.postDelayed(this, ROUTE_PICTURES_DELAY);
					}
					catch (Exception e) {
						e.printStackTrace();
						handler.postDelayed(this, 100);
					}
				}
			}
		});
	}

	@Override
	protected void onPause() {
		super.onPause();
		this.mWakeLock.release();
		stopIoManager();
		if (sDriver != null) {
			try {
				sDriver.close();
			} catch (IOException e) {
				// Ignore.
			}
			sDriver = null;
		}
		if (mTcpClient != null) {
			mTcpClient.stopClient();
		}
		if (mConnectorRunnable != null) {
			handler.removeCallbacks(mConnectorRunnable);
		}
		if (mOpenCvCameraView != null) {
			mOpenCvCameraView.disableView();
		}
		finish();
	}

	@Override
	protected void onResume() {
		super.onResume();

		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);

		Log.d(TAG, "Resumed, sDriver=" + sDriver);
		if (sDriver == null) {
			Toast.makeText(this, "No serial device", Toast.LENGTH_LONG).show();;
		} else {
			try {
				sDriver.open();
				sDriver.setParameters(115200, 8, UsbSerialDriver.STOPBITS_1, UsbSerialDriver.PARITY_NONE);
			} catch (IOException e) {
				Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
				//                mTitleTextView.setText("Error opening device: " + e.getMessage());
				Toast.makeText(this, "Error opening device", Toast.LENGTH_LONG).show();;
				try {
					sDriver.close();
				} catch (IOException e2) {
					// Ignore.
				}
				sDriver = null;
				return;
			}
			Toast.makeText(this, "Serial device " + sDriver.getClass().getSimpleName(), Toast.LENGTH_LONG).show();;
		}
		onDeviceStateChange();
		postConnectorRunnable();
	}

	private void stopIoManager() {
		if (mSerialIoManager != null) {
			Log.i(TAG, "Stopping io manager ..");
			mSerialIoManager.stop();
			mSerialIoManager = null;
			mRoboAntControl = null;
		}
	}

	private void startIoManager() {
		if (sDriver != null) {
			Log.i(TAG, "Starting io manager ..");
			mSerialIoManager = new SerialInputOutputManager(sDriver, mListener);
			mExecutor.submit(mSerialIoManager);
			mRoboAntControl = new RoboAntControl(mSerialIoManager);
		}
	}

	private void onDeviceStateChange() {
		stopIoManager();
		startIoManager();
	}

	private void updateReceivedData(byte[] data) {
		String message = new String(data);
		if (messageBuffer != null) {
			message = messageBuffer + message;
		}
		Log.i(TAG, "Message from serial: " + message);
		Matcher matcher = mSpeedPattern.matcher(message);
		Matcher laMatcher = mLookAroundPattern.matcher(message);
		if (laMatcher.find()) {
			Log.i(TAG, "Look around message received!");
			if (laMatcher.group(1).equals("d")) {
				Log.i(TAG, "Look around done!");
				mRoboAntControl.lookAroundDone();
			}
			else if (laMatcher.group(1).equals("g")) {
				Log.i(TAG, "Go towards done!");
				mRoboAntControl.goTowardsDone();
			}
			else {
				Log.i(TAG, "Look around step " + laMatcher.group(1));
				mRoboAntControl.lookAroundStepDone(Integer.parseInt(laMatcher.group(1)));
			}
			messageBuffer = null;
		}
		else if (matcher.find()) {
			mLeftSpeedIndicator.setText(matcher.group(1));
			mRightSpeedIndicator.setText(matcher.group(2));
			mLeftSeek.setProgress(Integer.parseInt(matcher.group(1)));
			mRightSeek.setProgress(Integer.parseInt(matcher.group(2)));
			messageBuffer = null;
		}
		else {
			messageBuffer = message;
		}
	}

	/**
	 * Starts the activity, using the supplied driver instance.
	 *
	 * @param context
	 * @param driver
	 */
	static void show(Context context, UsbSerialDriver driver) {
		sDriver = driver;
		final Intent intent = new Intent(context, AntControlActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
		context.startActivity(intent);
	}

	@Override
	protected void onStop() {

		super.onStop();
	}

	@Override
	public void onRouteSelected(List<Bitmap> bitmap) {
		mRoutePictures = bitmap;
	}

}
