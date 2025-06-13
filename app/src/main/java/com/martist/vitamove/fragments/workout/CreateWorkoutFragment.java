package com.martist.vitamove.fragments.workout;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.martist.vitamove.R;
import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.create_program.CreateProgramWeekActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class CreateWorkoutFragment extends Fragment {

    private static final String TAG = "CreateWorkoutFragment";
    
    private TextInputLayout nameLayout;
    private TextInputEditText nameEditText;
    private Spinner levelSpinner;
    private Spinner weeksSpinner;
    private Spinner daysPerWeekSpinner;
    private Button nextButton;

    
    public static CreateWorkoutFragment newInstance() {
        return new CreateWorkoutFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_workout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        
        initViews(view);
        
        
        setupSpinners();
        
        
        setupNextButton();
    }
    
    
    private void initViews(View view) {
        nameLayout = view.findViewById(R.id.name_input_layout);
        nameEditText = view.findViewById(R.id.name_edit_text);
        levelSpinner = view.findViewById(R.id.level_spinner);
        weeksSpinner = view.findViewById(R.id.weeks_spinner);
        daysPerWeekSpinner = view.findViewById(R.id.days_per_week_spinner);
        nextButton = view.findViewById(R.id.next_button);
    }
    
    
    private void setupSpinners() {
        
        List<String> levels = Arrays.asList(
                getString(R.string.level_beginner),
                getString(R.string.level_intermediate),
                getString(R.string.level_advanced)
        );
        
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                levels
        );
        levelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        levelSpinner.setAdapter(levelAdapter);
        
        
        List<String> weeks = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            weeks.add(String.valueOf(i));
        }
        
        ArrayAdapter<String> weeksAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                weeks
        );
        weeksAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        weeksSpinner.setAdapter(weeksAdapter);
        
        
        List<String> daysPerWeek = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            daysPerWeek.add(String.valueOf(i));
        }
        
        ArrayAdapter<String> daysAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                daysPerWeek
        );
        daysAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        daysPerWeekSpinner.setAdapter(daysAdapter);
    }
    
    
    private void setupNextButton() {
        nextButton.setOnClickListener(v -> {
            if (validateInput()) {
                createWorkoutProgram();
            }
        });
    }
    
    
    private boolean validateInput() {
        String name = nameEditText.getText() != null ? nameEditText.getText().toString().trim() : "";
        
        if (name.isEmpty()) {
            nameLayout.setError(getString(R.string.error_empty_name));
            return false;
        } else {
            nameLayout.setError(null);
        }
        
        return true;
    }
    
    
    private void createWorkoutProgram() {
        String name = nameEditText.getText().toString().trim();
        String level = levelSpinner.getSelectedItem().toString();
        int weeks = Integer.parseInt(weeksSpinner.getSelectedItem().toString());
        int daysPerWeek = Integer.parseInt(daysPerWeekSpinner.getSelectedItem().toString());
        
        
        String userId = ((VitaMoveApplication) requireActivity().getApplication()).getCurrentUserId();
        
        
        Intent intent = new Intent(requireActivity(), CreateProgramWeekActivity.class);
        intent.putExtra("NUMBER_OF_DAYS", daysPerWeek);
        intent.putExtra(CreateProgramWeekActivity.EXTRA_TOTAL_WEEKS, weeks);
        intent.putExtra("PROGRAM_NAME", name);
        intent.putExtra("PROGRAM_LEVEL", level);
        requireActivity().startActivity(intent);
    }
    
    
    private void navigateToProgramSetup(String programId) {
        
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
} 