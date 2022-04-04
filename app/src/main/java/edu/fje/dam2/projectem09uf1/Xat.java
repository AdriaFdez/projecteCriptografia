package edu.fje.dam2.projectem09uf1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;


public class Xat extends AppCompatActivity {

    private TextView usuari;
    private EditText msg;
    private FloatingActionButton btSend;

    private ArrayList<String> resultats;
    private ListView lvRanking;

    String clientId = MqttClient.generateClientId();
    MqttAndroidClient client =
            new MqttAndroidClient(this, "tcp://broker.emqx.io:1883",
                    clientId);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String usr = intent.getStringExtra("usr");
        setContentView(R.layout.xat);

        usuari = (TextView) findViewById(R.id.tvUsr);
        msg = (EditText) findViewById(R.id.etMsg);
        btSend = (FloatingActionButton) findViewById(R.id.floatingActionButton);

        usuari.setText(usr);

        resultats = new ArrayList<String>();

        resultats.add("Que tal?");
        resultats.add("Bé");

        loadPrevMsg();

        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i("Connexió", "onSuccess");
                    try {
                        subscribe();
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i("Connexió", "F");
                }

            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(View v){
        String content = String.valueOf(usuari.getText()) + ": " + String.valueOf(msg.getText());
        String topic = "/projecteM09UF3";
        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = content.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(topic, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
        msg.setText("");
    }

    public void updateChat(String msg) {
        resultats.add(msg);

        ArrayAdapter<String> itemsAdapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, resultats);

        ListView listView = (ListView) findViewById(R.id.lvXat);
        listView.setAdapter(itemsAdapter);
        listView.smoothScrollToPosition(itemsAdapter.getCount());
    }

    public void loadPrevMsg(){
        //RECOGER DE SQLITE EL LISTADO DE MENSAJES
        //ARREGLAR EL BLOQUE DE MENSAJES EN MENSAJES SEPARADOS
        //AÑADIR LOS MENSAJES EN EL ARRAY resultats

        ArrayAdapter<String> itemsAdapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, resultats);

        ListView listView = (ListView) findViewById(R.id.lvXat);
        listView.setAdapter(itemsAdapter);
        listView.smoothScrollToPosition(itemsAdapter.getCount());
    }

    public void subscribe() throws MqttException {
        String topic = "/projecteM09UF3";
        client.subscribe(topic, 0);
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                updateChat(new String(message.getPayload()));
                Log.i("Connexió rebut", new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

}

