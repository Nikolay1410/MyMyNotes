package com.example.mynotes;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mynotes.adapters.NotesAdapter;
import com.example.mynotes.db.NoteDBAdd;
import com.example.mynotes.db.NotesContract;
import com.example.mynotes.db.NotesDBHelper;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private final ArrayList<Note> notes = new ArrayList<>();
    private final ArrayList<Note> notesAdd = new ArrayList<>();
    public NotesAdapter adapter;
    public NotesDBHelper dbHelper;
    public NoteDBAdd dbAdd;
    private SQLiteDatabase database;
    private SQLiteDatabase databaseAdd;
    private FirebaseFirestore db;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch switchPriority;
    private FirebaseAuth mAuth;
    private String sortData = "ASC";
    private String sortBy = "data "+sortData;
    private TextView textViewNoteData;
    private TextView textViewPriority;

    private int seekId = 1;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_manu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.itemSighOut){
            signOut();
            mAuth.signOut();
        }
        return super.onOptionsItemSelected(item);
    }
    final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            this::onSignInResult
    );

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RecyclerView recyclerViewNotes = findViewById(R.id.recyclerViewNotes);
        mAuth = FirebaseAuth.getInstance();
        switchPriority = findViewById(R.id.switchPriority);
        textViewNoteData = findViewById(R.id.textViewNoteData);
        textViewPriority = findViewById(R.id.textViewPriority);
        db = FirebaseFirestore.getInstance();
        dellNoteCollection();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Intent intentDell = getIntent();

        dbHelper = new NotesDBHelper(this);
        database = dbHelper.getWritableDatabase();
        dbAdd = new NoteDBAdd(this);
        databaseAdd = dbAdd.getWritableDatabase();
        sortBy = preferences.getString("sort", null);
        getData(sortBy);
        getDataAdd();
        adapter = new NotesAdapter(notes);
        recyclerViewNotes.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewNotes.setAdapter(adapter);

        if (notesAdd.size()>0){
            for (Note noteAdd:notesAdd){
                Map<String, Object> note = new HashMap<>();
                note.put("id", noteAdd.getId());
                note.put("title", noteAdd.getTitle());
                note.put("description", noteAdd.getDescription());
                note.put("data", noteAdd.getData());
                note.put("priority", noteAdd.getPriority());
                db.collection("note")
                        .add(note);
                removeAdd(noteAdd.getId());
            }
        }

        if (intentDell.hasExtra("dell")){
            int positionDell = intentDell.getIntExtra("dell", 9999);
            if (sortBy.contains("data DESC")){
                remove(positionDell+1);
            }else {
                remove(positionDell);
            }
            adapter.notifyDataSetChanged();
        }

        if (Objects.equals(sortBy, "data ASC") || Objects.equals(sortBy, "data DESC")){
            switchPriority.setChecked(false);
            textViewNoteData.setTextColor(getResources().getColor(R.color.my_color));
            textViewPriority.setTextColor(getResources().getColor(R.color.black));
        }else {
            switchPriority.setChecked(true);
            textViewNoteData.setTextColor(getResources().getColor(R.color.black));
            textViewPriority.setTextColor(getResources().getColor(R.color.my_color));
        }

        adapter.setOnNoteClickListener(new NotesAdapter.OnNoteClickListener() {
            @Override
            public void onNoteClick(int position) {
                Note note = notes.get(position);
                Intent intent = new Intent(getApplicationContext(), AddNote.class);
                intent.putExtra("title", note.getTitle());
                intent.putExtra("description", note.getDescription());
                intent.putExtra("priority", note.getPriority());
                intent.putExtra("position", position);
                startActivity(intent);
            }
            @Override
            public void onLongClick(int position) {
                remove(position);
                adapter.notifyDataSetChanged();
            }
        });
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT|ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                remove(viewHolder.getAdapterPosition());
                adapter.notifyDataSetChanged();
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerViewNotes);
        switchPriority.setOnCheckedChangeListener((compoundButton, b) -> setMethodOfSort(b));

        if (mAuth.getCurrentUser() != null){
        }else {
            Toast.makeText(this, "Пользователь не зарегистрирован", Toast.LENGTH_SHORT).show();

        }
    }
    private void remove(int position){
        int id = notes.get(position).getId();
        String where = NotesContract.NotesEntry._ID+" =?";
        String[] whereArgs = new String[]{Integer.toString(id)};
        database.delete(NotesContract.NotesEntry.TABLE_NAME, where, whereArgs);
        getData(sortBy);
    }
    private void removeAdd(int idAdd){
        String where = NotesContract.NotesEntry.COLUMN_ID+" =?";
        String[] whereArgs = new String[]{Integer.toString(idAdd)};
        databaseAdd.delete(NotesContract.NotesEntry.TABLE_NAME, where, whereArgs);
    }

    public void onClickAddNote(View view) {
        Intent intent = new Intent(this, AddNote.class);
        startActivity(intent);
    }

    private void getData(String methodOfSort){
        notes.clear();
        Cursor cursor = database.query(NotesContract.NotesEntry.TABLE_NAME, null, null, null, null, null, methodOfSort);
        while (cursor.moveToNext()){
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(NotesContract.NotesEntry._ID));
            int myId = cursor.getInt(cursor.getColumnIndexOrThrow(NotesContract.NotesEntry.COLUMN_ID));
            String title = cursor.getString(cursor.getColumnIndexOrThrow(NotesContract.NotesEntry.COLUMN_TITLE));
            String description = cursor.getString(cursor.getColumnIndexOrThrow(NotesContract.NotesEntry.COLUMN_DESCRIPTION));
            String data = cursor.getString(cursor.getColumnIndexOrThrow(NotesContract.NotesEntry.COLUMN_DATA));
            int priority = cursor.getInt(cursor.getColumnIndexOrThrow(NotesContract.NotesEntry.COLUMN_PRIORITY));
            Note note = new Note(id, myId, title, description, data, priority);
            notes.add(note);
        }
        cursor.close();
    }

    private void getDataAdd(){
        notesAdd.clear();
        Cursor cursorAdd = databaseAdd.query(NotesContract.NotesEntry.TABLE_NAME, null, null, null, null, null, null);
        while (cursorAdd.moveToNext()){
            int id = cursorAdd.getInt(cursorAdd.getColumnIndexOrThrow(NotesContract.NotesEntry._ID));
            int myId = cursorAdd.getInt(cursorAdd.getColumnIndexOrThrow(NotesContract.NotesEntry.COLUMN_ID));
            String title = cursorAdd.getString(cursorAdd.getColumnIndexOrThrow(NotesContract.NotesEntry.COLUMN_TITLE));
            String description = cursorAdd.getString(cursorAdd.getColumnIndexOrThrow(NotesContract.NotesEntry.COLUMN_DESCRIPTION));
            String data = cursorAdd.getString(cursorAdd.getColumnIndexOrThrow(NotesContract.NotesEntry.COLUMN_DATA));
            int priority = cursorAdd.getInt(cursorAdd.getColumnIndexOrThrow(NotesContract.NotesEntry.COLUMN_PRIORITY));
            Note noteAdd = new Note(id, myId, title, description, data, priority);
            notesAdd.add(noteAdd);
        }
        cursorAdd.close();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void onClickNoteData(View view) {
        getData("data DESC");
        switchPriority.setChecked(false);
        textViewNoteData.setTextColor(getResources().getColor(R.color.my_color));
        textViewPriority.setTextColor(getResources().getColor(R.color.black));
        adapter.notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void onClickPriority(View view) {
        getData("priority");
        switchPriority.setChecked(true);
        textViewNoteData.setTextColor(getResources().getColor(R.color.black));
        textViewPriority.setTextColor(getResources().getColor(R.color.my_color));
        adapter.notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setMethodOfSort(boolean isTopRated){
        String methodOfSort;
        if (isTopRated){
            sortBy = "priority";
            methodOfSort = NotesContract.NotesEntry.COLUMN_PRIORITY;
            textViewNoteData.setTextColor(getResources().getColor(R.color.black));
            textViewPriority.setTextColor(getResources().getColor(R.color.my_color));
        }else {
            sortBy = "data "+sortData;
            methodOfSort = sortBy;
            textViewNoteData.setTextColor(getResources().getColor(R.color.my_color));
            textViewPriority.setTextColor(getResources().getColor(R.color.black));
        }
        getData(methodOfSort);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        preferences.edit().putString("sort", methodOfSort).apply();
        adapter.notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void onClickUp(View view) {
        if (sortBy.contains("data ASC") || sortBy.contains("data DESC")) {
            sortData = "ASC";
            sortBy = "data ASC";
            String sort = "data ASC";
            getData(sort);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            preferences.edit().putString("sort", sort).apply();
            adapter.notifyDataSetChanged();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void onClickDown(View view) {
        if (sortBy.contains("data ASC") || sortBy.contains("data DESC")) {
            sortData = "DESC";
            sortBy = "data DESC";
            String sort = "data DESC";
            getData(sort);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            preferences.edit().putString("sort", sort).apply();
            adapter.notifyDataSetChanged();
        }
    }

    @SuppressLint("InflateParams")
    public void addFirebase(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        view = inflater.inflate(R.layout.seek_bar, null);
        builder.setView(view);
        SeekBar seekBar = (SeekBar) view.findViewById(R.id.seekBar);
        TextView seekProgress = (TextView) view.findViewById(R.id.seekProgress);
        seekBar.setMax(29);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                seekId = i+1;
                seekProgress.setText(""+seekId);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        builder.setPositiveButton("Oк", (dialog, id) -> db.collection("note")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot == null) return;
                        long nawTime = System.currentTimeMillis();
                        long day = 86400000L * seekId;
                        long dayAgo = nawTime - day;
                        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatForDateNow = new SimpleDateFormat("dd.MM.yyyy hh:mm");
                        Date dataNote;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> user = document.getData();
                            String dataUser = Objects.requireNonNull(user.get("data")).toString();
                            try {
                                dataNote = formatForDateNow.parse(dataUser);
                                assert dataNote != null;
                                long longNote = dataNote.getTime();
                                if (longNote>dayAgo){
                                    ContentValues contentValues = new ContentValues();
                                    contentValues.put(NotesContract.NotesEntry.COLUMN_ID, Objects.requireNonNull(user.get("id")).toString());
                                    contentValues.put(NotesContract.NotesEntry.COLUMN_TITLE, Objects.requireNonNull(user.get("title")).toString());
                                    contentValues.put(NotesContract.NotesEntry.COLUMN_DESCRIPTION, Objects.requireNonNull(user.get("description")).toString());
                                    contentValues.put(NotesContract.NotesEntry.COLUMN_DATA, Objects.requireNonNull(user.get("data")).toString());
                                    contentValues.put(NotesContract.NotesEntry.COLUMN_PRIORITY, Objects.requireNonNull(user.get("priority")).toString());
                                    database.insert(NotesContract.NotesEntry.TABLE_NAME, null, contentValues);
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }
                        switchPriority.setChecked(!switchPriority.isChecked());
                    } else {
                        Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
                    }
                }));
        builder.setNegativeButton("Закрыть", (dialog, id) -> dialog.cancel());
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        IdpResponse response = result.getIdpResponse();
        if (result.getResultCode() == RESULT_OK) {
            // Successfully signed in
            FirebaseUser user = mAuth.getCurrentUser();
            Toast.makeText(this, "Авторизирован: "+ Objects.requireNonNull(mAuth.getCurrentUser()).getEmail(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Пользователь не зарегистрирован", Toast.LENGTH_SHORT).show();
            signOut();
        }
    }

    private void signOut(){
        AuthUI.getInstance().signOut(this).addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                List<AuthUI.IdpConfig> providers = Arrays.asList(
                        new AuthUI.IdpConfig.EmailBuilder().build(),
                        new AuthUI.IdpConfig.PhoneBuilder().build(),
                        new AuthUI.IdpConfig.GoogleBuilder().build());

// Create and launch sign-in intent
                Intent signInIntent = AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build();
                signInLauncher.launch(signInIntent);
            }
        });

    }
    void dellNoteCollection(){
        long nawTime = System.currentTimeMillis();
        long month = 86400000L * 30;
        long monthAgo = nawTime - month;
        db.collection("note")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot == null) return;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> user = document.getData();
                            String kay = document.getId();
                            String dataUser = Objects.requireNonNull(user.get("data")).toString();
                            @SuppressLint("SimpleDateFormat") SimpleDateFormat formatForDateNow = new SimpleDateFormat("dd.MM.yyyy hh:mm");
                            Date dataNote;
                            try {
                                dataNote = formatForDateNow.parse(dataUser);
                                assert dataNote != null;
                                long longNote = dataNote.getTime();
                                if (longNote<monthAgo){
                                    db.collection("note").document(kay)
                                            .delete()
                                            .addOnSuccessListener(aVoid -> {

                                            })
                                            .addOnFailureListener(e -> {

                                            });
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }

                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

}