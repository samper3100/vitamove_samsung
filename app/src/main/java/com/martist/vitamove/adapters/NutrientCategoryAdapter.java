package com.martist.vitamove.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.martist.vitamove.fragments.NutrientCategoryFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class NutrientCategoryAdapter extends FragmentStateAdapter {
    
    private final List<NutrientCategoryFragment> fragments = new ArrayList<>();
    private final List<String> titles = new ArrayList<>();
    
    public NutrientCategoryAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }
    
    public NutrientCategoryAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }
    
    public NutrientCategoryAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }
    

    public void addFragment(NutrientCategoryFragment fragment, String title) {
        fragments.add(fragment);
        titles.add(title);
    }
    
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments.get(position);
    }
    
    @Override
    public int getItemCount() {
        return fragments.size();
    }
    

    public String getTitle(int position) {
        return titles.get(position);
    }
    

    public void updateSelectedNutrients(Set<String> selectedNutrients) {
        for (NutrientCategoryFragment fragment : fragments) {
            if (fragment.isAdded()) {
                fragment.updateSelectedNutrients(selectedNutrients);
            }
        }
    }
    

    public List<NutrientCategoryFragment> getFragments() {
        return fragments;
    }
} 