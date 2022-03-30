package edu.fje.dam2.projectem09uf1;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.google.android.material.snackbar.Snackbar;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class Login extends AppCompatActivity {

    private EditText etPwd, etUsr;
    private Map<String,String> users = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        users.put("Adria", "abc");
        users.put("David", "def");

        etUsr = (EditText) findViewById(R.id.etUsr);
        etPwd = (EditText) findViewById(R.id.etPwd);
    }

    public void login(View v) {
        boolean validUsr = false;
        String usr = String.valueOf(etUsr.getText());
        String pwd = String.valueOf(etPwd.getText());

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

        if(validUsr){
            Intent intent = new Intent(this, Xat.class);
            startActivity(intent);
        }else{
            Snackbar.make(v, "Login incorrecte", Snackbar.LENGTH_LONG).setAction("Action", null).show();
        }
    }

}