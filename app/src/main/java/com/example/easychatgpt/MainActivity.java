package com.example.easychatgpt;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;


public class MainActivity extends AppCompatActivity {

    static final String WS_URL = "ws://192.168.145.79:6060";
    RecyclerView recyclerView;
    TextView welcomeTextView;
    EditText messageEditText;
    ImageButton sendButton;
    List<Message> messageList;
    MessageAdapter messageAdapter;

    // Text Speech
    static TextToSpeech textToSpeech;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        System.out.println("Test");
        messageList = new ArrayList<>();

        recyclerView = findViewById(R.id.recycler_view);
        welcomeTextView = findViewById(R.id.welcome_text);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_btn);

        //setup recycler view
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);

        //initialize the Text To Speech Engine
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS){
                    // Set the Language
                    textToSpeech.setLanguage(Locale.UK);
                    textToSpeech.setSpeechRate(1.0f);
                }
                else {
                    System.out.println("there is a problem with the TTS");
                }
            }
        });

        // Connecting to server
        webSocketConnection();

        sendButton.setOnClickListener((v)->{
            String question = messageEditText.getText().toString().trim();
            addToChat(question,Message.SENT_BY_USER);
            //TODO add text to speech for the written message
            messageEditText.setText("");
            welcomeTextView.setVisibility(View.GONE);
        });
    }

    void addToChat(String message,String sentBy){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageList.add(new Message(message,sentBy));
                messageAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());

                // play the message if it is from the user
                if (sentBy.equals(Message.SENT_BY_USER)){
                    playText(message);
                }
            }
        });
    }

    void addResponse(String response){
        //messageList.remove(messageList.size()-1); // this causes problem don't know why
        addToChat(response,Message.SENT_BY_SERVER);
    }

    // WebSocketConnection for autobahn
    public void webSocketConnection() {
        try {
            WebSocketConnection con = new WebSocketConnection();
            con.connect("ws://192.168.145.79:6060", new WebSocketHandler() {

                @Override
                public void onOpen() {
                    addResponse("open connection");
                }

                @Override
                public void onClose(int code, String reason) {
                    addResponse("close connection");
                }

                @Override
                public void onTextMessage(String payload) {
                    addResponse(payload);
                }
            });
        } catch (WebSocketException e) {
            e.printStackTrace();
            addResponse(e.getMessage());
        }
    }

    // Method to play the text using Text-to-Speech engine
    static void playText(String text) {
        if (textToSpeech != null && !textToSpeech.isSpeaking()) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }
}
