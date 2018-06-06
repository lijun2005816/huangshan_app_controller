package se.i_gh.smartgreencontroller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.BottomNavigationView.OnNavigationItemSelectedListener;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.alivc.player.AliVcMediaPlayer;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    public static final String URL = "http://sglive.dejon.cn/smartgreen/1.m3u8";
    final static String LOG_TAG = "MainActivity";
    private View includeShow, includeReceive, includeDashboard, includeSend, includeVideo;
    private TextView tv_h, tv_a, tv_o, tv_wea;
    private Timer timer = new Timer();
    private TimerTask task = new TimerTask() {
        @Override
        public void run() {
            sendCmd("valve0");
        }
    };
    private boolean isTimerStart = false;
    private long maxPumpTime = 30*60*1000;
    BroadcastReceiver broadcastReceiverSh = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String msgStr = Objects.requireNonNull(intent.getExtras()).getString("data");
            Log.i(LOG_TAG, msgStr);
            try {
                JSONObject json = new JSONObject(msgStr);
                if (json.has("hao")) {
                    String hao = json.get("hao").toString();
                    int indexH = hao.indexOf("H");
                    int indexA = hao.indexOf("A");
                    int indexO = hao.indexOf("O");
                    String humi_1 = Integer.toString(Integer.parseInt(hao.substring(indexH + 1, indexH + 3)));
                    if (humi_1.equals("99")) {
                        humi_1 = "--";
                    }
                    String humi_2 = Integer.toString(Integer.parseInt(hao.substring(indexH + 3, indexA)));
                    if (humi_2.equals("99")) {
                        humi_2 = "--";
                    }
                    String air_temp = Integer.toString(Integer.parseInt(hao.substring(indexA + 1, indexA + 3)));
                    if (air_temp.equals("99")) {
                        air_temp = "--";
                    }
                    String air_humi = Integer.toString(Integer.parseInt(hao.substring(indexA + 3, indexO)));
                    if (air_humi.equals("99")) {
                        air_humi = "--";
                    }
                    tv_h.setText(String.format("位置[1] %s%% 位置[2] %s%%", humi_1, humi_2));
                    tv_a.setText(String.format("温度 %s℃     湿度 %s%%", air_temp, air_humi));
                    String click = hao.substring(indexO + 1, indexO + 5) + "/" + hao.substring(indexO + 5, indexO + 7) + "/" + hao.substring(indexO + 7, indexO + 9) + " " + hao.substring(indexO + 9, indexO + 11) + ":" + hao.substring(indexO + 11, indexO + 13);
                    String status = (hao.substring(indexO + 13).equalsIgnoreCase("close")) ? "关闭" : "开启";
                    tv_o.setText(String.format("%s %s", click, status));
                    if (status.equals("开启")) {
                        if (! isTimerStart) {
                            isTimerStart = true;
                            timer.schedule(task, maxPumpTime);
                        }
                    } else {
                        if (isTimerStart) {
                            isTimerStart = false;
                            timer.cancel();
                        }
                    }
                    if (click.equals("2018/01/01 22:22")) {
                        String error = "暂无操作记录";
                        tv_o.setText(String.format("%s", error));
                    } else if (click.equals("2018/04/25 12:15")) {
                        String error = "暂未与设备连接";
                        tv_o.setText(String.format("%s", error));
                    }
                } else if (json.has("wea")) {
                    tv_wea.setText(String.format("%s", json.get("wea").toString().substring(15)));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };
    private TextView tv_rcvlog;
    BroadcastReceiver broadcastReceiverHz = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String msgStr = Objects.requireNonNull(intent.getExtras()).getString("data");
            tv_rcvlog.append("\n");
            tv_rcvlog.append(msgStr);
        }
    };
    private TextView tv_temp, tv_humi;
    private EditText et_cmdinput, et_maxhold, et_minhold;
    private SurfaceView mSurfaceViewOne, mSurfaceViewTwo, mSurfaceViewThree;
    private OnNavigationItemSelectedListener mOnNavigationItemSelectedListener = new OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_show:
                    includeShow.setVisibility(View.VISIBLE);
                    includeReceive.setVisibility(View.GONE);
                    includeDashboard.setVisibility(View.GONE);
                    includeSend.setVisibility(View.GONE);
                    includeVideo.setVisibility(View.GONE);
                    return true;
