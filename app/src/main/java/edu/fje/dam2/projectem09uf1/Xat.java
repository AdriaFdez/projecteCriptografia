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

import java.util.ArrayList;


public class Xat extends AppCompatActivity {

    private TextView usuari;
    private EditText msg;
    private FloatingActionButton btSend;

    private ArrayList<String> resultats;
    private ListView lvRanking;

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
        updateChat();
    }

    public void updateChat() {
        ArrayAdapter<String> itemsAdapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, resultats);

        ListView listView = (ListView) findViewById(R.id.lvXat);
        listView.setAdapter(itemsAdapter);
    }


    public void sendMsg(View v){
        String content = String.valueOf(usuari.getText()) + "-" + String.valueOf(msg.getText());
        Log.i("MSG", content);
    }

}