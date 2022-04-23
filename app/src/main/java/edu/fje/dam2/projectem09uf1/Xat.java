package edu.fje.dam2.projectem09uf1;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


public class Xat extends AppCompatActivity {

    private CoordinatorLayout coordinatorLayout;
    private static KeyGenerator keygenerator = null;
    private static SecretKey desKey = null;
    private static Cipher cipher = null;

    private static KeyPair keyPair = null;
    private static PublicKey publicKey = null;

    private TextView usuari;
    private EditText msg;
    private FloatingActionButton btSend;

    private ArrayList<String> resultats = new ArrayList<String>();

    private int encrMode = 404;
    private byte[] firmaDades = null;
    private byte[] firmaFirma = null;
    byte[] wrapText = null;
    private PublicKey firmaPbKey = null;

    String clientId = MqttClient.generateClientId();                                    //CONNEXIÓ MQTT
    MqttAndroidClient client =
            new MqttAndroidClient(this, "tcp://broker.emqx.io:1883",
                    clientId);


    private final String BASE_DADES = "xatAndroid";
    private final String TAULA = "xatProjecteCripto";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String usr = intent.getStringExtra("usr");
        encrMode = intent.getIntExtra("encrMode",404);
        setContentView(R.layout.xat);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator);

        try {                                                           //GENERACIÓ DE CLAUS SIMÈTRICA I ASIMÈTRIQUES
            keygenerator = KeyGenerator.getInstance("AES");
            desKey = keygenerator.generateKey();

            cipher = Cipher.getInstance("AES");
            keyPair = randomGenerate(2048);
        }catch(Exception e) {}

        usuari = (TextView) findViewById(R.id.tvUsr);
        msg = (EditText) findViewById(R.id.etMsg);
        btSend = (FloatingActionButton) findViewById(R.id.floatingActionButton);

        usuari.setText(usr);

        loadPrevMsg();

        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {                    //SUBSCRIPCIÓ ALS TEMES
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

    public void subscribe() throws MqttException, UnsupportedEncodingException {            //subscribció als diferents temes
        String topic = "/projecteM09UF3";
        client.subscribe(topic, 0);
        byte[] pKey = keyPair.getPublic().getEncoded();
        MqttMessage message = new MqttMessage(pKey);
        client.subscribe("/projecteM09UF3/SimMSG",0);
        client.subscribe("/projecteM09UF3/SimK",0);
        client.subscribe("/projecteM09UF3/textEmb",0);
        client.subscribe("/projecteM09UF3/Firma/dades",0);
        client.subscribe("/projecteM09UF3/Firma/firma",0);
        client.subscribe("/projecteM09UF3/Firma/key",0);

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
                updateChat(topic,message.getPayload());           //gestor de missatges rebuts pels temes
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    public void sendEncrKey(View v) {                       //enviament de clau simètrica
        String topic = "/projecteM09UF3/SimK";

        byte[] pKey = desKey.getEncoded();

        byte[] encodedPayload = new byte[0];
        try {
            MqttMessage messageN = new MqttMessage(pKey);
            client.publish(topic, messageN);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void sendSign(View v){                   //generació i enviament de firma
        PrivateKey pvk = keyPair.getPrivate();
        PublicKey pbk = keyPair.getPublic();
        String msg = "Exemple FIRMA";
        byte[] message = msg.getBytes();
        byte[] signature = signData(message, pvk);

        try {
            String topic = "/projecteM09UF3/Firma/dades";
            MqttMessage mqttMessage = new MqttMessage(message);
            client.publish(topic, mqttMessage);

            topic = "/projecteM09UF3/Firma/firma";
            mqttMessage = new MqttMessage(signature);
            client.publish(topic, mqttMessage);

            topic = "/projecteM09UF3/Firma/key";
            mqttMessage = new MqttMessage(pbk.getEncoded());
            client.publish(topic, mqttMessage);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(View v){
        if(encrMode == 0){                                              //Encriptació Simètrica i enviament del missatge
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
        }else if(encrMode == 1){                                    //Encriptació Asimètrica i enviament del missatge
            if(String.valueOf(msg.getText()).length() != 0){
                Log.i("MSG ENVIAT", String.valueOf(msg.getText()));

                //encrypt message
                byte[][] encrMsg = doEmbolcall(publicKey,String.valueOf(usuari.getText()) + ": " +String.valueOf(msg.getText()));

                try {
                    MqttMessage messageWrap = new MqttMessage(encrMsg[0]);
                    client.publish("/projecteM09UF3/textEmb", messageWrap);
                    MqttMessage message = new MqttMessage(encrMsg[1]);
                    client.publish("/projecteM09UF3", message);

                } catch (MqttException e) {
                    e.printStackTrace();
                }

                resultats.add(String.valueOf(usuari.getText()) + ": " +String.valueOf(msg.getText()));          //Carrega missatge al llistat
                ArrayAdapter<String> itemsAdapter =
                        new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, resultats);

                ListView listView = (ListView) findViewById(R.id.lvXat);
                listView.setAdapter(itemsAdapter);
                listView.smoothScrollToPosition(itemsAdapter.getCount());

                msg.setText("");
            }
        }

    }

    public void updateChat(String topic, byte[] message) throws NoSuchAlgorithmException, InvalidKeySpecException, MqttException, InvalidKeyException, NoSuchPaddingException {
        if(topic.equals("/projecteM09UF3/Firma/dades")){
            firmaDades = message;
        }else if(topic.equals("/projecteM09UF3/Firma/firma")){
            firmaFirma = message;
        }else if(topic.equals("/projecteM09UF3/Firma/key")){
            firmaPbKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(message));
            if(validateSignature(firmaDades,firmaFirma,firmaPbKey)){
                Log.i("firma", "CORRECTO");
                Snackbar.make(coordinatorLayout, String.valueOf("Validació de signatura: CORRECTE"), Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }else{
                Snackbar.make(coordinatorLayout, String.valueOf("Validació de signatura: INCORRECTE"), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                Log.i("firma", "INCORRECTO");
            }
        }else if(encrMode == 0){          //Desencriptació Simètrica
            if(topic.equals("/projecteM09UF3/SimK")){
                desKey = new SecretKeySpec(message, "AES");
            }else{
                byte[] msgDecrypted = decrypt(message);

                System.out.println(msgDecrypted);
                resultats.add(new String(msgDecrypted));
                Log.i("uwud",new String(msgDecrypted));


                ArrayAdapter<String> itemsAdapter =
                        new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, resultats);

                ListView listView = (ListView) findViewById(R.id.lvXat);
                listView.setAdapter(itemsAdapter);
                listView.smoothScrollToPosition(itemsAdapter.getCount());
            }

        }else if(encrMode == 1) {    //Gestor Asimètric
            byte[] pKey = keyPair.getPublic().getEncoded();
            MqttMessage messageN = new MqttMessage(pKey);

            if(message.length != 256){      //Enviaments automàtics de la clau pública que s'utilitza per xifrar asimètricament
                if(topic.equals("/projecteM09UF3/pkAdria")){
                    if(publicKey == null){
                        Log.i("Connexio", "Tipus Key");
                        publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(message));
                        client.publish("/projecteM09UF3/pkDavid", messageN);
                    }
                }else if(topic.equals("/projecteM09UF3/pkDavid")){
                    if(publicKey == null){
                        Log.i("Connexio", "Tipus Key");
                        publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(message));
                        client.publish("/projecteM09UF3/pkAdria", messageN);
                    }
                }else if(topic.equals("/projecteM09UF3/textEmb")){
                    Log.i("Connexio", "Tipus Text embolcall");
                    wrapText = message;
                    Log.i("Connexio", String.valueOf(wrapText));
                }

            }else {         //Desencriptació asimètrica
                Log.i("Connexio", "Tipus Missatge");

                String decrMsg = null;
                Log.i("Connexio", String.valueOf(wrapText));

                if(wrapText != null) {
                    decrMsg = desEmbolcalla(keyPair.getPrivate(), message, wrapText);
                }



                if(!decrMsg.equals("ERROR")) {
                    resultats.add(decrMsg);
                    Log.i("MSG REBUT", decrMsg);
                    ArrayAdapter<String> itemsAdapter =
                            new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, resultats);

                    ListView listView = (ListView) findViewById(R.id.lvXat);
                    listView.setAdapter(itemsAdapter);
                    listView.smoothScrollToPosition(itemsAdapter.getCount());
                }
            }
        }

    }

    public void loadPrevMsg(){              //Carrega dels missatges anteriors
        SQLiteDatabase baseDades = null;
        try {
            baseDades = this.openOrCreateDatabase(BASE_DADES, MODE_PRIVATE, null);

            baseDades.execSQL("CREATE TABLE IF NOT EXISTS "
                    + TAULA
                    + " (id INT(1), pvKey BLOB, pKey BLOB, xat VARCHAR);");

            Cursor c = baseDades.rawQuery("SELECT id, pvKey, pKey, xat"
                            + " FROM " + TAULA,
                    null);

            int columnaId = c.getColumnIndex("id");

            if (c != null) {

                if (c.isBeforeFirst()) {
                    c.moveToFirst();
                    int i = 0;

                    do {
                        i++;
                        byte[] pKey = null,pvKey = null;
                        String xat = null;
                        try {
                            pvKey = c.getBlob(1);
                            pKey = c.getBlob(2);
                            xat = c.getString(3);
                        }catch(Exception e){
                        }

                        String xatOb[] = xat.split("%%%") ;
                        resultats = new ArrayList<String>(Arrays.asList(xatOb));
                        } while (c.moveToNext());
                }
            }

        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if (baseDades != null) {
                baseDades.close();
            }
        }

        //resultats.add(msgFinal);
        ArrayAdapter<String> itemsAdapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, resultats);

        ListView listView = (ListView) findViewById(R.id.lvXat);
        listView.setAdapter(itemsAdapter);
        listView.smoothScrollToPosition(itemsAdapter.getCount());
    }

    public static byte[] encrypt(String missatge) {         //Encriptació simètrica
        byte[] encryptedMessage = null;

        try {
            byte[] message = missatge.getBytes();
            cipher.init(Cipher.ENCRYPT_MODE, desKey);
            encryptedMessage = cipher.doFinal(message);
        }catch (Exception e2) { System.out.println("Exception found while CRYPTING"); }

        return encryptedMessage;
    }

    public static byte[] decrypt(byte[] encryptedMessage) {     //Desencriptat simètric
        byte[] dencryptedMessage = null;

        try {
            cipher.init(Cipher.DECRYPT_MODE, desKey);
            dencryptedMessage = cipher.doFinal(encryptedMessage);
        }catch (Exception e2) { System.out.println("Exception found while DECRYPTING"); }

        return dencryptedMessage;
    }

    public KeyPair randomGenerate(int len) {                  //Generació claus asimètriques
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

    public byte[] encryptDataAs(String missatge, PublicKey pb) {            //Encriptació asimètrica
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

    public byte[] decryptDataAs(byte[] missatge, PrivateKey prv) {          //Desencriptació asimètrica
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

    @Override
    protected void onStop() {                   //Al tancar app, guarda els missatges
        super.onStop();

        String xat = String.join("%%%", resultats);
        Log.i("SAVE", xat);

        SQLiteDatabase baseDades = null;
        try {

            baseDades = this.openOrCreateDatabase(BASE_DADES, MODE_PRIVATE, null);

            baseDades.execSQL("INSERT OR REPLACE INTO "
                    + TAULA
                    + " (id, pvKey, pKey, xat)"
                    + " VALUES (" + 1 + ", '" + "pvKey" + "', '" + "pbKey" + "', '" + xat +"');");

        } finally {
            if (baseDades != null) {
                baseDades.close();
            }
        }
    }

    public static byte[] signData(byte[] data, PrivateKey priv) {               //Firma de les dades
        byte[] signature = null;
        try {
            Signature signer = Signature.getInstance("SHA1withRSA");
            signer.initSign(priv);
            signer.update(data);
            signature = signer.sign();
        } catch (Exception ex) {
            System.err.println("Error signant les dades: " + ex);
        }
        return signature;
    }

    public static boolean validateSignature(byte[] data, byte[] signature, PublicKey pub) {         //Validació de la firma
        boolean isValid = false;
        try {
            Signature signer = Signature.getInstance("SHA1withRSA");
            signer.initVerify(pub);
            signer.update(data);
            isValid = signer.verify(signature);
        } catch (Exception ex) {
            System.err.println("Error validant les dades: " + ex);
        }
        return isValid;
    }

    public byte[][] doEmbolcall(PublicKey pub, String data){            //Embolcall
        byte[][] encWrappedData = new byte[2][];
        try {
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            kgen.init(128);
            SecretKey sKey = kgen.generateKey();
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, sKey);
            byte[] encMsg = cipher.doFinal(data.getBytes());
            cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.WRAP_MODE, pub);
            byte[] encKey = cipher.wrap(sKey);
            encWrappedData[0] = encMsg;
            encWrappedData[1] = encKey;
            Log.i("EM", "XIFRAT");
        } catch (Exception ex) {
            System.err.println("Ha succeït un error xifrant: " + ex);
        }
        return encWrappedData;
    }

    //Desembolcall
    public String desEmbolcalla(PrivateKey pvKey, byte[] wrapKey, byte[] wrapXat) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException {
        Log.i("Connexio", "Desembolcallant...");
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.UNWRAP_MODE, pvKey);

        Key unwrappedKey = cipher.unwrap(wrapKey, "AES", Cipher.SECRET_KEY);

        byte[] dencryptedMessage = null;

        try {
            cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, unwrappedKey);
            dencryptedMessage = cipher.doFinal(wrapXat);
        }catch (Exception e2) { System.out.println("Exception found while DECRYPTING"); }

        return new String(dencryptedMessage);
    }

}

