package com.example.mynotes;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.example.mynotes.db.NoteDBAdd;
import com.example.mynotes.db.NotesContract;
import com.example.mynotes.db.NotesDBHelper;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AddNote extends AppCompatActivity {
    private EditText editTextTitle;
    private EditText editTextDescription;
    private RadioGroup radioGroupPriority;
    private SQLiteDatabase database;
    private SQLiteDatabase databaseAdd;
    private Button buttonSaveNote;
    private int position = -1;
    private String title;
    private String description;
    private String data;
    private int priority;
    private FirebaseFirestore db;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextDescription = findViewById(R.id.editTextDescription);
        radioGroupPriority = findViewById(R.id.radioGroupPriority);
        buttonSaveNote = findViewById(R.id.buttonSaveNote);
        db = FirebaseFirestore.getInstance();
        RadioButton button1 = findViewById(R.id.button1);
        RadioButton button2 = findViewById(R.id.button2);
        RadioButton button3 = findViewById(R.id.button3);
        EditText editTextTitle = findViewById(R.id.editTextTitle);
        EditText editTextDescription = findViewById(R.id.editTextDescription);
        Date dateNow = new Date();
        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatForDateNow = new SimpleDateFormat("dd.MM.yyyy hh:mm");
        data = formatForDateNow.format(dateNow);
        NotesDBHelper dbHelper = new NotesDBHelper(this);
        database = dbHelper.getWritableDatabase();
        NoteDBAdd dbAdd = new NoteDBAdd(this);
        databaseAdd = dbAdd.getWritableDatabase();
        Intent intent = getIntent();
        if (intent.hasExtra("title") && intent.hasExtra("description") && intent.hasExtra("priority") && intent.hasExtra("position")){
            buttonSaveNote.setText("Изменить заметку");
            title = intent.getStringExtra("title");
            description = intent.getStringExtra("description");
            priority = intent.getIntExtra("priority", 1);
            position = intent.getIntExtra("position", 1);
            editTextTitle.setText(title);
            editTextDescription.setText(description);
            Log.i("hhhh", ""+priority);
            switch (priority){
                case 3: button1.setChecked(true);
                    break;
                case 2: button2.setChecked(true);
                    break;
                case 1: button3.setChecked(true);
            }
        }
    }

    @SuppressLint("CommitPrefEdits")
    public void onClickAddNote(View view) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        int myId = preferences.getInt("id", 0);
        title = editTextTitle.getText().toString().trim();
        description = editTextDescription.getText().toString().trim();
        int radioButtonId = radioGroupPriority.getCheckedRadioButtonId();
        RadioButton radioButton = findViewById(radioButtonId);
        priority = Integer.parseInt(radioButton.getText().toString());
        if(isFilled(title, description)) {
            int id = myId+1;
            preferences.edit().putInt("id", id);
            ContentValues contentValues = new ContentValues();
            contentValues.put(NotesContract.NotesEntry.COLUMN_ID, id);
            contentValues.put(NotesContract.NotesEntry.COLUMN_TITLE, title);
            contentValues.put(NotesContract.NotesEntry.COLUMN_DESCRIPTION, description);
            contentValues.put(NotesContract.NotesEntry.COLUMN_DATA, data);
            contentValues.put(NotesContract.NotesEntry.COLUMN_PRIORITY, priority);
            database.insert(NotesContract.NotesEntry.TABLE_NAME, null, contentValues);
            Map<String, Object> note = new HashMap<>();
            note.put("id", id);
            note.put("title", title);
            note.put("description", description);
            note.put("data", data);
            note.put("priority", priority);
            db.collection("note")
                    .add(note)
                    .addOnSuccessListener(documentReference -> {

                    })
                    .addOnFailureListener(e -> {
                        ContentValues contentValuesAdd = new ContentValues();
                        contentValuesAdd.put(NotesContract.NotesEntry.COLUMN_ID, id);
                        contentValuesAdd.put(NotesContract.NotesEntry.COLUMN_TITLE, title);
                        contentValuesAdd.put(NotesContract.NotesEntry.COLUMN_DESCRIPTION, description);
                        contentValuesAdd.put(NotesContract.NotesEntry.COLUMN_DATA, data);
                        contentValuesAdd.put(NotesContract.NotesEntry.COLUMN_PRIORITY, priority);
                        databaseAdd.insert(NotesContract.NotesEntry.TABLE_NAME, null, contentValuesAdd);
                    });

            Intent intent = new Intent(this, MainActivity.class);
            if (position!= -1){
                intent.putExtra("dell", position);
            }
            startActivity(intent);
        }else {
            Toast.makeText(this, "Все поля должны быть заполнены!", Toast.LENGTH_SHORT).show();
        }
        buttonSaveNote.setText("Добавить заметку");
    }
    private boolean isFilled(String title, String description){
        return !title.isEmpty() && !description.isEmpty();
    }
}