package se.i_gh.smartgreencontroller;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

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
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import se.i_gh.smartgreencontroller.utils.Md5;

/**
 * Created by garylu on 2017/4/22.
 */

public class IotClient {
    final static String LOG_TAG = "sg_controller_iotclient";

    final static int MQ_ON_CONNECTION_LOST = 0;
    final static int MQ_ON_MSG_ARRIVED = 1;
    final static int MQ_ON_DELIVER_COMPLETE = 2;

    static String targetServer = "iot-as.aliyuncs.com:80";//接入服务器域名
    static String broker = "ssl://" + targetServer;

    static String productKey = "1000115125"; //这个是设备模版productkey
    static String productSecret = "jRl6CWrRbnsbRyeP";//这个是设备模版product secret

    static String sensorDeviceName = "sg_sensor";//这个是设备名称
    static String sensorDeviceSecret = "eIWjaKk00CGvsm02";//这个是设备秘钥

    static String controllerDeviceName = "sg_controller";//这个是设备名称
    static String controllerDeviceSecret = "bQucDCw5aryBR4ZJ";//这个是设备秘钥


    //sensor data topic
    static String subTopic = "/" + productKey + "/" + controllerDeviceName + "/get";

    //controller command topic
    static String pubTopic = "/" + productKey + "/" + controllerDeviceName + "/update";

    //客户端ID格式: productKey : deviceId:idtype:sdkVersion ,如果idtype＝1则 代表设备id是用户自定义的设备名称
    static String controllerClientId = productKey + ":" + controllerDeviceName + ":" + "1"+":1.0.0";


    static private IotClient iotClientInst = null;

    MqttClient mqttClient = null;

    private void setMqttClient(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    private int subStatus = 0;

    private static Context context;

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    private static TextView dataTv;

    static Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            String datetime = sdf.format(new Date());
            switch (msg.what) {
                case MQ_ON_CONNECTION_LOST:
                    dataTv.append(datetime +" ERROR: 连接失败,原因:" + msg.obj + "\n");
                    break;
                case MQ_ON_MSG_ARRIVED:
                    MqttMessage mqttMsg = (MqttMessage)msg.obj;
                    try {
                        dataTv.append(datetime + " DATA: " + new String(mqttMsg.getPayload(), "UTF-8") + "\n");
                    } catch (UnsupportedEncodingException e) {
                        Log.e(LOG_TAG, "UnsupportedEncodingException:utf-8", e);
                    }
                    break;
                case MQ_ON_DELIVER_COMPLETE:
                    dataTv.append(datetime +" 指令推送成功\n");
                    break;
                default:

            }
        }
    };

    public static IotClient getSubInstance(Context context) throws Exception {
        if (iotClientInst==null) {
            iotClientInst = new IotClient();
            iotClientInst.setContext(context);

            MemoryPersistence persistence = new MemoryPersistence();
            SSLSocketFactory socketFactory = createSSLSocket();
            final MqttClient sampleClient = new MqttClient(broker, controllerClientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setMqttVersion(4);// MQTT 3.1.1
            connOpts.setSocketFactory(socketFactory);
            //设置是否自动重连
            connOpts.setAutomaticReconnect(true);

            //用户名签名
            String sign = productKey + productSecret + controllerDeviceName + controllerDeviceSecret;
            String signUserName = Md5.getInstance().md5_32(sign).toUpperCase();

            connOpts.setUserName(signUserName);
            connOpts.setKeepAliveInterval(80);
            Log.i(LOG_TAG, controllerClientId + "，进行连接, 目的地: " + broker);
            sampleClient.connect(connOpts);
            sampleClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Message message = Message.obtain();
                    message.what = MQ_ON_CONNECTION_LOST;
                    message.obj = cause;
                    handler.sendMessage(message);
                    Log.e(LOG_TAG, "连接失败,原因:", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    Message msg = Message.obtain();
                    msg.what = MQ_ON_MSG_ARRIVED;
                    msg.obj = message;
                    handler.sendMessage(msg);
                    Log.i(LOG_TAG, System.currentTimeMillis() +"接收到消息,来至Topic [" + topic + "] , 内容是:["
                            + new String(message.getPayload(), "UTF-8") + "], num ");
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    //如果是qos 0消息 token.resp是没有回复的
                    Message msg = Message.obtain();
                    msg.what = MQ_ON_DELIVER_COMPLETE;
                    msg.obj = token;
                    handler.sendMessage(msg);
                    Log.i(LOG_TAG, "消息发送成功! " + ((token==null || token.getResponse()==null) ? "null":token.getResponse().getKey()));
                }
            });
            Log.i(LOG_TAG, System.currentTimeMillis() + "连接成功:---");

            iotClientInst.setMqttClient(sampleClient);
        }
        return iotClientInst;
    }


    public void subscribeSensor(TextView dataTv) throws Exception {
        this.dataTv = dataTv;
        if (this.subStatus == 1)
            return ;


        mqttClient.subscribe(subTopic);
        this.subStatus=1;
    }

    public void pubCommand(String command) {
        MqttMessage message = new MqttMessage(command.getBytes());
        message.setQos(0);
        try {
            mqttClient.publish(pubTopic, message);
        } catch (MqttException e) {
            Log.e(LOG_TAG, "推送指令异常", e);
        }
    }

    private static SSLSocketFactory createSSLSocket() throws Exception {
        //CA根证书，可以从官网下载
//        InputStream in = SimpleClient.class.getResourceAsStream("/pubkey.crt");
        InputStream in = context.getAssets().open("pubkey.crt");

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate ca = null;
        try {
            ca = cf.generateCertificate(in);
        } catch (CertificateException e) {
            e.printStackTrace();
        } finally {
            in.close();
        }
        String keyStoreType = KeyStore.getDefaultType();
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init(keyStore);
        SSLContext context = SSLContext.getInstance("TLSV1.2");
        context.init(null, tmf.getTrustManagers(), null);
        SSLSocketFactory socketFactory = context.getSocketFactory();
        return socketFactory;
    }
}
