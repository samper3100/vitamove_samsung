package com.martist.vitamove.workout.adapters;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;
import com.martist.vitamove.workout.data.models.ExerciseSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CardioSetAdapter extends RecyclerView.Adapter<CardioSetAdapter.SetViewHolder> {
    private static final String TAG = "CardioSetAdapter";
    private static final int TYPE_ACTIVE = 1;
    private static final int TYPE_INACTIVE = 2;
    
    private List<ExerciseSet> sets = new ArrayList<>();
    private OnSetClickListener listener;
    
    
    public interface OnDataChangeListener {
        void onDataChanged(ExerciseSet set, int position);
    }
    
    private OnDataChangeListener dataChangeListener;
    
    public void setOnDataChangeListener(OnDataChangeListener listener) {
        this.dataChangeListener = listener;
    }
    
    public interface OnSetClickListener {
        void onSetClick(ExerciseSet set, int position, boolean isCompleted);
    }
    
    public void setOnSetClickListener(OnSetClickListener listener) {
        this.listener = listener;
    }
    

    
    @Override
    public int getItemViewType(int position) {
        if (position < sets.size()) {
            return TYPE_ACTIVE;
        }
        return TYPE_INACTIVE;
    }
    
    @NonNull
    @Override
    public SetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_ACTIVE) {
            View view = inflater.inflate(R.layout.item_exercise_set_cardio, parent, false);
            SetViewHolder holder = new ActiveSetViewHolder(view);
            viewHolders.add(holder);
            return holder;
        } else {
            View view = inflater.inflate(R.layout.item_exercise_set_cardio_inactive, parent, false);
            SetViewHolder holder = new InactiveSetViewHolder(view);
            viewHolders.add(holder);
            return holder;
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull SetViewHolder holder, int position) {
        if (holder instanceof ActiveSetViewHolder) {
            ((ActiveSetViewHolder) holder).bind(sets.get(position), position);
        } else if (holder instanceof InactiveSetViewHolder) {
            ((InactiveSetViewHolder) holder).bind();
        }
    }
    
    @Override
    public int getItemCount() {
        return sets.size() + 1;
    }
    
    
    public void updateSets(List<ExerciseSet> newSets) {
        if (newSets == null) {
            Log.e(TAG, "updateSets: список подходов равен null");
            return;
        }
        
        try {
            
            final List<ExerciseSet> setsCopy = new ArrayList<>(newSets);
            
            
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    
                    RecyclerView parent = (RecyclerView) getRecyclerView();
                    if (parent == null || parent.isComputingLayout()) {
                        
                        
                        parent.post(() -> safeUpdateList(setsCopy));
                    } else {
                        
                        safeUpdateList(setsCopy);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "updateSets: ошибка при обновлении в UI потоке: " + e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "updateSets: ошибка при обновлении списка подходов: " + e.getMessage(), e);
        }
    }
    
    
    private void safeUpdateList(List<ExerciseSet> newSets) {
        try {
            
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return sets.size();
                }
    
                @Override
                public int getNewListSize() {
                    return newSets.size();
                }
    
                @Override
                public boolean areItemsTheSame(int oldPos, int newPos) {
                    return sets.get(oldPos).getId().equals(newSets.get(newPos).getId());
                }
    
                @Override
                public boolean areContentsTheSame(int oldPos, int newPos) {
                    ExerciseSet oldSet = sets.get(oldPos);
                    ExerciseSet newSet = newSets.get(newPos);
                    return oldSet.isCompleted() == newSet.isCompleted() &&
                           oldSet.getDurationSeconds() == newSet.getDurationSeconds();
                }
            });
            
            
            sets.clear();
            sets.addAll(newSets);
            
            
            diffResult.dispatchUpdatesTo(this);
            
            
        } catch (Exception e) {
            Log.e(TAG, "safeUpdateList: ошибка при безопасном обновлении списка: " + e.getMessage(), e);
            
            
            try {
                sets.clear();
                sets.addAll(newSets);
                notifyDataSetChanged();
            } catch (Exception ex) {
                Log.e(TAG, "safeUpdateList: ошибка при резервном обновлении: " + ex.getMessage(), ex);
            }
        }
    }
    
    
    private RecyclerView getRecyclerView() {
        RecyclerView.ViewHolder holder = viewHolders.size() > 0 ? viewHolders.get(0) : null;
        return holder != null ? (RecyclerView) holder.itemView.getParent() : null;
    }
    
    
    private final List<SetViewHolder> viewHolders = new ArrayList<>();
    
    
    abstract class SetViewHolder extends RecyclerView.ViewHolder {
        public SetViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
    
    class ActiveSetViewHolder extends SetViewHolder {
        private final TextView setNumberText;
        private final EditText minutesEdit;
        private final EditText secondsEdit;
        private final ImageButton actionButton;
        
        ActiveSetViewHolder(@NonNull View itemView) {
            super(itemView);
            setNumberText = itemView.findViewById(R.id.set_number);
            minutesEdit = itemView.findViewById(R.id.minutes_input);
            secondsEdit = itemView.findViewById(R.id.seconds_input);
            actionButton = itemView.findViewById(R.id.action_button);

            
            minutesEdit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        try {
                            int minutes = s.length() > 0 ? Integer.parseInt(s.toString()) : 0;
                            int seconds = 0;
                            if (sets.get(position).getDurationSeconds() != null) {
                                seconds = sets.get(position).getDurationSeconds() % 60;
                            }
                            
                            int totalSeconds = minutes * 60 + seconds;
                            sets.get(position).setDurationSeconds(totalSeconds);
                            
                            
                            if (dataChangeListener != null) {
                                dataChangeListener.onDataChanged(sets.get(position), position);
                            }
                        } catch (NumberFormatException e) {
                            minutesEdit.setText("0");
                        }
                    }
                }
            });

            
            minutesEdit.setOnEditorActionListener((v, actionId, event) -> {
                int position = getAdapterPosition();
                
                if (position == sets.size() - 1) {
                    
                    secondsEdit.requestFocus();
                    return true;
                }
                
                return false;
            });

            
            secondsEdit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        try {
                            int seconds = s.length() > 0 ? Integer.parseInt(s.toString()) : 0;
                            
                            if (seconds > 59) {
                                seconds = 59;
                                secondsEdit.setText("59");
                            }
                            
                            int minutes = 0;
                            if (sets.get(position).getDurationSeconds() != null) {
                                minutes = sets.get(position).getDurationSeconds() / 60;
                            }
                            
                            int totalSeconds = minutes * 60 + seconds;
                            sets.get(position).setDurationSeconds(totalSeconds);
                            
                            
                            sets.get(position).setReps(null);
                            sets.get(position).setWeight(null);
                            
                            
                            if (dataChangeListener != null) {
                                dataChangeListener.onDataChanged(sets.get(position), position);
                            }
                        } catch (NumberFormatException e) {
                            secondsEdit.setText("0");
                        }
                    }
                }
            });
            
            
            secondsEdit.setOnEditorActionListener((v, actionId, event) -> {
                int position = getAdapterPosition();
                
                if (position == sets.size() - 1) {
                    
                    
                    secondsEdit.clearFocus();
                    return true;
                }
                
                return false;
            });

            actionButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && position < sets.size() && listener != null) {
                    ExerciseSet currentSet = sets.get(position);
                    
                    listener.onSetClick(currentSet, position, !currentSet.isCompleted());
                }
            });
        }

        void bind(ExerciseSet set, int position) {
            setNumberText.setText(String.valueOf(set.getSetNumber()));
            
            
            int durationSeconds = set.getDurationSeconds() != null ? set.getDurationSeconds() : 0;
            int minutes = durationSeconds / 60;
            int seconds = durationSeconds % 60;
            
            
            minutesEdit.setText(String.valueOf(minutes));
            secondsEdit.setText(String.format(Locale.getDefault(), "%02d", seconds));
            
            
            actionButton.setImageResource(set.isCompleted() ? R.drawable.ic_check : R.drawable.ic_delete);
            actionButton.setEnabled(true); 
            actionButton.setColorFilter(itemView.getContext().getColor(
                set.isCompleted() ? R.color.green_500 : R.color.gray_500)); 
        }
    }

    class InactiveSetViewHolder extends SetViewHolder {

        private final ImageButton actionButton;

        InactiveSetViewHolder(@NonNull View itemView) {
            super(itemView);

            actionButton = itemView.findViewById(R.id.action_button);

            itemView.setOnClickListener(v -> addNewSet());
            actionButton.setOnClickListener(v -> addNewSet());
        }

        void bind() {
            
        }

        private void addNewSet() {
            if (listener != null) {
                try {
                    
                    boolean hasTemporarySet = false;
                    for (ExerciseSet set : sets) {
                        if (set.getId() != null && set.getId().startsWith("temp_") && !set.isCompleted()) {
                            hasTemporarySet = true;
                            break;
                        }
                    }
                    
                    if (hasTemporarySet) {
                        
                        return;
                    }
                    
                    
                    ExerciseSet newSet = new ExerciseSet(null, null, 0, false);
                    
                    listener.onSetClick(newSet, sets.size(), false);
                } catch (Exception e) {
                    Log.e(TAG, "addNewSet: Ошибка при добавлении нового подхода: " + e.getMessage(), e);
                }
            }
        }
    }
    
    @Override
    public void onViewRecycled(@NonNull SetViewHolder holder) {
        super.onViewRecycled(holder);
        
        if (holder instanceof ActiveSetViewHolder) {
            ActiveSetViewHolder activeHolder = (ActiveSetViewHolder) holder;
            activeHolder.minutesEdit.clearFocus();
            activeHolder.secondsEdit.clearFocus();
        }
    }
} 