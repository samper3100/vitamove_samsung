package com.martist.vitamove.fragments;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.martist.vitamove.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class NutrientCategoryFragment extends Fragment {
    
    private static final String ARG_CATEGORY = "category";
    private static final String ARG_NUTRIENT_IDS = "nutrient_ids";
    private static final String ARG_NUTRIENT_NAMES = "nutrient_names";
    private static final String ARG_SELECTED_NUTRIENTS = "selected_nutrients";
    
    private String category;
    private ArrayList<String> nutrientIds;
    private ArrayList<String> nutrientNames;
    private ArrayList<String> selectedNutrients;
    private final List<MaterialCheckBox> checkboxes = new ArrayList<>();
    private NutrientSelectionListener listener;
    
    
    public interface NutrientSelectionListener {
        
        boolean onNutrientSelectionChanged(String nutrientId, boolean isChecked);
    }
    
    
    public static NutrientCategoryFragment newInstance(String category, 
                                                      ArrayList<String> nutrientIds, 
                                                      ArrayList<String> nutrientNames,
                                                      ArrayList<String> selectedNutrients) {
        NutrientCategoryFragment fragment = new NutrientCategoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY, category);
        args.putStringArrayList(ARG_NUTRIENT_IDS, nutrientIds);
        args.putStringArrayList(ARG_NUTRIENT_NAMES, nutrientNames);
        args.putStringArrayList(ARG_SELECTED_NUTRIENTS, selectedNutrients);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (getArguments() != null) {
            category = getArguments().getString(ARG_CATEGORY);
            nutrientIds = getArguments().getStringArrayList(ARG_NUTRIENT_IDS);
            nutrientNames = getArguments().getStringArrayList(ARG_NUTRIENT_NAMES);
            selectedNutrients = getArguments().getStringArrayList(ARG_SELECTED_NUTRIENTS);
        }
        
        if (getParentFragment() instanceof NutrientSelectionListener) {
            listener = (NutrientSelectionListener) getParentFragment();
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nutrient_category, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        LinearLayout checkboxContainer = view.findViewById(R.id.checkbox_container);
        
        
        boolean isNightMode = (requireContext().getResources().getConfiguration().uiMode & 
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
                
        
        if (nutrientIds != null && nutrientNames != null) {
            for (int i = 0; i < nutrientIds.size(); i++) {
                String nutrientId = nutrientIds.get(i);
                String nutrientName = nutrientNames.get(i);
                
                MaterialCheckBox checkbox = new MaterialCheckBox(requireContext());
                checkbox.setText(nutrientName);
                checkbox.setTag(nutrientId);
                checkbox.setTextSize(16);
                checkbox.setPadding(8, 12, 8, 12);
                
                
                if (isNightMode) {
                    checkbox.setTextColor(requireContext().getResources().getColor(R.color.primary_text));
                }
                
                
                checkbox.setChecked(selectedNutrients != null && selectedNutrients.contains(nutrientId));
                
                
                checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (listener != null) {
                        if (isChecked && !listener.onNutrientSelectionChanged(nutrientId, true)) {
                            
                            checkbox.setOnCheckedChangeListener(null);
                            checkbox.setChecked(false);
                            checkbox.setOnCheckedChangeListener((b, c) -> 
                                listener.onNutrientSelectionChanged(nutrientId, c));
                        } else {
                            listener.onNutrientSelectionChanged(nutrientId, isChecked);
                        }
                    }
                });
                
                checkboxes.add(checkbox);
                
                
                LinearLayout checkboxWrapper = new LinearLayout(requireContext());
                checkboxWrapper.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                checkboxWrapper.setBackground(requireContext().getDrawable(R.drawable.checkbox_ripple_background));
                checkboxWrapper.addView(checkbox);
                
                checkboxContainer.addView(checkboxWrapper);
            }
        }
    }
    
    
    public void updateSelectedNutrients(Set<String> selectedNutrients) {
        for (MaterialCheckBox checkbox : checkboxes) {
            String nutrientId = (String) checkbox.getTag();
            
            
            checkbox.setOnCheckedChangeListener(null);
            checkbox.setChecked(selectedNutrients.contains(nutrientId));
            
            
            checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    if (isChecked && !listener.onNutrientSelectionChanged(nutrientId, true)) {
                        
                        checkbox.setOnCheckedChangeListener(null);
                        checkbox.setChecked(false);
                        checkbox.setOnCheckedChangeListener((b, c) -> 
                            listener.onNutrientSelectionChanged(nutrientId, c));
                    } else {
                        listener.onNutrientSelectionChanged(nutrientId, isChecked);
                    }
                }
            });
        }
    }
    
    
    public List<MaterialCheckBox> getCheckboxes() {
        return checkboxes;
    }
} 