package info.akkuma.nearbysample;

import android.content.Intent;
import android.content.IntentSender;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.Strategy;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_RESOLVE_ERROR = 1;
    private static final String ENCODE = "UTF-8";

    private Button mSendButton;
    private EditText mEditText;
    private TextInputLayout mTextInputLayout;
    private MessageListAdapter mAdapter;
    private List<ChatMessage> mMessageList = new ArrayList<>();
    private boolean mResolvingError = false;

    private GoogleApiClient mGoogleApiClient;
    private NearbyConnectionCallbacks mConnectionCallbacks = new NearbyConnectionCallbacks();
    private NearbyConnectionFailedListener mFailedListener = new NearbyConnectionFailedListener();
    private Strategy mStrategy = new Strategy.Builder()
            .setDiscoveryMode(Strategy.DISCOVERY_MODE_DEFAULT)
            .setDistanceType(Strategy.DISTANCE_TYPE_DEFAULT)
            .setTtlSeconds(Strategy.TTL_SECONDS_DEFAULT)
            .build();

    private MessageListener mMessageListener = new MessageListener() {
        @Override
        public void onFound(Message message) {
            if (message != null) {
                String json;
                try {
                    json = new String(message.getContent(), ENCODE);
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, e.toString());
                    return;
                }
                addNewMessage(ChatMessage.fromJson(json));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(mConnectionCallbacks)
                .addOnConnectionFailedListener(mFailedListener)
                .build();

        mSendButton = (Button) findViewById(R.id.send_button);
        mEditText = (EditText) findViewById(R.id.edit_text);
        mTextInputLayout = (TextInputLayout) findViewById(R.id.text_input_layout);
        mAdapter = new MessageListAdapter(this, mMessageList);
        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setAdapter(mAdapter);

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode == RESULT_OK) {
                Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener);
            } else {
                Log.d(TAG, "Failed to resolve error with code " + resultCode);
            }
        }
    }

    private void sendMessage() {
        final ChatMessage chatMessage = new ChatMessage(mEditText.getText().toString(), System.currentTimeMillis());
        mEditText.setText("");
        mTextInputLayout.setHint(getString(R.string.hint_sending));
        if (getCurrentFocus() != null) {
            // hide ime
            InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }

        byte[] content;
        try {
            content = chatMessage.toString().getBytes(ENCODE);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.toString());
            return;
        }
        Message message = new Message(content, ChatMessage.TYPE_USER_CHAT);

        Nearby.Messages.publish(mGoogleApiClient, message, mStrategy)
        .setResultCallback(new ResultCallback<Status>(){
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    mTextInputLayout.setHint(getString(R.string.hint_input));
                    addNewMessage(chatMessage);
                } else {
                    if (status.hasResolution()) {
                        handleStartSolution(status);
                    } else {
                        Log.e(TAG, "sendMessage failed." + status.toString());
                    }
                }
            }
        });
    }

    private void handleStartSolution(Status status) {
        if (!mResolvingError) {
            try {
                status.startResolutionForResult(MainActivity.this, REQUEST_RESOLVE_ERROR);
                mResolvingError = true;
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    private void addNewMessage(ChatMessage message) {
        if (mMessageList.contains(message)) return;
        mMessageList.add(message);
        Collections.sort(mMessageList);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    private class NearbyConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle bundle) {
            Nearby.Messages.getPermissionStatus(mGoogleApiClient)
            .setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    if (status.isSuccess()) {
                        Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener);
                    } else {
                        if (status.hasResolution()) {
                            if (!mResolvingError) {
                                handleStartSolution(status);
                            }
                        } else {
                            Log.e(TAG, "sendMessage failed.");
                        }
                    }
                }
            });

        }

        @Override
        public void onConnectionSuspended(int i) {

        }
    }

    private class NearbyConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {

            Log.e(TAG, "connect failed.");
        }
    }
}