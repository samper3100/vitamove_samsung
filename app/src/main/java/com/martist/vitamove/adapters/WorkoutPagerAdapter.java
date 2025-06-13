package com.martist.vitamove.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.martist.vitamove.fragments.WorkoutFragment;
import com.martist.vitamove.fragments.workout.ActiveWorkoutFragment;
import com.martist.vitamove.fragments.workout.HistoryFragment;
import com.martist.vitamove.fragments.workout.ProgramsFragment;

public class WorkoutPagerAdapter extends FragmentStateAdapter {

    public WorkoutPagerAdapter(@NonNull WorkoutFragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new HistoryFragment();
            case 1:
                return new ActiveWorkoutFragment();
            case 2:
                return new ProgramsFragment();
            default:
                throw new IllegalStateException("Unexpected position " + position);
        }
    }

    @Override
    public int getItemCount() {
        return 3; 
    }
} 