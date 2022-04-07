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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;


public class Xat extends AppCompatActivity {

    private static KeyGenerator keygenerator = null;
    private static SecretKey desKey = null;
    private static Cipher cipher = null;

    private static KeyPair keyPair = null;
    private static PublicKey publicKey = null;

    private TextView usuari;
    private EditText msg;
    private FloatingActionButton btSend;

    private ArrayList<String> resultats;
    private int encrMode = 404;

    String clientId = MqttClient.generateClientId();
    MqttAndroidClient client =
            new MqttAndroidClient(this, "tcp://broker.emqx.io:1883",
                    clientId);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String usr = intent.getStringExtra("usr");
        encrMode = intent.getIntExtra("encrMode",404);
        setContentView(R.layout.xat);

        try {
            keygenerator = KeyGenerator.getInstance("DES");
            desKey = keygenerator.generateKey();

            cipher = Cipher.getInstance("DES");
            keyPair = randomGenerate(2048);
        }catch(Exception e) {}

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
                    } catch (MqttException | UnsupportedEncodingException e) {
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
        if(encrMode == 0){          //Encriptació Simètrica
            if(String.valueOf(msg.getText()).length() != 0){
                Log.i("uwu", String.valueOf(msg.getText()));

                //encrypt message
                String messageToEncrypt = String.valueOf(usuari.getText()) + ": " + String.valueOf(msg.getText());
                byte[] encrMsg = encrypt(messageToEncrypt);
                Log.i("uwue", new String(encrMsg));



                String topic = "/projecteM09UF3/SimMSG";

                byte[] encodedPayload = new byte[0];
                try {
                    MqttMessage message = new MqttMessage(encrMsg);
                    client.publish(topic, message);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                msg.setText("");
            }
        }else if(encrMode == 1){    //Encriptació Asimètrica
            if(String.valueOf(msg.getText()).length() != 0){
                Log.i("MSG ENVIAT", String.valueOf(msg.getText()));

                //encrypt message
                //byte[] encrMsg = encryptA(String.valueOf(msg.getText()));
                byte[] encrMsg = encryptDataAs(String.valueOf(usuari.getText()) + ": " +String.valueOf(msg.getText()), publicKey);

                String topic = "/projecteM09UF3";
                try {
                    MqttMessage message = new MqttMessage(encrMsg);
                    client.publish(topic, message);

                } catch (MqttException e) {
                    e.printStackTrace();
                }

                resultats.add(String.valueOf(usuari.getText()) + ": " +String.valueOf(msg.getText()));
                ArrayAdapter<String> itemsAdapter =
                        new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, resultats);

                ListView listView = (ListView) findViewById(R.id.lvXat);
                listView.setAdapter(itemsAdapter);
                listView.smoothScrollToPosition(itemsAdapter.getCount());

                msg.setText("");
            }
        }

    }

    public void updateChat(String topic, byte[] message) throws NoSuchAlgorithmException, InvalidKeySpecException, MqttException {
        if(encrMode == 0){          //Encriptació Simètrica
            byte[] msgDecrypted = decrypt(message);

            System.out.println(msgDecrypted);
            resultats.add(new String(msgDecrypted));
            Log.i("uwud",new String(msgDecrypted));


            ArrayAdapter<String> itemsAdapter =
                    new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, resultats);

            ListView listView = (ListView) findViewById(R.id.lvXat);
            listView.setAdapter(itemsAdapter);
            listView.smoothScrollToPosition(itemsAdapter.getCount());
        }else if(encrMode == 1) {    //Encriptació Asimètrica
            byte[] pKey = keyPair.getPublic().getEncoded();
            MqttMessage messageN = new MqttMessage(pKey);

            if(message.length != 256){
                if(topic.equals("/projecteM09UF3/pkAdria")){
                    if(publicKey == null){
                        Log.i("Connexio", "Tipus Key");
                        publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(message));
                    }else{
                        client.publish("/projecteM09UF3/pkDavid", messageN);

                    }
                }else if(topic.equals("/projecteM09UF3/pkDavid")){
                    if(publicKey == null){
                        Log.i("Connexio", "Tipus Key");
                        publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(message));
                    }else {
                        client.publish("/projecteM09UF3/pkAdria", messageN);
                    }
                }

            }else {
                Log.i("Connexio", "Tipus Missatge");

                byte[] decrMsg = decryptDataAs(message, keyPair.getPrivate());

                String msgFinal = new String(decrMsg);

                if(!msgFinal.equals("ERROR")) {
                    resultats.add(msgFinal);
                    Log.i("MSG REBUT", msgFinal);
                    ArrayAdapter<String> itemsAdapter =
                            new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, resultats);

                    ListView listView = (ListView) findViewById(R.id.lvXat);
                    listView.setAdapter(itemsAdapter);
                    listView.smoothScrollToPosition(itemsAdapter.getCount());
                }
            }
        }

    }

    public void loadPrevMsg(){
        //RECOGER DE SQLITE EL LISTADO DE MENSAJES
        //ARREGLAR EL BLOQUE DE MENSAJES EN MENSAJES SEPARADOS
        //AÑADIR LOS MENSAJES EN EL ARRAY resultats

        //resultats.add(msgFinal);
        ArrayAdapter<String> itemsAdapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, resultats);

        ListView listView = (ListView) findViewById(R.id.lvXat);
        listView.setAdapter(itemsAdapter);
        listView.smoothScrollToPosition(itemsAdapter.getCount());
    }

    public void subscribe() throws MqttException, UnsupportedEncodingException {
        String topic = "/projecteM09UF3";
        client.subscribe(topic, 0);
        byte[] pKey = keyPair.getPublic().getEncoded();
        MqttMessage message = new MqttMessage(pKey);
        client.subscribe("/projecteM09UF3/SimMSG",0);
        client.subscribe("/projecteM09UF3/SimK",0);

        if(usuari.getText().equals("David")){
            client.subscribe("/projecteM09UF3/pkAdria",0);
            client.publish("/projecteM09UF3/pkDavid", message);
        }else{
            client.subscribe("/projecteM09UF3/pkDavid", 0);
            client.publish("/projecteM09UF3/pkAdria", message);
        }
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                updateChat(topic,message.getPayload());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }


    public static byte[] encrypt(String missatge) {
        byte[] encryptedMessage = null;

        try {
            byte[] message = missatge.getBytes();
            cipher.init(Cipher.ENCRYPT_MODE, desKey);
            encryptedMessage = cipher.doFinal(message);
        }catch (Exception e2) { System.out.println("Exception found while CRYPTING"); }

        return encryptedMessage;
    }

    public static byte[] decrypt(byte[] encryptedMessage) {
        byte[] dencryptedMessage = null;

        try {
            cipher.init(Cipher.DECRYPT_MODE, desKey);
            dencryptedMessage = cipher.doFinal(encryptedMessage);
        }catch (Exception e2) { System.out.println("Exception found while DECRYPTING"); }

        return dencryptedMessage;
    }

    public KeyPair randomGenerate(int len) {
        KeyPair keys = null;
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(len);
            keys = keyGen.genKeyPair();
        } catch (Exception ex) {
            System.err.println("Generador no disponible.");
        }
        return keys;
    }

    public byte[] encryptDataAs(String missatge, PublicKey pb) {
        byte[] encryptedData = new byte[0];
        try {
            byte[] msg = missatge.getBytes();
            Cipher ciphera = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            ciphera.init(Cipher.ENCRYPT_MODE, pb);
            encryptedData = ciphera.doFinal(msg);
            Log.i("DEC", String.valueOf(encryptedData.length));
        } catch (Exception ex) {
            System.err.println("Error xifrant: " + ex);
        }
        return encryptedData;
    }

    public byte[] decryptDataAs(byte[] missatge, PrivateKey prv) {
        boolean er = false;
        byte[] encryptedData = new byte[0];
        try {
            Log.i("DEC", String.valueOf(missatge.length));
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, prv);
            encryptedData = cipher.doFinal(missatge);
        } catch (Exception ex) {
            er = true;
            System.err.println("Error desxifrant: " + ex);
        }
        if(er){
            encryptedData = "ERROR".getBytes();
        }
        return encryptedData;
    }


}

