package com.example.neesarg.ece452;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import static com.example.neesarg.ece452.R.id.board;

public class GameplayActivity extends AppCompatActivity implements View.OnTouchListener {

    private BoardView boardView;
    private GameEngine gameEngine;
    float dX;
    float dY;
    int lastAction;
    float widthOfSquare;
    float origX = 0;
    float origY = 0;
    static char[][][] gameBoard = new char[3][3][3];

    private static final String TAG = "GameplayActivity";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;


    private String mConnectedDeviceName = null;
    private StringBuffer mOutStringBuffer;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothService bluetoothService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gameplay);
        boardView = findViewById(board);
        final View large1 = findViewById(R.id.l1);
        final View large2 = findViewById(R.id.l2);
        final View large3 = findViewById(R.id.l3);
        final View medium1 = findViewById(R.id.m1);
        final View medium2 = findViewById(R.id.m2);
        final View medium3 = findViewById(R.id.m3);
        final View small1 = findViewById(R.id.s1);
        final View small2 = findViewById(R.id.s2);
        final View small3 = findViewById(R.id.s3);
        large1.setOnTouchListener(this);
        large2.setOnTouchListener(this);
        large3.setOnTouchListener(this);
        medium1.setOnTouchListener(this);
        medium2.setOnTouchListener(this);
        medium3.setOnTouchListener(this);
        small1.setOnTouchListener(this);
        small2.setOnTouchListener(this);
        small3.setOnTouchListener(this);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        widthOfSquare = displayMetrics.widthPixels / 3;
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 3; z++) {
                    gameBoard[x][y][z] = '0';
                }
            }
        }
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth is not available", Toast.LENGTH_LONG).show();
        }

        final Button startButton = findViewById(R.id.start);
        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Performing this check in onResume() covers the case in which BT was
                // not enabled during onStart(), so we were paused to enable it...
                // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
                if (bluetoothService != null) {
                    // Only if the state is STATE_NONE, do we know that we haven't started already
                    if (bluetoothService.getState() == BluetoothService.STATE_NONE) {
                        // Start the Bluetooth chat services
                        Log.d("started", "started");
                        bluetoothService.start();
                    }
                }
            }
        });

        final Button connectButton = findViewById(R.id.connect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                connectDevice(getIntent(),false);
            }
        });

    }



    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            Toast.makeText(getApplicationContext(), "currently connected to " + mConnectedDeviceName,
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            Toast.makeText(getApplicationContext(), "connecting...",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            Toast.makeText(getApplicationContext(), "Not Connected.",
                                    Toast.LENGTH_SHORT).show();
                            break;
                    }
                    break;
                case 3:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    break;
                case 2:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Toast.makeText(getApplicationContext(), readMessage,
                            Toast.LENGTH_SHORT).show();
                    opponentMove(readMessage);
                    break;
                case 4:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString("device_name");
                    if (null != getApplicationContext()) {
                        Toast.makeText(getApplicationContext(), "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 5:
                    if (null != getApplicationContext()) {
                        Toast.makeText(getApplicationContext(), msg.getData().getString("toast"),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("mytag", "runniing on activity reslt");
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getApplicationContext(), "you have to enable bluetooth",
                            Toast.LENGTH_SHORT).show();
                   // getApplicationContext().finish();
                }
        }
    }

    /**
     * Establish connection with other device
     *
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(LobbyActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        Toast.makeText(getApplicationContext(), "the address is: " + address, Toast.LENGTH_SHORT).show();
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        bluetoothService.connect(device, secure);
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (bluetoothService == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bluetoothService != null) {
            bluetoothService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the BluetoothChatService to perform bluetooth connections
        bluetoothService = new BluetoothService(getApplicationContext(), mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
        //TO SENT MESSAGE CALL sendMessage(message)
    }

    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (bluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(getApplicationContext(), "its not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            bluetoothService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
        }
    }

    public void opponentMove(String readMessage) {
        int xCoord = Integer.parseInt(Character.toString(readMessage.charAt(0)));
        int yCoord = Integer.parseInt(Character.toString(readMessage.charAt(1)));
        int zCoord = Integer.parseInt(Character.toString(readMessage.charAt(2)));
        float pieceSize = 0;
        if (zCoord == 0) {
            pieceSize = findViewById(R.id.l1).getWidth();
        } else if (zCoord == 1) {
            pieceSize = findViewById(R.id.m1).getWidth();
        } else {
            pieceSize = findViewById(R.id.s1).getWidth();
        }
        float left = widthOfSquare * xCoord + (widthOfSquare - pieceSize) / 2;
        float top = widthOfSquare * yCoord + (widthOfSquare - pieceSize) / 2;

        RelativeLayout layout = findViewById(R.id.thisisalayout);
        ImageView imageView = new ImageView(this);
        imageView.setImageResource(R.drawable.l_squareo);
        imageView.setX(left);
        imageView.setY(top);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams((int) pieceSize, (int) pieceSize);
        imageView.setLayoutParams(layoutParams);
        layout.addView(imageView);
        gameBoard[xCoord][yCoord][zCoord] = 2;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        int xCoord = 0;
        int yCoord = 0;
        int zCoord = 0;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                origX = view.getX();
                origY = view.getY();
                dX = view.getX() - event.getRawX();
                dY = view.getY() - event.getRawY();
                lastAction = MotionEvent.ACTION_DOWN;
                break;

            case MotionEvent.ACTION_MOVE:
                view.setY(event.getRawY() + dY);
                view.setX(event.getRawX() + dX);
                lastAction = MotionEvent.ACTION_MOVE;
                break;

            case MotionEvent.ACTION_UP:
                if (getCenterXCoordinate(view) < widthOfSquare) {
                    view.setX(widthOfSquare / 2 - view.getWidth() / 2);
                    xCoord = 0;
                } else if (getCenterXCoordinate(view) > widthOfSquare && getCenterXCoordinate(view) < widthOfSquare * 2) {
                    view.setX(widthOfSquare * (float) 1.5 - view.getWidth() / 2);
                    xCoord = 1;
                } else if (getCenterXCoordinate(view) > widthOfSquare * 2) {
                    view.setX(widthOfSquare * (float) 2.5 - view.getWidth() / 2);
                    xCoord = 2;
                }


                if (getCenterYCoordinate(view) < widthOfSquare) {
                    view.setY(widthOfSquare / 2 - view.getWidth() / 2);
                    yCoord = 0;
                } else if (getCenterYCoordinate(view) > widthOfSquare && getCenterYCoordinate(view) < widthOfSquare * 2) {
                    view.setY(widthOfSquare * (float) 1.5 - view.getWidth() / 2);
                    yCoord = 1;
                } else if (getCenterYCoordinate(view) > widthOfSquare * 2 && getCenterYCoordinate(view) < widthOfSquare * 3) {
                    view.setY(widthOfSquare * (float) 2.5 - view.getWidth() / 2);
                    yCoord = 2;
                } else {
                    view.setX(origX);
                    view.setY(origY);
                    break;
                }

                if (view.getId() == R.id.l1 || view.getId() == R.id.l2 || view.getId() == R.id.l3 ) {
                    zCoord = 0;
                } else if (view.getId() == R.id.m1 || view.getId() == R.id.m2 || view.getId() == R.id.m3 ) {
                    zCoord = 1;
                } else {
                    zCoord = 2;
                }

                if (gameBoard[xCoord][yCoord][zCoord] == '0') {
                    //valid move
                    gameBoard[xCoord][yCoord][zCoord] = '1';
                    sendMessage(Integer.toString(xCoord) + Integer.toString(yCoord) + Integer.toString(zCoord));
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(), "This space is occupied.", Toast.LENGTH_SHORT);
                    toast.show();
                    view.setX(origX);
                    view.setY(origY);
                }

                break;

            default:
                return false;
        }
        return true;
    }

    public float getCenterXCoordinate (View view) {
        return view.getX() + view.getWidth()  / 2;
    }

    public float getCenterYCoordinate (View view) {
        return view.getY() + view.getHeight()  / 2;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_new_game) {
            newGame();
        }

        return super.onOptionsItemSelected(item);
    }


    private void newGame() {
        boardView.invalidate();
    }


}