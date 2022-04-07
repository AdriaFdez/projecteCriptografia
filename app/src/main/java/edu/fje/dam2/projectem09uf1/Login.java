package edu.fje.dam2.projectem09uf1;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;

import com.google.android.material.snackbar.Snackbar;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Login extends AppCompatActivity {

    private EditText etPwd, etUsr;
    private Map<String,String> users = new HashMap<>();
    private Switch switchEncrypt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        users.put("Adria", "900150983cd24fb0d6963f7d28e17f72");//abc
        users.put("David", "4ed9407630eb1000c0f6b63842defa7d");//def

        etUsr = (EditText) findViewById(R.id.etUsr);
        etPwd = (EditText) findViewById(R.id.etPwd);
        switchEncrypt = (Switch) findViewById(R.id.switchEncrypt);
    }

    public void login(View v) {
        boolean validUsr = false;
        String usr = String.valueOf(etUsr.getText());
        String pwd = String.valueOf(etPwd.getText());


        pwd = md5Encryption(pwd,192);

        if (usr.length() == 0 || pwd.length() == 0) {
            Snackbar.make(v, "Introdueix un nom d'usuari o contrassenya", Snackbar.LENGTH_LONG).setAction("Action", null).show();
        }

        String hash = "Metode que retorna el hash del user + pwd";

        for (Map.Entry<String, String> user : users.entrySet()) {
            if (user.getKey().equals(usr)) {
                if (user.getValue().equals(pwd)) {  //en vez de pwd tendria q ser hash
                    validUsr = true;
                }
            }
        }
        int encrMode = 404;
        if(switchEncrypt.isChecked()) encrMode = 1;
        else encrMode = 0;


        if(validUsr){
            Intent intent = new Intent(this, Xat.class);
            intent.putExtra("usr", usr);
            intent.putExtra("encrMode", encrMode);
            startActivity(intent);
        }else{
            Snackbar.make(v, "Login incorrecte", Snackbar.LENGTH_LONG).setAction("Action", null).show();
        }
    }

    public static String md5Encryption(String text, int keySize) {
        SecretKey sKey = null;
        String hashtext = "";
        if ((keySize == 128)||(keySize == 192)||(keySize == 256)) {
            try {
                byte[] data = text.getBytes(StandardCharsets.UTF_8);
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] hash = md.digest(data);
                BigInteger bigInt = new BigInteger(1,hash);
                hashtext = bigInt.toString(16);
                while(hashtext.length() < 32 ){
                    hashtext = "0"+hashtext;
                }

            } catch (Exception ex) {
                System.err.println("Error encriptant la contrassenya:" + ex);
            }
        }
        return hashtext;
    }

}