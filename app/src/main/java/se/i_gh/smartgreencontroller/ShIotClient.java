package se.i_gh.smartgreencontroller;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.aliyun.alink.linksdk.channel.core.base.AError;
import com.aliyun.alink.linksdk.channel.core.base.ARequest;
import com.aliyun.alink.linksdk.channel.core.base.AResponse;
import com.aliyun.alink.linksdk.channel.core.base.IOnCallListener;
import com.aliyun.alink.linksdk.channel.core.persistent.IOnSubscribeListener;
import com.aliyun.alink.linksdk.channel.core.persistent.PersistentNet;
import com.aliyun.alink.linksdk.channel.core.persistent.event.IConnectionStateListener;
import com.aliyun.alink.linksdk.channel.core.persistent.event.IOnPushListener;
import com.aliyun.alink.linksdk.channel.core.persistent.event.PersistentEventDispatcher;
import com.aliyun.alink.linksdk.channel.core.persistent.mqtt.MqttInitParams;
import com.aliyun.alink.linksdk.channel.core.persistent.mqtt.request.MqttPublishRequest;

public class ShIotClient extends Service {
    public static String action = "shiot.broadcast.action";
    private String LOG_TAG = "ShIotClient";
    public IOnCallListener onCallListener = new IOnCallListener() {
        @Override
        public void onSuccess(ARequest request, AResponse response) {
            Log.i(LOG_TAG, "Send success");
            Toast.makeText(ShIotClient.this, String.format("Message[%s] send success", request.toString()), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onFailed(ARequest request, AError error) {
            Log.i(LOG_TAG, "Send failed");
            Toast.makeText(ShIotClient.this, String.format("Message[%s] send failed", request.toString()), Toast.LENGTH_LONG).show();
        }

        @Override
        public boolean needUISafety() {
            return false;
        }
    };
    public IOnSubscribeListener onSubscribeListener = new IOnSubscribeListener() {
        @Override
        public void onSuccess(String s) {
            Log.i(LOG_TAG, "Subscribe success");
            Toast.makeText(ShIotClient.this, String.format("Subscribe success [%s]", s), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onFailed(String s, AError aError) {
            Log.i(LOG_TAG, "Subscribe failed");
            Toast.makeText(ShIotClient.this, String.format("Subscribe failed [%s]", s), Toast.LENGTH_LONG).show();
        }

        @Override
        public boolean needUISafety() {
            return false;
        }
    };
    public IOnPushListener onPushListener = new IOnPushListener() {
        @Override
        public void onCommand(String topic, String data) {
            Intent intent = new Intent(action);
            intent.putExtra("data", data);
            sendBroadcast(intent);
            Log.i(LOG_TAG, topic + ":" + data);
            Toast.makeText(ShIotClient.this, String.format("Push [%s:%s] success", topic, data), Toast.LENGTH_LONG).show();
        }

        @Override
        public boolean shouldHandle(String topic) {
            return true;
        }
    };
    private String productKey = "a1a5Glt8cL5";
    private String deviceName = "rNQOXGcMAE5Sw65zU9wu";
    private String deviceSecret = "e2uHC5enrruwKW9MJxDkaPB8QE62owXU";
    private String pubTopic = String.format("/%s/%s/update", productKey, deviceName);
    private String subTopic = String.format("/%s/%s/get", productKey, deviceName);
    public IConnectionStateListener channelStateListener = new IConnectionStateListener() {
        @Override
        public void onConnectFail(String msg) {
            Log.e(LOG_TAG, "Connect fail");
        }

        @Override
        public void onConnected() {
            Log.i(LOG_TAG, "Connect success");
            Toast.makeText(ShIotClient.this, String.format("ShIotClient connection success"), Toast.LENGTH_LONG).show();
            MqttPublishRequest publishRequest = new MqttPublishRequest();
            publishRequest.isRPC = false;
            publishRequest.topic = pubTopic;
            publishRequest.payloadObj = "{'citycode':'0' , 'status':'ok'}";
            PersistentNet.getInstance().asyncSend(publishRequest, onCallListener);
            PersistentNet.getInstance().subscribe(subTopic, onSubscribeListener);
        }

        @Override
        public void onDisconnect() {
            Log.e(LOG_TAG, "Disconnect");
            Toast.makeText(ShIotClient.this, String.format("ShIotClient disconnect"), Toast.LENGTH_LONG).show();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        //link to aliyun
        MqttInitParams initParams = new MqttInitParams(productKey, deviceName, deviceSecret);
        PersistentNet.getInstance().init(this, initParams);
        PersistentEventDispatcher.getInstance().registerOnTunnelStateListener(channelStateListener, true);// 注册连接云端监听回调函数
        PersistentEventDispatcher.getInstance().registerOnPushListener(onPushListener, true);// 注册下行数据监听回调函数
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        PersistentNet.getInstance().unSubscribe(subTopic, onSubscribeListener);
        PersistentEventDispatcher.getInstance().unregisterOnPushListener(onPushListener);//取消监听
        PersistentEventDispatcher.getInstance().unregisterOnTunnelStateListener(channelStateListener);//取消监听
        super.onDestroy();
    }
}
