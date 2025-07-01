package com.martist.vitamove.fragments.workout;

import static com.martist.vitamove.VitaMoveApplication.context;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.martist.vitamove.R;
import com.martist.vitamove.VitaMoveApplication;
import com.martist.vitamove.adapters.ProgramAdapter;
import com.martist.vitamove.adapters.ProgramDayAdapter;
import com.martist.vitamove.utils.AsyncCallback;
import com.martist.vitamove.utils.Constants;
import com.martist.vitamove.utils.SupabaseCallback;
import com.martist.vitamove.utils.SupabaseClient;
import com.martist.vitamove.workout.data.managers.ProgramManager;
import com.martist.vitamove.workout.data.models.ProgramDay;
import com.martist.vitamove.workout.data.models.ProgramType;
import com.martist.vitamove.workout.data.models.WorkoutPlan;
import com.martist.vitamove.workout.data.models.WorkoutProgram;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


public class ProgramsFragment extends Fragment implements ProgramAdapter.OnProgramClickListener {
    private static final String TAG = "ProgramsFragment";

    private RecyclerView programsRecyclerView;
    private ProgramAdapter programAdapter;
    private ChipGroup filterChipGroup;
    private View emptyView;
    private ProgressBar progressBar;
    private View activeWorkoutCard;
    private ProgramManager programManager;
    private com.google.android.material.textfield.TextInputLayout searchLayout;
    
    
    private RecyclerView userProgramsRecyclerView;
    private ProgramAdapter userProgramAdapter;
    private TextView userProgramsTitle;
    
    
    private WorkoutProgram activeProgram;
    
    
    private List<WorkoutProgram> allPrograms = new ArrayList<>();
    
    private List<WorkoutProgram> userPrograms = new ArrayList<>();
    
    private List<WorkoutProgram> recommendedPrograms = new ArrayList<>();
    
    private RecyclerView activeProgramDaysRecyclerView;
    private ProgramDayAdapter programDayAdapter;
    private TextView activeProgramDaysTitle;
    
    private ExecutorService executorService;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        
        Context context = requireContext();
        programManager = new ProgramManager(context);
        executorService = Executors.newSingleThreadExecutor();

    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_programs, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        
        initViews(view);
        
        
        if (emptyView != null) {
            





            
            
            ImageView emptyIcon = emptyView.findViewById(android.R.id.icon);
            if (emptyIcon == null) {
                
                if (emptyView instanceof ViewGroup) {
                    ViewGroup emptyViewGroup = (ViewGroup) emptyView;
                    for (int i = 0; i < emptyViewGroup.getChildCount(); i++) {
                        View child = emptyViewGroup.getChildAt(i);
                        if (child instanceof ImageView) {
                            emptyIcon = (ImageView) child;
                            break;
                        }
                    }
                }
            }
            
            
            if (emptyIcon != null) {
                emptyIcon.setVisibility(View.GONE);
                
            }
        }
        
        setupRecyclerView();
        setupActiveProgramDaysRecyclerView();

        
        
