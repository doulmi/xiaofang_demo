package com.xw.sample.dashboardviewdemo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.Channel;
import com.pusher.client.channel.ChannelEventListener;
import com.pusher.client.channel.SubscriptionEventListener;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private DashboardView4 mDashboardView4;

    private boolean isAnimFinished = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDashboardView4 = (DashboardView4) findViewById(R.id.dashboard_view_4);
        this.initPusher();
    }

    private void initPusher() {
        // Create a new Pusher instance
        PusherOptions options = new PusherOptions().setCluster("ap1").setEncrypted(true);
        Pusher pusher = new Pusher("345afdb48b47f7d355e6", options);

        pusher.connect(new ConnectionEventListener() {
            @Override
            public void onConnectionStateChange(ConnectionStateChange change) {
                Log.e("Pusher", "State changed to " + change.getCurrentState() +
                        " from " + change.getPreviousState());
            }

            @Override
            public void onError(String message, String code, Exception e) {
                Log.e("Pusher", "There was a problem connecting!" + code + message);
                e.getStackTrace();
                Log.e("Pusher", "What the fuck");
            }
        }, ConnectionState.ALL);

        // Subscribe to a channel
        Channel channel = pusher.subscribe("devices.DE5C62587B5E", new ChannelEventListener() {
            @Override
            public void onEvent(String channelName, String eventName, String data) {
                System.out.println("Date:" + data);
            }

            @Override
            public void onSubscriptionSucceeded(String channelName) {
                System.out.println("Subscribed to channel: " + channelName);
            }
            // Other ChannelEventListener methods
        });

        // Bind to listen for events called "my-event" sent to "my-channel"
        channel.bind("devicesUpdated", new SubscriptionEventListener() {
            @Override
            public void onEvent(String channel, String event, final String data) {
                Log.d("PUSHER", data);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateUI(data);
                    }
                });
//                Log.v("progress", progress + " % LEL");
//                waveProgress.setProgress((float) (progress / 100.0));
            }
        });

        // Disconnect from the service (or become disconnected my network conditions)
        pusher.disconnect();

        // Reconnect, with all channel subscriptions and event bindings automatically recreated
        pusher.connect();
        // The state change listener is notified when the connection has been re-established,
        // the subscription to "my-channel" and binding on "my-event" still exist.
    }

    private void updateUI(String data) {
        try {
            JSONObject jsonObject = new JSONObject(data);
            JSONObject device = jsonObject.getJSONObject("device");
            int progress = (int) device.getDouble("sensor");

            if (isAnimFinished) {
                Log.d("PUSHER", "" + progress);
                ObjectAnimator animator = ObjectAnimator.ofInt(mDashboardView4, "mRealTimeValue",
                        mDashboardView4.getVelocity(), progress);
                animator.setDuration(1500).setInterpolator(new LinearInterpolator());
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        isAnimFinished = false;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        isAnimFinished = true;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        isAnimFinished = true;
                    }
                });
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int value = (int) animation.getAnimatedValue();
                        mDashboardView4.setVelocity(value);
                    }
                });
                animator.start();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
