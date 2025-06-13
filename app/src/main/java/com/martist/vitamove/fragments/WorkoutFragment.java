package com.martist.vitamove.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.tabs.TabLayout;
import com.martist.vitamove.R;
import com.martist.vitamove.events.WorkoutStartedEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class WorkoutFragment extends Fragment {
    private static final String TAG = "WorkoutFragment";
    private TabLayout tabLayout;
    private NavController navController;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
            
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_workout, container, false);

        tabLayout = view.findViewById(R.id.workout_tab_layout);

        NavHostFragment navHostFragment = (NavHostFragment) getChildFragmentManager().findFragmentById(R.id.workout_nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            setupTabLayout();
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        
        if (getActivity() != null && getActivity().getWindow() != null) {
            getActivity().getWindow().setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.background_color));
            int currentNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            int flags = getActivity().getWindow().getDecorView().getSystemUiVisibility();
            if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_NO) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            getActivity().getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    private void setupTabLayout() {
        
        
        
        

        if (tabLayout.getTabCount() == 0) { 
            tabLayout.addTab(tabLayout.newTab().setText("История"));
            tabLayout.addTab(tabLayout.newTab().setText("Тренировка"));
            tabLayout.addTab(tabLayout.newTab().setText("Программа"));
        }


        
        
        
        

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                
                if (navController == null) return;

                
                
                int currentDestinationId = navController.getCurrentDestination() != null ? navController.getCurrentDestination().getId() : -1;

                switch (tab.getPosition()) {
                    case 0: 
                        if (currentDestinationId != R.id.historyFragment) {
                            navController.navigate(R.id.action_global_to_historyFragment);
                        }
                        break;
                    case 1: 
                        if (currentDestinationId != R.id.activeWorkoutFragment) {
                            navController.navigate(R.id.action_global_to_activeWorkoutFragment);
                        }
                        break;
                    case 2: 
                        if (currentDestinationId != R.id.programsFragment) {
                            navController.navigate(R.id.action_global_to_programsFragment);
                        }
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                
                
            }
        });

        
        
        
         navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            
            
            if (destination.getId() == R.id.historyFragment) {
                if (tabLayout.getSelectedTabPosition() != 0) tabLayout.selectTab(tabLayout.getTabAt(0));
            } else if (destination.getId() == R.id.activeWorkoutFragment) {
                if (tabLayout.getSelectedTabPosition() != 1) tabLayout.selectTab(tabLayout.getTabAt(1));
            } else if (destination.getId() == R.id.programsFragment) {
                 if (tabLayout.getSelectedTabPosition() != 2) tabLayout.selectTab(tabLayout.getTabAt(2));
            }
             
            else if (destination.getId() == R.id.programDetailsFragment) {
                if (tabLayout.getSelectedTabPosition() != 2) tabLayout.selectTab(tabLayout.getTabAt(2), false); 
            }
        });


        
        
        
        int startDestinationId = navController.getGraph().getStartDestinationId();

        if (navController.getCurrentDestination() == null || navController.getCurrentDestination().getId() != startDestinationId) {
             
             
             
             
        }
        
        if (tabLayout.getTabCount() > 1 && tabLayout.getSelectedTabPosition() != 1) {
             
             
             
        }

    }

    
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWorkoutStartedEvent(WorkoutStartedEvent event) {
        
        if (tabLayout != null && tabLayout.getTabCount() > 1) {
            TabLayout.Tab workoutTab = tabLayout.getTabAt(1); 
            if (workoutTab != null) {
                workoutTab.select();
            }
        }
        
        
        if (navController != null && 
            (navController.getCurrentDestination() == null || 
             navController.getCurrentDestination().getId() != R.id.activeWorkoutFragment)) {
            try {
                navController.navigate(R.id.action_global_to_activeWorkoutFragment);
                
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при навигации к activeWorkoutFragment: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
            
        }
    }
}
