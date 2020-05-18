package ugr.gbv.cognimobile.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import ugr.gbv.cognimobile.R;
import ugr.gbv.cognimobile.interfaces.TextTaskCallback;

public class WordListAdapter extends RecyclerView.Adapter<WordListAdapter.MyViewHolder> {
    private ArrayList<String> mDatasetReversed;
    TextTaskCallback taskCallback;



    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class MyViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        TextView word;
        EditText editableWord;
        ImageButton editButton;
        ImageButton deleteButton;
        boolean isBeingEdited;



        MyViewHolder(View itemView) {
            super(itemView);

            word =  itemView.findViewById(R.id.word);
            editableWord =  itemView.findViewById(R.id.editWord);
            editButton =  itemView.findViewById(R.id.icon_edit);
            deleteButton =  itemView.findViewById(R.id.icon_delete);
            isBeingEdited = false;

        }

        void displayTextView(){
            word.setVisibility(View.VISIBLE);
            editableWord.setVisibility(View.GONE);
            isBeingEdited = false;
        }

        void displayEditable(){
            word.setVisibility(View.GONE);
            editableWord.setVisibility(View.VISIBLE);
            editableWord.setText(word.getText());
            isBeingEdited = true;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public WordListAdapter(TextTaskCallback taskCallback) {
        mDatasetReversed = new ArrayList<>();
        this.taskCallback = taskCallback;
    }

    // Create new views (invoked by the layout manager)
    @Override
    @NonNull
    public WordListAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                           int viewType) {

        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.word_view;
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(layoutIdForListItem, parent, false);

        return new MyViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        View.OnClickListener editBehaviour = v -> {
            if(holder.isBeingEdited){
                editWord(holder.word.getText().toString(), holder.editableWord.getText().toString());
                holder.displayTextView();
            }
            else{
                //holder.displayEditable();
                taskCallback.editWord(holder.word.getText().toString());
            }
        };



        if(mDatasetReversed.get(position) != null && !mDatasetReversed.get(position).isEmpty()){
            holder.word.setText(mDatasetReversed.get(position));
            holder.editButton.setOnClickListener(editBehaviour);
            holder.deleteButton.setOnClickListener(v -> removeWord(mDatasetReversed.get(position)));
        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDatasetReversed.size();
    }

    public void addWord(String word){
        if(!mDatasetReversed.contains(word)){
            mDatasetReversed.add(0,word);
            notifyItemInserted(0);
        }
    }

    private void addWordInIndex(String word, int index){
        if (!mDatasetReversed.contains(word)) {
            mDatasetReversed.add(index, word);
            notifyItemInserted(index);
        }
    }

    private void removeWord(String word) {
        /*if(mDatasetReversed.size() > position) {
            mDatasetReversed.remove(position);
            notifyItemRemoved(position);
        }*/

        int position = mDatasetReversed.indexOf(word);
        if (position >= 0) {
            mDatasetReversed.remove(position);
            notifyItemRemoved(position);
        }
        notifyDataSetChanged();
    }

    public void editWord(String original, String replace){
        int position = mDatasetReversed.indexOf(original);
        removeWord(original);
        addWordInIndex(replace,position);
        notifyItemChanged(position);
    }

    public void removeAllWords() {
        mDatasetReversed = new ArrayList<>();
    }

    public ArrayList<String> getAllWords() {
        return mDatasetReversed;
    }
}