/*                case R.id.navigation_receive:
                    includeShow.setVisibility(View.GONE);
                    includeReceive.setVisibility(View.VISIBLE);
                    includeDashboard.setVisibility(View.GONE);
                    includeSend.setVisibility(View.GONE);
                    includeVideo.setVisibility(View.GONE);
                    return true;
                case R.id.navigation_dashboard:
                    includeShow.setVisibility(View.GONE);
                    includeReceive.setVisibility(View.GONE);
                    includeDashboard.setVisibility(View.VISIBLE);
                    includeSend.setVisibility(View.GONE);
                    includeVideo.setVisibility(View.GONE);
                    return true;*/
                case R.id.navigation_send:
                    includeShow.setVisibility(View.GONE);
                    includeReceive.setVisibility(View.GONE);
                    includeDashboard.setVisibility(View.GONE);
                    includeSend.setVisibility(View.VISIBLE);
                    includeVideo.setVisibility(View.GONE);
                    return true;
/*                case R.id.navigation_video:
                    includeShow.setVisibility(View.GONE);
                    includeReceive.setVisibility(View.GONE);
                    includeDashboard.setVisibility(View.GONE);
                    includeSend.setVisibility(View.GONE);
                    includeVideo.setVisibility(View.VISIBLE);
                    return true;*/
            }
            return false;
        }
    };
    private OnClickListener listenerClear = new OnClickListener() {
        @Override
        public void onClick(View v) {
            tv_rcvlog.setText(getResources().getText(R.string.tv_receive));
            tv_rcvlog.append("\n");
        }
    };
    private OnClickListener listenerSend = new OnClickListener() {
        @Override
        public void onClick(View v) {
            String jsonStr = "{\"devname\":\"sg_sensor\",\"content\":{\"CH2\":130.37,\"CH1\":-50,\"CH3\":200.31,\"CH4\":50.10,\"CH5\":60.31,\"CH6\":70.31},\"timestamp\":\"2017-05-06 00:50:48.989\"}";
/*
            Intent hzIotClient = new Intent(MainActivity.this, HzIotClient.class);
            hzIotClient.putExtra("command", jsonStr);
            startService(hzIotClient);
*/
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.CHINA);
                String datetime = sdf.format(new Date());
                JSONObject dataJson = new JSONObject(jsonStr);
                JSONObject contentJson1 = dataJson.optJSONObject("content");
                Double ch1 = contentJson1.optDouble("CH1");
                Double ch2 = contentJson1.optDouble("CH2");
                Double ch3 = contentJson1.optDouble("CH3");
                Double ch4 = contentJson1.optDouble("CH4");
                Double ch5 = contentJson1.optDouble("CH5");
                Double ch6 = contentJson1.optDouble("CH6");
                tv_rcvlog.append("\n");
                tv_rcvlog.append(String.format("%s CH1: %sC / CH2: %sC / CH3: %sC / CH4: %sRH%% / CH5: %sRH%% / CH6: %sRH%%", datetime, ch1, ch2, ch3, ch4, ch5, ch6));
                tv_temp.setText(String.format("温度\nCH1:%s\nCH2:%s\nCH3:%s", ch1, ch2, ch3));
                tv_humi.setText(String.format("湿度\nCH1:%s\nCH2:%s\nCH3:%s", ch4, ch5, ch6));
            } catch (JSONException e) {
                Log.e(LOG_TAG, "json format exception", e);
            } catch (Exception e) {
                Log.e(LOG_TAG, "data parse exception:" + jsonStr, e);
            }
        }
    };
    private OnClickListener listenerCmdClear = new OnClickListener() {
        @Override
        public void onClick(View v) {
            et_cmdinput.setText("");
        }
    };
    private OnClickListener listenerCmdSend = new OnClickListener() {
        @Override
        public void onClick(View v) {
            String commandStr = et_cmdinput.getText().toString();
            sendCmd(commandStr);
            et_cmdinput.setText("");
        }
    };
    private OnClickListener listenerCmdVideopen = new OnClickListener() {
        @Override
        public void onClick(View v) {
            sendCmd("rtmp1");
        }
    };
    private OnClickListener listenerCmdVideoClose = new OnClickListener() {
        @Override
        public void onClick(View v) {
            sendCmd("rtmp0");
        }
    };
    private OnClickListener listenerCmdDataopen = new OnClickListener() {
        @Override
        public void onClick(View v) {
            sendCmd("senr1");
        }
    };
    private OnClickListener listenerCmdDataclose = new OnClickListener() {
        @Override
        public void onClick(View v) {
            sendCmd("senr0");
        }
    };
    private OnClickListener listenerCmdMaxhold = new OnClickListener() {
        @Override
        public void onClick(View v) {
            String maxhold = Integer.toString(Integer.parseInt(et_maxhold.getText().toString()));
            sendCmd(String.format("maxhold%s", maxhold));
            et_maxhold.setText("");
        }
    };
    private OnClickListener listenerCmdMinhold = new OnClickListener() {
        @Override
        public void onClick(View v) {
            String minhold = Integer.toString(Integer.parseInt(et_minhold.getText().toString()));
            sendCmd(String.format("minhold%s", minhold));
            et_minhold.setText("");
        }
    };
    private OnClickListener listenerCmdRemotetest = new OnClickListener() {
        @Override
        public void onClick(View v) {
            sendCmd("spkr6");
        }
    };
    private AdapterView.OnItemSelectedListener listenerValveMaxSelected = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String sInfo=parent.getItemAtPosition(position).toString();
            int indexStart = sInfo.indexOf("-");
            int indexEnd = sInfo.lastIndexOf("-");
            String num = sInfo.substring(indexStart + 2, indexEnd-1);
            long minute = Long.valueOf(num);
            SharedPreferences setting = getSharedPreferences("setting", 0);
            SharedPreferences.Editor editor = setting.edit();
            editor.putLong("valvemax", minute*60*1000).apply();
            Toast.makeText(MainActivity.this, String.format("max valve open time is saved : %s minutes", minute), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //set navigation bar
        includeShow = findViewById(R.id.include_content_show);
        includeReceive = findViewById(R.id.include_content_receive);
        includeDashboard = findViewById(R.id.include_content_dashboard);
        includeSend = findViewById(R.id.include_content_send);
        includeVideo = findViewById(R.id.include_content_video);
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        //iot connect
        Intent hzIotClient = new Intent(this, HzIotClient.class);
        hzIotClient.putExtra("command", "");
        startService(hzIotClient);
        Intent shIotClient = new Intent(this, ShIotClient.class);
        shIotClient.putExtra("command", "");
        startService(shIotClient);
        Intent hsIotClient = new Intent(this, HsIotClient.class);
        hsIotClient.putExtra("command", "");
        startService(hsIotClient);
        //update ui from broadcast
        IntentFilter filterHz = new IntentFilter(HzIotClient.action);
        registerReceiver(broadcastReceiverHz, filterHz);
        IntentFilter filterSh = new IntentFilter(ShIotClient.action);
        registerReceiver(broadcastReceiverSh, filterSh);
        //set show page
        tv_h = findViewById(R.id.tv_h);
        tv_a = findViewById(R.id.tv_a);
        tv_o = findViewById(R.id.tv_o);
        tv_wea = findViewById(R.id.tv_wea);
        //set receive page
        tv_rcvlog = findViewById(R.id.tv_rcvlog);
        Button btn_clear = findViewById(R.id.btn_clear);
        btn_clear.setOnClickListener(listenerClear);
        //set dashboard page
        Button btn_random = findViewById(R.id.btn_random);
        btn_random.setOnClickListener(listenerSend);
        tv_temp = findViewById(R.id.tv_temp);
        tv_temp.setText("温度\n");
        tv_humi = findViewById(R.id.tv_humi);
        tv_humi.setText("湿度\n");
        //set send page
        et_cmdinput = findViewById(R.id.et_cmd_input);
        et_maxhold = findViewById(R.id.et_cmd_maxhold);
        et_minhold = findViewById(R.id.et_cmd_minhold);
        Button btn_cmd_clear = findViewById(R.id.btn_cmd_clear);
        btn_cmd_clear.setOnClickListener(listenerCmdClear);
        Button btn_cmd_send = findViewById(R.id.btn_cmd_send);
        btn_cmd_send.setOnClickListener(listenerCmdSend);
        Button btn_cmd_videopen = findViewById(R.id.btn_cmd_videopen);
        btn_cmd_videopen.setOnClickListener(listenerCmdVideopen);
        Button btn_cmd_videoclose = findViewById(R.id.btn_cmd_videoclose);
        btn_cmd_videoclose.setOnClickListener(listenerCmdVideoClose);
        Button btn_cmd_dataopen = findViewById(R.id.btn_cmd_dataopen);
        btn_cmd_dataopen.setOnClickListener(listenerCmdDataopen);
        Button btn_cmd_dataclose = findViewById(R.id.btn_cmd_dataclose);
        btn_cmd_dataclose.setOnClickListener(listenerCmdDataclose);
        Button btn_cmd_maxhold = findViewById(R.id.btn_cmd_maxhold);
        btn_cmd_maxhold.setOnClickListener(listenerCmdMaxhold);
        Button btn_cmd_minhold = findViewById(R.id.btn_cmd_minhold);
        btn_cmd_minhold.setOnClickListener(listenerCmdMinhold);
        Button btn_cmd_remotetest = findViewById(R.id.btn_cmd_remotetest);
        btn_cmd_remotetest.setOnClickListener(listenerCmdRemotetest);
        Spinner spnValveMax = findViewById(R.id.spn_valve_max);
        spnValveMax.setOnItemSelectedListener(listenerValveMaxSelected);
        SharedPreferences setting = getSharedPreferences("setting", 0);
        maxPumpTime = setting.getLong("valvemax", 30*60*1000);
        spnValveMax.setSelection((int)(maxPumpTime/1000/60/5-1));
        Log.i(LOG_TAG, String.format("valvemax:%s", maxPumpTime));
        //set video page
        mSurfaceViewOne = findViewById(R.id.PlayerOne);
        mSurfaceViewTwo = findViewById(R.id.PlayerTwo);
        mSurfaceViewThree = findViewById(R.id.PlayerThree);
        mSurfaceViewOne.setZOrderOnTop(true);
        mSurfaceViewTwo.setZOrderOnTop(true);
        mSurfaceViewThree.setZOrderOnTop(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
/*        mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            public void run() {
                startPlayerOne();
            }
        }, 1000);
        mHandler.postDelayed(new Runnable() {
            public void run() {
                startPlayerTwo();
            }
        }, 5000);
        mHandler.postDelayed(new Runnable() {
            public void run() {
                startPlayerThree();
            }
        }, 10000);*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiverHz);
    }

    public void sendCmd(String commandStr) {
        if (!TextUtils.isEmpty(commandStr)) {
            JSONObject commandJson = new JSONObject();
            try {
                commandJson.put("cmd", commandStr);
                String cmdStr = commandJson.toString();
                Log.i(LOG_TAG, "send json：" + cmdStr);
                Intent hzIotClient = new Intent(this, HzIotClient.class);
                hzIotClient.putExtra("command", cmdStr);
                startService(hzIotClient);
            } catch (Exception e) {
                Log.e(LOG_TAG, "send exception", e);
            }
        }
    }

    private void startPlayerOne() {
        AliVcMediaPlayer mPlayerOne = new AliVcMediaPlayer(getApplicationContext(), mSurfaceViewOne);
        mPlayerOne.prepareAndPlay(URL);
    }

    private void startPlayerTwo() {
        AliVcMediaPlayer mPlayerTwo = new AliVcMediaPlayer(getApplicationContext(), mSurfaceViewTwo);
        mPlayerTwo.prepareAndPlay(URL);
    }

    private void startPlayerThree() {
        AliVcMediaPlayer mPlayerThree = new AliVcMediaPlayer(getApplicationContext(), mSurfaceViewThree);
        mPlayerThree.prepareAndPlay(URL);
    }
}
