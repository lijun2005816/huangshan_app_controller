package se.i_gh.smartgreencontroller;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Objects;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import se.i_gh.smartgreencontroller.utils.Md5;

public class HzIotClient extends Service {
    public static String action = "hziot.broadcast.action";
    private String LOG_TAG = "HzIotClient";
    private MqttCallback callback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
            Log.e(LOG_TAG, "Connection lost: ", cause);
            Toast.makeText(HzIotClient.this, "Connection lost: " + cause.toString(), Toast.LENGTH_LONG).show();
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws UnsupportedEncodingException {
            String strData = new String(message.getPayload(), "UTF-8");
            Intent intent = new Intent(action);
            intent.putExtra("data", strData);
            sendBroadcast(intent);
            Log.i(LOG_TAG, String.format("Message arrived: %s:%s", topic, strData));
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            Log.i(LOG_TAG, String.format("Delivery complete: %s", (token == null || token.getResponse() == null) ? "null" : token.getResponse().getKey()));
            Toast.makeText(HzIotClient.this, String.format("Delivery complete: %s", (token == null || token.getResponse() == null) ? "null" : token.getResponse().getKey()), Toast.LENGTH_LONG).show();
        }
    };
    private String targetServer = "iot-as.aliyuncs.com:80";//接入服务器域名
    private String broker = "ssl://" + targetServer;
    private String productKey = "1000115125"; //这个是设备模版productkey
    private String productSecret = "jRl6CWrRbnsbRyeP";//这个是设备模版product secret
    private String sensorDeviceName = "sg_sensor";//这个是设备名称
    private String sensorDeviceSecret = "eIWjaKk00CGvsm02";//这个是设备秘钥
    private String controllerDeviceName = "sg_controller";//这个是设备名称
    private String controllerDeviceSecret = "bQucDCw5aryBR4ZJ";//这个是设备秘钥
    private String subTopic = String.format("/%s/%s/get", productKey, controllerDeviceName);
    private String pubTopic = String.format("/%s/%s/update", productKey, controllerDeviceName);
    private String controllerClientId = String.format("%s:%s:1:1.0.0", productKey, controllerDeviceName);
    private MqttClient mqttClient;

    private static SSLSocketFactory createSSLSocket(Context context) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate ca = null;
        try (InputStream in = context.getAssets().open("pubkey.crt")) {
            ca = cf.generateCertificate(in);
        } catch (CertificateException e) {
            e.printStackTrace();
        }
        String keyStoreType = KeyStore.getDefaultType();
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init(keyStore);
        SSLContext iContext = SSLContext.getInstance("TLSV1.2");
        iContext.init(null, tmf.getTrustManagers(), null);
        return iContext.getSocketFactory();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            SSLSocketFactory socketFactory;
            socketFactory = createSSLSocket(this);
            mqttClient = new MqttClient(broker, controllerClientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setMqttVersion(4);
            connOpts.setSocketFactory(socketFactory);
            connOpts.setAutomaticReconnect(true);
            String sign = productKey + productSecret + controllerDeviceName + controllerDeviceSecret;
            String signUserName = Md5.getInstance().md5_32(sign).toUpperCase();
            connOpts.setUserName(signUserName);
            connOpts.setKeepAliveInterval(80);
            Objects.requireNonNull(mqttClient).connect(connOpts);
            Objects.requireNonNull(mqttClient).setCallback(callback);
            mqttClient.subscribe(subTopic);
            Toast.makeText(HzIotClient.this, String.format("HzIotClient connection success"), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String command = intent.getStringExtra("command");
        if (!command.equals("")) {
            pubCommand(command);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        try {
            mqttClient.unsubscribe(subTopic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    public void pubCommand(String command) {
        try {
            MqttMessage message = new MqttMessage(command.getBytes());
            message.setQos(0);
            mqttClient.publish(pubTopic, message);
            Log.i(LOG_TAG, command);
            Toast.makeText(HzIotClient.this, String.format("Command [%s] push success", command), Toast.LENGTH_LONG).show();
        } catch (MqttException e) {
            Log.e(LOG_TAG, "Publish exception: ", e);
        }
    }
}
