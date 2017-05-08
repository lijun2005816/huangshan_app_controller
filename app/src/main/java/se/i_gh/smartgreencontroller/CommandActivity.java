package se.i_gh.smartgreencontroller;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONObject;

public class CommandActivity extends AppCompatActivity {

    final static String LOG_TAG = "sg_controller";

    EditText etCommand;
    Button btClear;
    Button btPub;
    Button btOpenVideo;
    Button btCloseVideo;
    Button btOpenData;
    Button btCloseData;
    Button btRemoteTest;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_command);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        this.etCommand = (EditText) this.findViewById(R.id.et_command);

        this.btClear = (Button)this.findViewById(R.id.bt_clear);
        this.btClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CommandActivity.this.etCommand.setText("");
            }
        });

        this.btPub = (Button)this.findViewById(R.id.bt_pub);
        this.btPub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String commandStr = CommandActivity.this.etCommand.getText().toString();
                sendCmd(commandStr);
                CommandActivity.this.etCommand.setText("");
            }
        });

        this.btOpenVideo = (Button)this.findViewById(R.id.bt_open_video);
        this.btOpenVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCmd("rtmp1");
            }
        });

        this.btCloseVideo = (Button)this.findViewById(R.id.bt_close_video);
        this.btCloseVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCmd("rtmp0");
            }
        });

        this.btOpenData = (Button)this.findViewById(R.id.bt_open_data);
        this.btOpenData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCmd("senr1");
            }
        });

        this.btCloseData = (Button)this.findViewById(R.id.bt_close_data);
        this.btCloseData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCmd("senr0");
            }
        });

        this.btRemoteTest = (Button)this.findViewById(R.id.bt_remote_test);
        this.btRemoteTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCmd("spkr6");
            }
        });

    }

    public void sendCmd(String commandStr) {
        if (!TextUtils.isEmpty(commandStr)) {
            JSONObject commandJson = new JSONObject();
            try {
                commandJson.put("cmd", commandStr);
                String cmdStr = commandJson.toString();
                Log.i(LOG_TAG, "推送json内容："+cmdStr);
                IotClient.getSubInstance(CommandActivity.this).pubCommand(cmdStr);
//                CommandActivity.this.etCommand.setText("");
            } catch (Exception e) {
                Log.e(LOG_TAG, "推送指令异常", e);
            }
        }
    }

}
