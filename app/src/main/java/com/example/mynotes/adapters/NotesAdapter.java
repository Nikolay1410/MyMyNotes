package com.example.mynotes.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mynotes.Note;
import com.example.mynotes.R;

import java.util.ArrayList;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NotesViewHolder> {
    private final ArrayList<Note> notes;
    private OnNoteClickListener onNoteClickListener;

    public NotesAdapter(ArrayList<Note> notes) {
        this.notes = notes;
    }

    public interface OnNoteClickListener{
        void onNoteClick(int position);
        void onLongClick(int position);
    }

    public void setOnNoteClickListener(OnNoteClickListener onNoteClickListener) {
        this.onNoteClickListener = onNoteClickListener;
    }

    @NonNull
    @Override
    public NotesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_item, parent, false);
        return new NotesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotesViewHolder holder, int position) {
        Note note = notes.get(position);

        holder.textViewTitle.setText(note.getTitle());
        holder.textViewDescription.setText(note.getDescription());
        holder.textViewData.setText(note.getData());
        int colorId;
        int priority = note.getPriority();
        switch (priority){
            case 1:
                colorId = holder.itemView.getResources().getColor(android.R.color.holo_red_dark);
                break;
            case 2:
                colorId = holder.itemView.getResources().getColor(android.R.color.holo_orange_dark);
                break;
            default :
                colorId = holder.itemView.getResources().getColor(android.R.color.holo_green_dark);
                break;
        }
        holder.textViewTitle.setBackgroundColor(colorId);
        holder.textViewData.setBackgroundColor(colorId);
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    class NotesViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewTitle;
        private final TextView textViewDescription;
        private final TextView textViewData;

        public NotesViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewDescription = itemView.findViewById(R.id.textViewDescription);
            textViewData = itemView.findViewById(R.id.textViewData);
            itemView.setOnClickListener(view -> {
                if(onNoteClickListener!=null){
                    onNoteClickListener.onNoteClick(getAdapterPosition());
                }
            });
            itemView.setOnLongClickListener(view -> {
                if(onNoteClickListener!=null){
                    onNoteClickListener.onLongClick(getAdapterPosition());
                }
                return true;
            });
        }
    }

}
