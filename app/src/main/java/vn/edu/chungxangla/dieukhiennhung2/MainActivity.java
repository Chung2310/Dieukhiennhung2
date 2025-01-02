package vn.edu.chungxangla.dieukhiennhung2;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {
    VideoView bangtruyen;
    ImageView imageView_red,imageView_blue,imageView_green;
    TextView textView_speed,textView_sum,textView_red,textView_blue,textView_green;
    ImageButton tang,giam;
    AppCompatButton onoff,reset;
    private String MQTTHOST = "ws://103.180.149.239:9001";
    private MqttAndroidClient client;
    private MqttConnectOptions options;
    private String topic = "chuoi_json";
    int red,blue,green,redl,bluel,greenl;
    int speed=3;
    int chedo=0;
    boolean isColorChanged;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        anhXa();
        playVideo();
        initcontrol();

        SharedPreferences sharedPreferences = getSharedPreferences("mypref",MODE_PRIVATE);
        redl = red = sharedPreferences.getInt("red",0);
        bluel = blue = sharedPreferences.getInt("blue",0);
        greenl = green = sharedPreferences.getInt("green",0);
        chedo = sharedPreferences.getInt("mode",0);
        speed = sharedPreferences.getInt("speed",0);
        hienThi(red,green,blue,chedo,speed);

        String clientId = MqttClient.generateClientId();
        clientId = String.valueOf(new MqttAndroidClient(this.getApplicationContext(), MQTTHOST, clientId));

        client = new MqttAndroidClient(this.getApplicationContext(), MQTTHOST, clientId);

        options = new MqttConnectOptions();

        try {
            IMqttToken token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    SUB(client, topic);
                    Toast.makeText(MainActivity.this, "CONNECTED", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(MainActivity.this, "DISCONNECTED", Toast.LENGTH_LONG).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @SuppressLint("ResourceType")
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                if (topic.equals("chuoi_json")) {
                    String mydata = message.toString();
                    Gson gson = new Gson();
                    ThongTin thongTin = gson.fromJson(mydata, ThongTin.class);
                    chedo = thongTin.getChedo();
                    speed = thongTin.getSpeed();
                    red = thongTin.getRed();
                    green = thongTin.getGreen();
                    blue = thongTin.getBlue();

                    if (chedo == 0) {
                        onoff.setText("Bật");
                        textView_speed.setText("Tốc Độ: " + speed + "m/s");
                        bangtruyen.start();
                    } else {
                        onoff.setText("Tắt");
                        textView_speed.setText("Tốc Độ: " + 0 + "m/s");
                        bangtruyen.pause();
                    }
                }
                boolean hasChanged = false;
                if (redl != red && red !=0) {
                    startConveyorAnimation(imageView_red);
                    redl = red;
                    hasChanged = true;
                }
                if (greenl != green && green!=0) {
                    startConveyorAnimation(imageView_green);
                    greenl = green;
                    hasChanged = true;
                }
                if (bluel != blue && blue!=0) {
                    startConveyorAnimation(imageView_blue);
                    bluel = blue;
                    hasChanged = true;
                }

                if (hasChanged) {
                    //luu csdl
                }

                hienThi(red, green, blue, chedo, speed);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }
    private void initcontrol() {
        giam.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ResourceType")
            @Override
            public void onClick(View v) {
                String message = "";
                if (speed > 0) {
                    speed--;
                    String speed_st = String.valueOf(speed);
                    textView_speed.setText("Tốc độ: " + speed_st + "m/s");
                    if (speed == 0) {
                        Toast.makeText(MainActivity.this, "Hệ thống tắt", Toast.LENGTH_LONG).show();
                        isColorChanged = true;
                        onoff.setText("Tắt");
                        bangtruyen.pause();
                        chedo = 1;
                    }
                    ThongTin thongTin = new ThongTin();
                    thongTin.setChedo(chedo);
                    thongTin.setSpeed(speed);
                    thongTin.setRed(red);
                    thongTin.setBlue(blue);
                    thongTin.setGreen(green);
                    guiMQTT(thongTin);
                } else {
                    Toast.makeText(MainActivity.this, "Hệ thống tắt", Toast.LENGTH_LONG).show();
                }
            }
        });
        tang.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ResourceType")
            @Override
            public void onClick(View v) {
                String message = "";
                if(speed < 5) {
                    if (speed == 0) {
                        Toast.makeText(MainActivity.this, "Hệ thống bật", Toast.LENGTH_LONG).show();
                        isColorChanged = false;
                        onoff.setText("Bật");
                        chedo = 0;
                    }
                    speed++;
                    String speed_st = String.valueOf(speed);
                    textView_speed.setText("Tốc độ: " + speed_st + "m/s");
                    ThongTin thongTin = new ThongTin();
                    thongTin.setChedo(chedo);
                    thongTin.setSpeed(speed);
                    thongTin.setRed(red);
                    thongTin.setBlue(blue);
                    thongTin.setGreen(green);
                    guiMQTT(thongTin);
                }
                else {
                    Toast.makeText(MainActivity.this,"Tốc độ đã max !",Toast.LENGTH_LONG).show();
                }
            }
        });
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speed = 3;
                red = redl = 0;
                blue = bluel = 0;
                green = greenl = 0;
                chedo = 0;
                textView_red.setText("0");
                textView_blue.setText("0");
                textView_green.setText("0");
                textView_speed.setText("Tốc độ: " + speed + "m/s");
                textView_sum.setText("0");
                ThongTin thongTin = new ThongTin();
                thongTin.setChedo(chedo);
                thongTin.setSpeed(speed);
                thongTin.setRed(red);
                thongTin.setBlue(blue);
                thongTin.setGreen(green);
                guiMQTT(thongTin);
                SharedPreferences sharedPreferences = getSharedPreferences("mypref",MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("red",0);
                editor.putInt("blue",0);
                editor.putInt("green",0);
                editor.putInt("mode",0);
                editor.putInt("speed",3);
                editor.commit();
            }
        });
    }

    protected void onPause(){
        super.onPause();
        SharedPreferences sharedPreferences = getSharedPreferences("mypref",MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("red",red);
        editor.putInt("blue",blue);
        editor.putInt("green",green);
        editor.putInt("mode",chedo);
        editor.putInt("speed",speed);
        editor.commit();
    }

    @SuppressLint("ResourceType")
    private void hienThi(int red1, int green1, int blue1, int mode1, int speed1){
        textView_red.setText(String.valueOf(red1));
        textView_green.setText(String.valueOf(green1));
        textView_blue.setText(String.valueOf(blue1));
        textView_speed.setText(String.valueOf("Tốc độ: " + speed1 + "m/s"));
        textView_sum.setText(String.valueOf(red1+blue1+green1));
        if(mode1 == 0)
        {
            isColorChanged = false;
            onoff.setText("Tắt");
            playVideo();
        }
        else {
            onoff.setText("Bật");
            isColorChanged = true;
            bangtruyen.pause();
        }
    }
    private void startConveyorAnimation(ImageView imageView) {
        imageView.setVisibility(View.VISIBLE);
        int parentWidth = ((View)imageView.getParent()).getWidth(); // Lấy chiều rộng của phần tử cha
        int imageViewWidth = imageView.getWidth(); // Lấy chiều rộng của ImageView

        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0f,
                Animation.RELATIVE_TO_PARENT, 1f,
                Animation.ABSOLUTE, 0f,
                Animation.ABSOLUTE, 0f);

        animation.setDuration(3000);
        animation.setFillAfter(true);
        imageView.startAnimation(animation);
    }
    private void SUB(MqttAndroidClient client, String topic){
        int qos = 1;
        try{
            IMqttToken subtoken = client.subscribe(topic,qos);
            subtoken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                }
            });
        }
        catch (MqttException e){
            e.printStackTrace();
        }
    }

    private void guiMQTT(ThongTin thongTin){
        Gson gson = new Gson();
        String message = gson.toJson(thongTin);
        String topic = "chuoi_json";
        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = message.getBytes("UTF-8");
            MqttMessage message1 = new MqttMessage(encodedPayload);
            client.publish(topic,message1);
        }
        catch (UnsupportedEncodingException | MqttException e){
            e.printStackTrace();
        }
    }
    private void playVideo() {
        String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.bang_truyen33s;
        Uri uri = Uri.parse(videoPath);

        bangtruyen.setVideoURI(uri);

        // Phát video tự động
        bangtruyen.start();

        // Lặp lại video khi kết thúc
        bangtruyen.setOnCompletionListener(mp -> bangtruyen.start());
    }

    private void anhXa() {
        bangtruyen = findViewById(R.id.bangtruyen);
        imageView_red = findViewById(R.id.tomato_red);
        imageView_green = findViewById(R.id.tomato_green);
        imageView_blue = findViewById(R.id.tomato_blue);
        textView_red = findViewById(R.id.red);
        textView_blue = findViewById(R.id.blue);
        textView_green = findViewById(R.id.green);
        textView_sum = findViewById(R.id.sum);
        textView_speed = findViewById(R.id.speed);
        tang = findViewById(R.id.tang);
        giam = findViewById(R.id.giam);
        onoff = findViewById(R.id.onoff);
        reset = findViewById(R.id.reset);

    }
}