        loadPrograms();
    }
    
    
    private void initViews(View view) {
        
        programsRecyclerView = view.findViewById(R.id.programs_recycler_view);

        emptyView = view.findViewById(R.id.empty_view);
        progressBar = view.findViewById(R.id.progress_bar);
        activeWorkoutCard = view.findViewById(R.id.active_program_card);
        searchLayout = view.findViewById(R.id.search_layout);
        activeProgramDaysRecyclerView = view.findViewById(R.id.active_program_days_recycler_view);
        activeProgramDaysTitle = view.findViewById(R.id.active_program_days_title);
        userProgramsRecyclerView = view.findViewById(R.id.user_programs_recycler_view);
        userProgramsTitle = view.findViewById(R.id.user_programs_title);

        if (activeWorkoutCard == null) {
            Log.e(TAG, "activeWorkoutCard не найден в разметке!");
        }
        
        if (searchLayout == null) {
            Log.e(TAG, "searchLayout не найден в разметке!");
        }
        
        
        FloatingActionButton fab = view.findViewById(R.id.fab_create_program);
        fab.setOnClickListener(v -> createNewProgram());
        
        
        setupSearchField(view);
    }
    
    
    private void setupSearchField(View view) {
        TextInputEditText searchEditText = view.findViewById(R.id.search_edit_text);
        if (searchEditText != null) {
            
            searchEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    
                    searchPrograms(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {
                    
                }
            });
            
            
            searchEditText.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    searchPrograms(v.getText().toString());
                    return true;
                }
                return false;
            });
        }
    }
    
    
    private void searchPrograms(String query) {
        if (allPrograms == null || allPrograms.isEmpty()) {
            return;
        }
        
        if (query == null || query.trim().isEmpty()) {
            
            userProgramAdapter.updatePrograms(userPrograms);
            programAdapter.updatePrograms(recommendedPrograms);
            
            
            updateUserProgramsVisibility();
            return;
        }
        
        String searchQuery = query.toLowerCase().trim();
        
        

        
        
        List<WorkoutProgram> filteredUserPrograms = userPrograms.stream()
            .filter(program -> {
                return program.getName().toLowerCase().contains(searchQuery) ||
                       (program.getDescription() != null && program.getDescription().toLowerCase().contains(searchQuery)) ||
                       (program.getGoal() != null && program.getGoal().toLowerCase().contains(searchQuery));
            })
            .collect(Collectors.toList());

        
        List<WorkoutProgram> filteredRecommendedPrograms = recommendedPrograms.stream()
            .filter(program -> {
                
                boolean matchesSearch = program.getName().toLowerCase().contains(searchQuery) ||
                                      (program.getDescription() != null && program.getDescription().toLowerCase().contains(searchQuery)) ||
                                      (program.getGoal() != null && program.getGoal().toLowerCase().contains(searchQuery));
                
                



                

                return matchesSearch;
            })
            .collect(Collectors.toList());
        
        
        userProgramAdapter.updatePrograms(filteredUserPrograms);
        programAdapter.updatePrograms(filteredRecommendedPrograms);
        
        
        if (filteredUserPrograms.isEmpty()) {
            userProgramsTitle.setVisibility(View.GONE);
            userProgramsRecyclerView.setVisibility(View.GONE);
        } else {
            userProgramsTitle.setVisibility(View.VISIBLE);
            userProgramsRecyclerView.setVisibility(View.VISIBLE);
        }
        
        
        if (filteredUserPrograms.isEmpty() && filteredRecommendedPrograms.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);




        } else {
            emptyView.setVisibility(View.GONE);
        }
    }
    
    
    private void updateUserProgramsVisibility() {
        
        if (activeProgram != null) {
            if (userProgramsTitle != null) {
                userProgramsTitle.setVisibility(View.GONE);
            }
            if (userProgramsRecyclerView != null) {
                userProgramsRecyclerView.setVisibility(View.GONE);
            }
            return;
        }
        
        
        if (userPrograms.isEmpty()) {
            if (userProgramsTitle != null) {
                userProgramsTitle.setVisibility(View.GONE);
            }
            if (userProgramsRecyclerView != null) {
                userProgramsRecyclerView.setVisibility(View.GONE);
            }
        } else {
            if (userProgramsTitle != null) {
                userProgramsTitle.setVisibility(View.VISIBLE);
            }
            if (userProgramsRecyclerView != null) {
                userProgramsRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }
    
    














    
    








































    
    
    private void setupRecyclerView() {
        allPrograms = new ArrayList<>();
        userPrograms = new ArrayList<>();
        recommendedPrograms = new ArrayList<>();

        
        programAdapter = new ProgramAdapter(recommendedPrograms, this, requireContext());
        programsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        programsRecyclerView.setAdapter(programAdapter);

        
        userProgramAdapter = new ProgramAdapter(userPrograms, this, requireContext());
        userProgramsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        userProgramsRecyclerView.setAdapter(userProgramAdapter);
    }
    
    
    private List<WorkoutProgram> filterProgramsByGoal(String goal) {
        if (goal == null || goal.isEmpty()) {
            return allPrograms;
        }
        
        return allPrograms.stream()
            .filter(program -> program.getGoal().equals(goal))
            .collect(Collectors.toList());
    }
    
    
    public void loadPrograms(boolean loadActiveProgram) {
        showLoading(true);
        
        
        
        
        allPrograms.clear();
        userPrograms.clear();
        recommendedPrograms.clear();
        programAdapter.updatePrograms(new ArrayList<>());
        userProgramAdapter.updatePrograms(new ArrayList<>());
        
        programManager.getAllProgramsAsync(new AsyncCallback<List<WorkoutProgram>>() {
            @Override
            public void onSuccess(List<WorkoutProgram> result) {
                
                
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        allPrograms = result;
                        
                        
                        separatePrograms(allPrograms);
                        
                        

                        
                        
                        showLoading(false);
                        
                        
                        updateEmptyView();
                    });
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Ошибка при загрузке программ: " + e.getMessage(), e);
                
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        
                        Toast.makeText(requireContext(), 
                            "Ошибка при загрузке программ: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();

                        
                        showLoading(false);
                        
                        
                        updateEmptyView();
                    });
                }
            }
        });
        
        
        if (loadActiveProgram) {
            loadActiveProgram();
        }
    }
    
    
    private void separatePrograms(List<WorkoutProgram> programs) {
        userPrograms.clear();
        recommendedPrograms.clear();
        
        
        String userId = null;
        if (context != null) {
            userId = ((VitaMoveApplication) context.getApplicationContext()).getCurrentUserId();
        }
        final String currentUserId = userId;
        
        
        
        Set<String> addedProgramIds = new HashSet<>();
        
        for (WorkoutProgram program : programs) {
            
            if (program.getId() != null && !addedProgramIds.add(program.getId())) {
                continue;
            }
            
            if (program.getType() == ProgramType.USER_CREATED ||
                (currentUserId != null && currentUserId.equals(program.getUserId()))) {
                userPrograms.add(program);
            } else {
                recommendedPrograms.add(program);
            }
        }
        
        
        userProgramAdapter.updatePrograms(userPrograms);
        programAdapter.updatePrograms(recommendedPrograms);
        
        
        
        updateUserProgramsVisibility();
    }
    
    
    public void loadPrograms() {
        loadPrograms(true);
    }
    
    
    private void loadActiveProgram() {
        
        showLoading(true);
        programManager.getActiveProgramAsync(new AsyncCallback<WorkoutProgram>() {
            @Override
            public void onSuccess(WorkoutProgram program) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    showLoading(false);
                    activeProgram = program;
                    if (activeProgram != null) {
                        
                        updateActiveWorkoutCard();
                        
                        
                        SharedPreferences prefs = requireContext().getSharedPreferences("vitamove_user_prefs", Context.MODE_PRIVATE);
                        String programDataKey = "program_data_loaded_" + activeProgram.getId();
                        boolean isProgramDataLoaded = prefs.getBoolean(programDataKey, false);
                        
                        if (!isProgramDataLoaded) {
                            
                            
                            
                            programManager.fetchAndCacheFullProgramAsync(activeProgram.getId(), new AsyncCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    
                                    
                                    prefs.edit().putBoolean(programDataKey, true).apply();
                                    loadAndDisplayActiveProgramDetails();
                                }
                                
                                @Override
                                public void onFailure(Exception e) {
                                    Log.e(TAG, "Ошибка при обновлении данных программы с сервера: " + e.getMessage());
                                    
                                    loadAndDisplayActiveProgramDetails();
                                }
                            });
                        } else {
                            
                            
                            loadAndDisplayActiveProgramDetails();
                        }
                    } else {
                        
                        activeWorkoutCard.setVisibility(View.GONE);
                        hideActiveProgramDays(); 
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    showLoading(false);
                    Log.e(TAG, "Ошибка при загрузке активной программы: " + e.getMessage(), e);
                    activeWorkoutCard.setVisibility(View.GONE);
                    hideActiveProgramDays(); 
                    showError("Не удалось загрузить активную программу");
                });
            }
        });
    }
    
    
    private void loadAndDisplayActiveProgramDetails() {
        if (activeProgram == null || activeProgram.getId() == null) {
            hideActiveProgramDays();
            Log.e(TAG, "loadAndDisplayActiveProgramDetails: Не удалось загрузить детали - activeProgram пустой или без ID");
            return;
        }

        final String programId = activeProgram.getId();
        
        showLoading(true); 

        
        com.martist.vitamove.workout.data.cache.ProgramRoomCache.getProgramAsync(programId, new AsyncCallback<com.martist.vitamove.workout.data.models.Program>() {
            @Override
            public void onSuccess(com.martist.vitamove.workout.data.models.Program cachedProgram) {
                
                List<ProgramDay> programDays = cachedProgram.getDays();

                if (programDays != null && !programDays.isEmpty()) {
                     
                     
                     fetchAndCombineWorkoutPlans(programId, programDays);
                    
                     
                     if (activeProgram != null) {
                         fetchAndUpdateProgramProgress(activeProgram);
                     }
                } else {
                    
                    
                     if (getActivity() != null) {
                         getActivity().runOnUiThread(() -> {
                             hideActiveProgramDays();
                             showLoading(false);
                             
                             
                             
                             programManager.fetchAndCacheFullProgramAsync(programId, new AsyncCallback<Void>() {
                                 @Override
                                 public void onSuccess(Void result) {
                                     
                                     loadAndDisplayActiveProgramDetails(); 
                                 }
                                 
                                 @Override
                                 public void onFailure(Exception e) {
                                     Log.e(TAG, "Не удалось загрузить программу с сервера: " + e.getMessage());
                                 }
                             });
                             
                             
                             if (activeProgram != null) {
                                 fetchAndUpdateProgramProgress(activeProgram);
                             }
                         });
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "loadAndDisplayActiveProgramDetails: Ошибка получения программы из кэша Room для " + programId, e);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        
                        
                        programManager.fetchAndCacheFullProgramAsync(programId, new AsyncCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                
                                loadAndDisplayActiveProgramDetails(); 
                            }
                            
                            @Override
                            public void onFailure(Exception e) {
                                Log.e(TAG, "Не удалось загрузить программу с сервера после ошибки кэша: " + e.getMessage());
                                hideActiveProgramDays();
                                showLoading(false);
                            }
                        });
                        
                        
                        if (activeProgram != null) {
                            fetchAndUpdateProgramProgress(activeProgram);
                        }
                    });
                }
            }
        });
    }

    
    private void fetchAndCombineWorkoutPlans(String programId, List<ProgramDay> programDays) {
        
        programManager.getCachedWorkoutPlansAsync(programId, new AsyncCallback<List<WorkoutPlan>>() {
            @Override
            public void onSuccess(List<WorkoutPlan> workoutPlans) {
                
                combineAndDisplayDaysAndPlans(programDays, workoutPlans);
            }

            @Override
            public void onFailure(Exception error) {
                Log.e(TAG, "fetchAndCombineWorkoutPlans: Ошибка получения планов из кэша", error);
                
                combineAndDisplayDaysAndPlans(programDays, null);
            }
        });
    }

    
    private void combineAndDisplayDaysAndPlans(List<ProgramDay> programDays, @Nullable List<WorkoutPlan> workoutPlans) {
        
        
        Map<String, WorkoutPlan> planMap = new HashMap<>();
        
        
        if (workoutPlans != null) {
            
            for (WorkoutPlan plan : workoutPlans) {
                
                if (plan.getProgramDayId() != null) {
                    planMap.put(plan.getProgramDayId(), plan);
                }
            }
        } else {
            
        }

        
        for (ProgramDay day : programDays) {
            WorkoutPlan correspondingPlan = planMap.get(day.getId());
            if (correspondingPlan != null) {
                day.setPlannedTimestamp(correspondingPlan.getPlannedDate());
                
                
                
                
                
                if ("completed".equals(correspondingPlan.getStatus())) {
                    day.setCompleted(true);
                    day.setStatus("completed");
                    
                } else if ("skipped".equals(correspondingPlan.getStatus())) {
                    day.setStatus("skipped");
                    
                } else {
                    day.setStatus(correspondingPlan.getStatus());
                    
                }
                
                
            } else {
                 day.setPlannedTimestamp(0); 
                 day.setStatus("planned");  
                 
            }
        }

        
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (!programDays.isEmpty()) {
                    
                    
                    for (ProgramDay day : programDays) {
                        
                    }
                    programDayAdapter.updateDays(programDays);
                    activeProgramDaysTitle.setVisibility(View.VISIBLE);
                    activeProgramDaysRecyclerView.setVisibility(View.VISIBLE);
                } else {
                    
                    hideActiveProgramDays();
                }
                showLoading(false); 
            });
        }
    }

    
    private void hideActiveProgramDays() {
        if (activeProgramDaysTitle != null) activeProgramDaysTitle.setVisibility(View.GONE);
        if (activeProgramDaysRecyclerView != null) activeProgramDaysRecyclerView.setVisibility(View.GONE);
        if (programDayAdapter != null) programDayAdapter.updateDays(new ArrayList<>()); 
    }

    
    private List<ProgramDay> parseProgramDaysFromJson(JSONObject programJson) {
        List<ProgramDay> daysList = new ArrayList<>();
        try {
            if (programJson.has("days")) {
                JSONArray daysArray = programJson.getJSONArray("days");
                for (int i = 0; i < daysArray.length(); i++) {
                    JSONObject dayJson = daysArray.getJSONObject(i);
                    ProgramDay day = new ProgramDay();
                    
                    day.setId(dayJson.optString("id"));
                    day.setProgramId(dayJson.optString("program_id", activeProgram.getId())); 
                    day.setDayNumber(dayJson.optInt("day_number"));
                    day.setName(dayJson.optString("name", "День " + day.getDayNumber()));
                    day.setDescription(dayJson.optString("description"));
                    
                    
                    
                    
                    if (dayJson.has("exercises")) {
                         JSONArray exercisesArray = dayJson.getJSONArray("exercises");
                         
                         
                         
                    } else {
                        
                    }

                    daysList.add(day);
                }
                 
                daysList.sort(Comparator.comparingInt(ProgramDay::getDayNumber));
                return daysList;
            } else {
                
                return null;
            }
        } catch (JSONException e) {
            Log.e(TAG, "Ошибка парсинга дней программы из JSON", e);
            return null;
        }
    }
    
    
    private void updateActiveWorkoutCard() {
        
        
        if (activeWorkoutCard == null) {
            Log.e(TAG, "activeWorkoutCard is null, не удается обновить UI");
            return;
        }
        
        try {
            
            View filtersContainer = null;
            RecyclerView programsRecyclerView = null;
            TextView programsTitle = null;
            com.google.android.material.textfield.TextInputLayout searchLayoutRef = null;

            try {

                programsRecyclerView = requireView().findViewById(R.id.programs_recycler_view);
                
                searchLayoutRef = requireView().findViewById(R.id.search_layout);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при получении ссылок на UI компоненты: " + e.getMessage(), e);
            }
            
            if (activeProgram != null) {
                




                
                
                if (searchLayoutRef != null) {
                    searchLayoutRef.setVisibility(View.GONE);
                    
                }
                
                
                if (userProgramsTitle != null) {
                    userProgramsTitle.setVisibility(View.GONE);
                    
                }
                
                if (userProgramsRecyclerView != null) {
                    userProgramsRecyclerView.setVisibility(View.GONE);
                    
                }
                






                
                
                FloatingActionButton fab = requireView().findViewById(R.id.fab_create_program);
                if (fab != null) {
                    fab.setVisibility(View.GONE);
                    
                }
                
                
                TextView programNameTextView = activeWorkoutCard.findViewById(R.id.active_program_name);
                TextView programProgressTextView = activeWorkoutCard.findViewById(R.id.active_program_progress_text);
                
                if (programNameTextView != null) {
                    programNameTextView.setText(activeProgram.getName());
                }
                
                
                TextView programLevelTextView = activeWorkoutCard.findViewById(R.id.active_program_level);
                if (programLevelTextView != null) {
                    programLevelTextView.setText(activeProgram.getLevel());
                }
                
                if (programProgressTextView != null) {
                    
                    fetchAndUpdateProgramProgress(activeProgram);
                }
                
                
                View continueButton = activeWorkoutCard.findViewById(R.id.active_program_continue_button);
                if (continueButton != null) {
                    continueButton.setOnClickListener(v -> continueProgramWorkout(activeProgram));
                }
                
                View changeButton = activeWorkoutCard.findViewById(R.id.active_program_change_button);
                if (changeButton != null) {
                    changeButton.setOnClickListener(v -> changeProgram(activeProgram));
                }
                
                View configButton = activeWorkoutCard.findViewById(R.id.active_program_config_button);
                if (configButton != null) {
                    configButton.setOnClickListener(v -> Toast.makeText(context,"Функионал в разработке",Toast.LENGTH_LONG).show());
                }
                
                
                View infoIcon = activeWorkoutCard.findViewById(R.id.active_program_info);
                if (infoIcon != null) {
                    infoIcon.setOnClickListener(v -> showActiveProgramInfo(activeProgram));
                }
                
                
                if (programNameTextView != null) {
                    programNameTextView.setOnClickListener(v -> showActiveProgramInfo(activeProgram));
                }
                
                
                View headerContainer = activeWorkoutCard.findViewById(R.id.active_program_header);
                if (headerContainer == null) {
                    
                    
                    LinearLayout linearLayout = (LinearLayout) ((ViewGroup) activeWorkoutCard).getChildAt(0);
                    if (linearLayout != null && linearLayout.getChildCount() > 0) {
                        View firstChild = linearLayout.getChildAt(0);
                        if (firstChild instanceof LinearLayout) {
                            firstChild.setOnClickListener(v -> showActiveProgramInfo(activeProgram));
                        }
                    }
                } else {
                    headerContainer.setOnClickListener(v -> showActiveProgramInfo(activeProgram));
                }
                
                
                activeWorkoutCard.setVisibility(View.VISIBLE);
                
                
                
                
                
                if (filtersContainer != null) {
                    filtersContainer.setVisibility(View.GONE);
                    
                }
                
                if (programsRecyclerView != null) {
                    programsRecyclerView.setVisibility(View.GONE);
                    
                }
                
                
                updateEmptyView();
            } else {
                
                activeWorkoutCard.setVisibility(View.GONE);
                
                
                
                if (programsTitle != null) {
                    programsTitle.setVisibility(View.VISIBLE);
                }
                

                
                
                if (searchLayoutRef != null) {
                    searchLayoutRef.setVisibility(View.VISIBLE);
                    
                }
                
                
                if (userProgramsTitle != null && !userPrograms.isEmpty()) {
                    userProgramsTitle.setVisibility(View.VISIBLE);
                    
                }
                
                if (userProgramsRecyclerView != null && !userPrograms.isEmpty()) {
                    userProgramsRecyclerView.setVisibility(View.VISIBLE);
                    
                }
                
                
                if (filtersContainer != null) {
                    filtersContainer.setVisibility(View.VISIBLE);
                    
                }
                
                if (programsRecyclerView != null) {
                    programsRecyclerView.setVisibility(View.VISIBLE);
                    
                }
                






                
                
                FloatingActionButton fab = requireView().findViewById(R.id.fab_create_program);
                if (fab != null) {
                    fab.setVisibility(View.VISIBLE);
                    
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при обновлении карточки активной программы: " + e.getMessage(), e);
        }
    }
    







    
    












































































    
    
    private void updateEmptyView() {
        
        if (emptyView != null) {
            emptyView.setVisibility(View.GONE);
        }
    }
    
    
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    


    
    private void createNewProgram() {
        CreateWorkoutFragment createFragment = CreateWorkoutFragment.newInstance();
        requireActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, createFragment)
            .addToBackStack(null)
            .commit();
    }
    
    
    private void continueProgramWorkout(WorkoutProgram program) {
        if (program == null || program.getId() == null) {
            Log.e(TAG, "Невозможно продолжить тренировку: программа или её ID равны null");
            showError("Ошибка: программа не найдена");
            return;
        }
        
        
        showLoading(true);
        
        
        String programId = program.getId();
        
        
        executorService.execute(() -> {
            try {
                
                programManager.fetchAndCacheWorkoutPlansAsync(programId, new AsyncCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        
                        findAndStartNextWorkout(programId);
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Ошибка обновления кэша планов: " + e.getMessage(), e);
                        
                        findAndStartNextWorkout(programId);
                    }
                });
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showError("Ошибка при поиске планов тренировок: " + e.getMessage());
                    });
                }
            }
        });
    }
    
    
    private void findAndStartNextWorkout(String programId) {
        executorService.execute(() -> {
            try {
                List<WorkoutPlan> workoutPlans = programManager.getWorkoutPlansByProgramId(programId);
                
                if (workoutPlans == null || workoutPlans.isEmpty()) {
                    Log.e(TAG, "Планы тренировок не найдены для программы: " + programId);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showLoading(false);
                            showError("Не найдены тренировки в программе");
                        });
                    }
                    return;
                }
                
                
                
                
                for (WorkoutPlan plan : workoutPlans) {
                    if ("in_progress".equals(plan.getStatus())) {
                        
                        
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                showLoading(false);
                                navigateToActiveWorkout();
                            });
                        }
                        return; 
                    }
                }
                
                
                

                
                Collections.sort(workoutPlans, (p1, p2) -> Long.compare(p1.getPlannedDate(), p2.getPlannedDate()));
                
                WorkoutPlan planToPropose = null;
                boolean allPlansAreTrulyCompleted = true; 

                if (workoutPlans.isEmpty()) { 
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showLoading(false);
                            showError("В программе нет тренировок.");
                        });
                    }
                    return;
                }

                for (WorkoutPlan plan : workoutPlans) {
                    if (!"completed".equals(plan.getStatus())) {
                        allPlansAreTrulyCompleted = false; 
                        if (planToPropose == null && !"skipped".equals(plan.getStatus())) {
                            planToPropose = plan; 
                        }
                    }
                }

                if (allPlansAreTrulyCompleted) {
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(requireContext(), "Программа полностью завершена.", Toast.LENGTH_SHORT).show();
                        });
                    }
                    return; 
                }

                if (planToPropose == null) {
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(requireContext(), "Нет доступных тренировок для запуска.", Toast.LENGTH_SHORT).show();
                        });
                    }
                    return; 
                }
                
                
                
                
                final WorkoutPlan planToStartDialog = planToPropose;

                
                if (getActivity() != null) {
                    final WorkoutPlan finalNextPlan = planToStartDialog; 
                    getActivity().runOnUiThread(() -> {
                        new AlertDialog.Builder(requireContext())
                            .setTitle("Подтверждение")
                            .setMessage("Вы действительно хотите запустить тренировку '" + finalNextPlan.getName() + "' прямо сейчас?")
                            .setPositiveButton("Запустить", (dialog, which) -> {
                                
                                executorService.execute(() -> createAndStartWorkout(finalNextPlan, programId));
                            })
                            .setNegativeButton("Отмена", (dialog, which) -> {
                                
                                showLoading(false);
                                
                            })
                            .setOnCancelListener(dialog -> {
                                
                                showLoading(false);
                                
                            })
                            .show();
                    });
                } else {
                    
                    showLoading(false);
                    Log.e(TAG, "Activity is null, не могу показать диалог подтверждения.");
                }

            } catch (Exception e) {
                Log.e(TAG, "Ошибка при поиске следующего плана: " + e.getMessage(), e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showError("Ошибка при поиске следующего дня программы: " + e.getMessage());
                    });
                }
            }
        });
    }
    
    
    private void navigateToActiveWorkout() {
        try {
            
            
            com.martist.vitamove.events.WorkoutStartedEvent event = 
                new com.martist.vitamove.events.WorkoutStartedEvent(System.currentTimeMillis());
            org.greenrobot.eventbus.EventBus.getDefault().post(event);
            
            
            View workoutsTab = requireActivity().findViewById(R.id.nav_workouts);
            if (workoutsTab != null) {
                workoutsTab.performClick();
                
            } else {
                Log.e(TAG, "Не удалось найти кнопку навигации тренировок");
                
                if (requireActivity() instanceof com.martist.vitamove.activities.MainActivity) {
                    View view = requireActivity().findViewById(R.id.nav_workouts);
                    if (view != null) {
                        view.performClick();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при переходе к экрану активной тренировки: " + e.getMessage(), e);
            showError("Не удалось открыть экран тренировки");
        }
    }
    
    
    @Override
    public void onProgramClick(WorkoutProgram program) {
        
        navigateToProgramDetails(program.getId());
    }

    @Override
    public void onStartClick(WorkoutProgram program) {
        
        startProgram(program);
    }

    @Override
    public void onSetupClick(WorkoutProgram program) {
        
        navigateToProgramSetup(program.getId());
    }

    @Override
    public void onDetailsClick(WorkoutProgram program) {
        
        navigateToProgramDetails(program.getId());
    }

    
    private void navigateToProgramDetails(String programId) {
        
        requireActivity().getSharedPreferences("VitaMovePrefs", 0)
            .edit()
            .putInt("workout_tab_index", 2) 
            .apply();
            
        ProgramDetailsFragment detailsFragment = ProgramDetailsFragment.newInstance(programId);
        requireActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, detailsFragment)
            .addToBackStack(null)
            .commit();
    }

    
    private void navigateToProgramSetup(String programId) {
        ProgramSetupFragment setupFragment = ProgramSetupFragment.newInstance(programId);
        requireActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, setupFragment)
            .addToBackStack(null)
            .commit();
    }

    
    private void startProgram(WorkoutProgram program) {
        showLoading(true);
        
        
        if (emptyView != null) {
            emptyView.setVisibility(View.GONE);
            
            
            ImageView emptyIcon = findEmptyViewIcon();
            if (emptyIcon != null) {
                emptyIcon.setVisibility(View.GONE);
            }
        }
        
        long startDate = System.currentTimeMillis();
        
        programManager.startProgramAsync(program.getId(), startDate, new AsyncCallback<String>() {
            @Override
            public void onSuccess(String result) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (result != null && !result.isEmpty()) {
                            Toast.makeText(requireContext(), 
                                getString(R.string.program_started_success), 
                                Toast.LENGTH_SHORT).show();
                            
                            
                            programManager.fetchAndCacheWorkoutPlansAsync(program.getId(), new AsyncCallback<Void>() {
                                @Override
                                public void onSuccess(Void ignored) {
                                    
                                    
                                    
                                    programManager.getActiveProgramAsync(new AsyncCallback<WorkoutProgram>() {
                                        @Override
                                        public void onSuccess(WorkoutProgram activatedProgram) {
                                            if (activatedProgram != null) {
                                                
                                                activeProgram = activatedProgram;
                                                
                                                
                                                updateActiveWorkoutCard();
                                                
                                                
                                                loadAndDisplayActiveProgramDetails();
                                                
                                                
                                                RecyclerView programsRecyclerView = requireView().findViewById(R.id.programs_recycler_view);
                                                if (programsRecyclerView != null) {
                                                    programsRecyclerView.setVisibility(View.GONE);
                                                }
                                                
                                                showLoading(false);
                                            } else {
                                                
                                                showLoading(false);
                                                navigateToProgramDetails(program.getId());
                                            }
                                        }
                                        
                                        @Override
                                        public void onFailure(Exception e) {
                                            Log.e(TAG, "Не удалось загрузить активированную программу", e);
                                            showLoading(false);
                                            
                                            navigateToProgramDetails(program.getId());
                                        }
                                    });
                                }
                                
                                @Override
                                public void onFailure(Exception e) {
                                    Log.e(TAG, "Ошибка обновления кэша планов после старта программы", e);
                                    
                                    programManager.getActiveProgramAsync(new AsyncCallback<WorkoutProgram>() {
                                        @Override
                                        public void onSuccess(WorkoutProgram activatedProgram) {
                                            if (activatedProgram != null) {
                                                activeProgram = activatedProgram;
                                                updateActiveWorkoutCard();
                                                loadAndDisplayActiveProgramDetails();
                                                
                                                RecyclerView programsRecyclerView = requireView().findViewById(R.id.programs_recycler_view);
                                                if (programsRecyclerView != null) {
                                                    programsRecyclerView.setVisibility(View.GONE);
                                                }
                                                
                                                showLoading(false);
                                            } else {
                                                showLoading(false);
                                                navigateToProgramDetails(program.getId());
                                            }
                                        }
                                        
                                        @Override
                                        public void onFailure(Exception e) {
                                            Log.e(TAG, "Не удалось загрузить активированную программу", e);
                                            showLoading(false);
                                            navigateToProgramDetails(program.getId());
                                        }
                                    });
                                }
                            });
                        } else {
                            showLoading(false);
                            showError("Не удалось начать программу");
                        }
                    });
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showError("Ошибка при запуске программы: " + e.getMessage());
                    });
                }
            }
        });
    }


    
    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Ошибка: " + message);
        }
    }

    
    private void changeProgram(WorkoutProgram program) {
        if (program == null) {
            Log.e(TAG, "Попытка деактивировать null программу");
            Toast.makeText(requireContext(), "Ошибка: программа не найдена", Toast.LENGTH_SHORT).show();
            return;
        }
        
        
        
        
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.program_change_title)
                .setMessage(R.string.program_change_message)
                .setPositiveButton(R.string.program_change_confirm, (dialog, which) -> {
                    
                    showLoading(true);
                    
                    
                    String programId = program.getId();
                    
                    
                    
                    String userId = ((VitaMoveApplication) requireActivity().getApplication()).getCurrentUserId();
                    if (userId == null || userId.isEmpty()) {
                        Log.e(TAG, "Не удалось получить ID пользователя");
                        showError("Ошибка: не удалось определить пользователя");
                        showLoading(false);
                        return;
                    }
                    
                    try {
                        
                        SupabaseClient supabaseClient = SupabaseClient.getInstance(
                                Constants.SUPABASE_CLIENT_ID,
                                Constants.SUPABASE_CLIENT_SECRET
                        );
                        
                        
                        showLoading(true);
                        
                        
                        supabaseClient.rpc("clean_program_data")
                            .param("p_program_id", programId)
                            .param("p_user_id", userId)
                            .executeAsync(new SupabaseCallback<String>() {
                                @Override
                                public void onSuccess(String responseBody) {
                                    
                                    
                                    
                                    Activity activity = getActivity();
                                    if (activity != null) {
                                        activity.runOnUiThread(() -> {
                                            
                                            
                                            
                                            programManager.clearProgramCache();
                                            
                                            Toast.makeText(requireContext(), 
                                                    getString(R.string.program_deactivated), 
                                                    Toast.LENGTH_SHORT).show();
                                            
                                            
                                            activeProgram = null;
                                            
                                            
                                            updateActiveWorkoutCard();
                                            
                                            
                                            hideActiveProgramDays();
                                            
                                            
                                            try {

                                                RecyclerView programsRecyclerView = requireView().findViewById(R.id.programs_recycler_view);
                                                




                                                
                                                if (programsRecyclerView != null) {
                                                    programsRecyclerView.setVisibility(View.VISIBLE);
                                                    
                                                }
                                            } catch (Exception e) {
                                                Log.e(TAG, "Ошибка при отображении компонентов после смены программы: " + e.getMessage(), e);
                                            }
                                            
                                            
                                            loadPrograms(false);
                                            
                                            
                                            showLoading(false);
                                        });
                                    }
                                }
                                
                                @Override
                                public void onFailure(Exception e) {
                                    Log.e(TAG, "Ошибка при вызове RPC-функции clean_program_data: " + e.getMessage(), e);
                                    Activity activity = getActivity();
                                    if (activity != null) {
                                        activity.runOnUiThread(() -> {
                                            Log.e(TAG, "Ошибка при смене программы: " + e.getMessage(), e);
                                            
                                            
                                            activeProgram = null;
                                            updateActiveWorkoutCard();
                                            
                                            
                                            hideActiveProgramDays();
                                            
                                            
                                            try {

                                                RecyclerView programsRecyclerView = requireView().findViewById(R.id.programs_recycler_view);
                                                



                                                
                                                if (programsRecyclerView != null) {
                                                    programsRecyclerView.setVisibility(View.VISIBLE);
                                                }
                                            } catch (Exception ex) {
                                                Log.e(TAG, "Ошибка при отображении компонентов после ошибки: " + ex.getMessage(), ex);
                                            }
                                            
                                            
                                            loadPrograms(false);
                                            
                                            
                                            showError(getString(R.string.program_deactivation_error) + ": " + e.getMessage());
                                            
                                            
                                            showLoading(false);
                                        });
                                    }
                                }
                            });
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при запуске асинхронной задачи очистки программы: " + e.getMessage(), e);
                        Toast.makeText(requireContext(), getString(R.string.program_deactivation_error), Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    }
                })
                .setNegativeButton(R.string.program_change_cancel, null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        
        
        
        
        loadActiveProgram();
        
        
        if (activeProgram != null) {
            updateActiveWorkoutCard();
        }
    }
    
    
    private void refreshWorkoutPlansCache(String programId) {
        if (programId == null || programId.isEmpty()) {
            Log.e(TAG, "refreshWorkoutPlansCache: Program ID не может быть пустым");
            return;
        }
        
        
        
        
        
        programManager.fetchAndCacheWorkoutPlansAsync(programId, new AsyncCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                
                
                
                loadAndDisplayActiveProgramDetails();
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "refreshWorkoutPlansCache: Ошибка обновления кэша планов", e);
                
                
                loadAndDisplayActiveProgramDetails();
            }
        });
    }

    
    private void navigateToActiveProgramDetails() {
        if (activeProgram != null) {
            
            
            
            try {

                RecyclerView programsRecyclerView = requireView().findViewById(R.id.programs_recycler_view);
                com.google.android.material.textfield.TextInputLayout searchLayoutRef = requireView().findViewById(R.id.search_layout);
                
                if (searchLayoutRef != null) {
                    searchLayoutRef.setVisibility(View.GONE);
                    
                }




                if (programsRecyclerView != null) {
                    programsRecyclerView.setVisibility(View.GONE);
                    
                }
            } catch (Exception e) {
                Log.e(TAG, "(Навигация) Ошибка при скрытии элементов перед переходом: " + e.getMessage(), e);
            }
            

        } else {
            Log.e(TAG, "Попытка перехода к активной программе, но программа не загружена");
            showError("Активная программа не загружена");
        }
    }
    

    
    private void setupActiveProgramDaysRecyclerView() {
        programDayAdapter = new ProgramDayAdapter(new ArrayList<>(), programDay -> {
            
            
            navigateToProgramDayDetails(programDay.getId());
        });
        activeProgramDaysRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        activeProgramDaysRecyclerView.setAdapter(programDayAdapter);
        activeProgramDaysRecyclerView.setNestedScrollingEnabled(false); 
    }
    
    
    private void navigateToProgramDayDetails(String dayId) {
        if (dayId == null || dayId.isEmpty()) {
            showError("ID дня программы не найден");
            return;
        }
        
        try {
            
            ProgramDayDetailsFragment detailsFragment = ProgramDayDetailsFragment.newInstance(dayId);
            
            
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, detailsFragment)
                .addToBackStack(null)
                .commit();
                
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при переходе к деталям дня программы: " + e.getMessage(), e);
            showError("Не удалось открыть детали дня программы");
        }
    }

    
    private void showActiveProgramInfo(WorkoutProgram program) {
        if (program != null) {
            
            ActiveProgramInfoBottomSheet bottomSheet = ActiveProgramInfoBottomSheet.newInstance(program);
            bottomSheet.show(getChildFragmentManager(), "ActiveProgramInfoBottomSheet");
        } else {
            Log.e(TAG, "Невозможно показать информацию: программа равна null");
        }
    }

    
    private ImageView findEmptyViewIcon() {
        if (emptyView == null) return null;
        
        ImageView emptyIcon = emptyView.findViewById(android.R.id.icon);
        if (emptyIcon == null && emptyView instanceof ViewGroup) {
            
            ViewGroup emptyViewGroup = (ViewGroup) emptyView;
            for (int i = 0; i < emptyViewGroup.getChildCount(); i++) {
                View child = emptyViewGroup.getChildAt(i);
                if (child instanceof ImageView) {
                    return (ImageView) child;
                }
            }
        }
        return emptyIcon;
    }

    

















    
                                            @Override
                                            public void onDeleteClick(WorkoutProgram program) {
                                                if (program == null || program.getId() == null) {
                                                    showError("Невозможно удалить программу: отсутствует ID программы");
                                                    return;
                                                }

                                                
                                                String userId = ((VitaMoveApplication) requireContext().getApplicationContext()).getCurrentUserId();
                                                if (userId == null || userId.isEmpty()) {
                                                    showError("Невозможно удалить программу: не удалось определить ID пользователя");
                                                    return;
                                                }

                                                
                                                new AlertDialog.Builder(requireContext())
                                                    .setTitle("Удаление программы")
                                                    .setMessage("Вы действительно хотите удалить программу \"" + program.getName() + "\"? Это действие невозможно отменить.")
                                                    .setPositiveButton("Удалить", (dialog, which) -> {
                                                        
                                                        deleteUserProgram(program.getId(), userId);
                                                    })
                                                    .setNegativeButton("Отмена", null)
                                                    .show();
                                            }

                                            
                                            private void deleteUserProgram(String programTemplateId, String authorId) {
                                                
                                                showLoading(true);

                                                
                                                executorService.execute(() -> {
            try {
                
                SupabaseClient supabaseClient = SupabaseClient.getInstance(
                    Constants.SUPABASE_CLIENT_ID,
                    Constants.SUPABASE_CLIENT_SECRET
                );
                
                
                
                
                
                supabaseClient.from("program_templates")
                    .eq("id", programTemplateId)
                    .eq("author_id", authorId)
                    .delete()
                    .executeDelete();
                
                
                if (getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    showLoading(false);
                    
                    Toast.makeText(requireContext(), "Программа успешно удалена", Toast.LENGTH_SHORT).show();
                    
                    
                    loadPrograms();
                });
            } catch (Exception e) {
                if (getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    showLoading(false);
                    Log.e(TAG, "Ошибка при удалении программы: " + e.getMessage(), e);
                    showError("Не удалось удалить программу: " + e.getMessage());
                });
            }
        });
    }

    
    private void fetchAndUpdateProgramProgress(WorkoutProgram program) {
        if (program == null || program.getId() == null) {
            Log.e(TAG, "Невозможно обновить прогресс: программа или её ID равны null");
            return;
        }

        String programId = program.getId();
        
        
        
        TextView programProgressTextView = activeWorkoutCard.findViewById(R.id.active_program_progress_text);
        ProgressBar progressBar = activeWorkoutCard.findViewById(R.id.active_program_progress_bar);
        TextView nextDayTextView = activeWorkoutCard.findViewById(R.id.active_program_progress_week);
        
        
        programManager.fetchAndCacheWorkoutPlansAsync(programId, new AsyncCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                
                loadWorkoutPlansAndUpdateProgress(programId, programProgressTextView, progressBar, nextDayTextView);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Ошибка при обновлении кэша планов тренировок: " + e.getMessage(), e);
                
                loadWorkoutPlansAndUpdateProgress(programId, programProgressTextView, progressBar, nextDayTextView);
            }
        });
    }
    
    
    private void loadWorkoutPlansAndUpdateProgress(String programId, TextView programProgressTextView, 
                                               ProgressBar progressBar, TextView nextDayTextView) {
        
        executorService.execute(() -> {
            try {
                List<WorkoutPlan> workoutPlans = programManager.getWorkoutPlansByProgramId(programId);
                
                if (workoutPlans == null || workoutPlans.isEmpty()) {
                    
                    
                    updateProgressUI(0, programProgressTextView, progressBar, "Программа загружается...", nextDayTextView);
                    return;
                }
                
                
                
                
                int totalPlans = workoutPlans.size();
                int completedPlans = 0;
                
                
                WorkoutPlan nextDay = null;
                
                
                Collections.sort(workoutPlans, (p1, p2) -> Long.compare(p1.getPlannedDate(), p2.getPlannedDate()));
                
                for (WorkoutPlan plan : workoutPlans) {
                    if ("completed".equals(plan.getStatus())) {
                        completedPlans++;
                    } else if (nextDay == null && !"completed".equals(plan.getStatus()) && !"skipped".equals(plan.getStatus())) {
                        
                        nextDay = plan;
                    }
                }
                
                
                
                
                final float progressPercentage = totalPlans > 0 ? 
                        (float) completedPlans / totalPlans * 100 : 0;
                
                
                final String nextDayName;
                if (nextDay != null) {
                    nextDayName = nextDay.getName();
                    
                } else if (completedPlans == totalPlans && totalPlans > 0) {
                    nextDayName = null; 
                    
                } else {
                    nextDayName = "Следующая тренировка";
                    
                }
                
                
                updateProgressUI(progressPercentage, programProgressTextView, progressBar, nextDayName, nextDayTextView);
                
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при получении планов тренировок: " + e.getMessage(), e);
                
                updateProgressUI(0, programProgressTextView, progressBar, "Проверка прогресса...", nextDayTextView);
            }
        });
    }

    
    private void updateProgressUI(float progressPercentage, TextView progressTextView, ProgressBar progressBar, 
                            String nextDayName, TextView nextDayTextView) {
        
        if (Looper.myLooper() != Looper.getMainLooper()) {
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> 
                    updateProgressUI(progressPercentage, progressTextView, progressBar, nextDayName, nextDayTextView));
            }
            return;
        }
        
        
        if (progressTextView != null) {
            String progressText = String.format("%.0f%% завершено", progressPercentage);
            progressTextView.setText(progressText);
            
        }
        
        if (progressBar != null) {
            progressBar.setProgress((int)progressPercentage);
            
        }
        
        if (nextDayTextView != null) {
            if (nextDayName != null) {
                nextDayTextView.setText(nextDayName);
                
            } else {
                nextDayTextView.setText("Программа завершена");
                
            }
        }
    }

    private void createAndStartWorkout(WorkoutPlan planToStart, String programId) {
        try {
            
            com.martist.vitamove.workout.data.repository.WorkoutRepository workoutRepository = 
                ((VitaMoveApplication) requireActivity().getApplication()).getWorkoutRepository();
            
            
            String workoutId = workoutRepository.createWorkoutFromPlan(planToStart);
            
            if (workoutId != null && !workoutId.isEmpty()) {
                
                
                
                com.martist.vitamove.events.WorkoutStartedEvent event = 
                    new com.martist.vitamove.events.WorkoutStartedEvent(System.currentTimeMillis());
                org.greenrobot.eventbus.EventBus.getDefault().post(event);
                
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        navigateToActiveWorkout();
                    });
                }
            } else {
                Log.e(TAG, "Создание тренировки из плана вернуло null ID для программы: " + programId);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showError("Не удалось создать тренировку из плана");
                    });
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при создании тренировки из плана: " + planToStart.getName() + " для программы: " + programId + ": " + e.getMessage(), e);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    showLoading(false);
                    showError("Ошибка при создании тренировки: " + e.getMessage());
                });
            }
        }
    }
} 