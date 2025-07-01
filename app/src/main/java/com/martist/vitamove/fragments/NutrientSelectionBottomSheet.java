package com.martist.vitamove.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.martist.vitamove.R;
import com.martist.vitamove.adapters.NutrientCategoryAdapter;
import com.martist.vitamove.events.TrackedNutrientsChangedEvent;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NutrientSelectionBottomSheet extends BottomSheetDialogFragment 
        implements NutrientCategoryFragment.NutrientSelectionListener {
    
    private static final String PREFS_NAME = "VitaMoveNutrientPrefs";
    private static final String TRACKED_NUTRIENTS_KEY = "tracked_nutrients";
    private static final int MAX_SELECTIONS = 3;
    
    private SharedPreferences prefs;
    private Set<String> selectedNutrients = new HashSet<>();
    

    private final ArrayList<String> vitaminIds = new ArrayList<>();
    private final ArrayList<String> vitaminNames = new ArrayList<>();
    
    private final ArrayList<String> mineralIds = new ArrayList<>();
    private final ArrayList<String> mineralNames = new ArrayList<>();
    
    private final ArrayList<String> otherNutrientIds = new ArrayList<>();
    private final ArrayList<String> otherNutrientNames = new ArrayList<>();
    
    private NutrientCategoryAdapter adapter;
    private ViewPager2 viewPager;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.CustomBottomSheetStyle);
        
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        

        initializeNutrientLists();
        

        selectedNutrients = new HashSet<>(prefs.getStringSet(TRACKED_NUTRIENTS_KEY, new HashSet<>()));
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_nutrient_selection, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        

        viewPager = view.findViewById(R.id.view_pager);
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        

        adapter = new NutrientCategoryAdapter(this);
        

        NutrientCategoryFragment vitaminsFragment = NutrientCategoryFragment.newInstance(
                "Витамины", vitaminIds, vitaminNames, new ArrayList<>(selectedNutrients));
        
        NutrientCategoryFragment mineralsFragment = NutrientCategoryFragment.newInstance(
                "Минералы", mineralIds, mineralNames, new ArrayList<>(selectedNutrients));
        
        NutrientCategoryFragment otherFragment = NutrientCategoryFragment.newInstance(
                "Другие", otherNutrientIds, otherNutrientNames, new ArrayList<>(selectedNutrients));
        
        adapter.addFragment(vitaminsFragment, "ВИТАМИНЫ");
        adapter.addFragment(mineralsFragment, "МИНЕРАЛЫ");
        adapter.addFragment(otherFragment, "ДРУГИЕ");
        
        viewPager.setAdapter(adapter);
        

        viewPager.getLayoutParams().height = getResources().getDisplayMetrics().heightPixels / 3;
        

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(adapter.getTitle(position));
        }).attach();
        

        Button saveButton = view.findViewById(R.id.save_button);
        Button cancelButton = view.findViewById(R.id.cancel_button);
        
        saveButton.setOnClickListener(v -> {
            saveSelectedNutrients();
            dismiss();
        });
        
        cancelButton.setOnClickListener(v -> dismiss());
    }


    @Override
    public void onStart() {
        super.onStart();

        if (getDialog() != null) {
            View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                bottomSheet.setBackgroundResource(android.R.color.transparent);
            }
        }
    }
    

    private void initializeNutrientLists() {

        vitaminIds.addAll(Arrays.asList(
            "vitamin_a", "vitamin_b1", "vitamin_b2", "vitamin_b3", "vitamin_b5", 
            "vitamin_b6", "vitamin_b9", "vitamin_b12", "vitamin_c", "vitamin_d", 
            "vitamin_e", "vitamin_k"
        ));
        vitaminNames.addAll(Arrays.asList(
            "Витамин A", "Витамин B1", "Витамин B2", "Витамин B3", "Витамин B5", 
            "Витамин B6", "Витамин B9", "Витамин B12", "Витамин C", "Витамин D", 
            "Витамин E", "Витамин K"
        ));
        

        mineralIds.addAll(Arrays.asList(
            "calcium", "iron", "magnesium", "phosphorus", "potassium", "sodium", "zinc"
        ));
        mineralNames.addAll(Arrays.asList(
            "Кальций", "Железо", "Магний", "Фосфор", "Калий", "Натрий", "Цинк"
        ));
        

        otherNutrientIds.addAll(Arrays.asList(
            "fiber", "sugar", "cholesterol", "saturated_fats", "trans_fats"
        ));
        otherNutrientNames.addAll(Arrays.asList(
            "Клетчатка", "Сахар", "Холестерин", "Насыщенные жиры", "Трансжиры"
        ));
    }
    

    @Override
    public boolean onNutrientSelectionChanged(String nutrientId, boolean isChecked) {
        if (isChecked) {

            if (selectedNutrients.size() >= MAX_SELECTIONS) {
                Toast.makeText(requireContext(), 
                    "Можно выбрать не более " + MAX_SELECTIONS + " нутриентов", 
                    Toast.LENGTH_SHORT).show();
                return false;
            }
            
            selectedNutrients.add(nutrientId);
        } else {
            selectedNutrients.remove(nutrientId);
        }
        
        return true;
    }
    

    private void saveSelectedNutrients() {

        

        prefs.edit().putStringSet(TRACKED_NUTRIENTS_KEY, selectedNutrients).apply();
        

        List<String> selectedNames = new ArrayList<>();
        for (String id : selectedNutrients) {
            int vIndex = vitaminIds.indexOf(id);
            if (vIndex != -1) {
                selectedNames.add(vitaminNames.get(vIndex));
                continue;
            }
            
            int mIndex = mineralIds.indexOf(id);
            if (mIndex != -1) {
                selectedNames.add(mineralNames.get(mIndex));
                continue;
            }
            
            int oIndex = otherNutrientIds.indexOf(id);
            if (oIndex != -1) {
                selectedNames.add(otherNutrientNames.get(oIndex));
            }
        }
        

        EventBus.getDefault().post(new TrackedNutrientsChangedEvent(new ArrayList<>(selectedNutrients)));
        

        String message;
        if (selectedNutrients.isEmpty()) {
            message = "Отслеживание нутриентов отключено";
        } else {
            message = "Отслеживаемые нутриенты: " + String.join(", ", selectedNames);
        }
        

        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
    

    public static List<String> getTrackedNutrients(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> nutrients = prefs.getStringSet(TRACKED_NUTRIENTS_KEY, new HashSet<>());
        

        return new ArrayList<>(nutrients);
    }
} 