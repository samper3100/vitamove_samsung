package com.martist.vitamove.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;
import com.martist.vitamove.adapters.FoodAdapter;
import com.martist.vitamove.managers.FoodManager;
import com.martist.vitamove.models.Food;
import com.martist.vitamove.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class FoodSelectionActivity extends BaseActivity {
    private static final String TAG = "FoodSelection";
    private FoodAdapter adapter;
    private ActivityResultLauncher<Intent> portionSizeLauncher;
    private ActivityResultLauncher<Intent> barcodeScanLauncher;
    private SearchView searchView;
    private FoodManager foodManager;
    private RecyclerView foodsList;
    private List<Food> allFoods;
    private String mealType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_selection);
        
        mealType = getIntent().getStringExtra(Constants.EXTRA_MEAL_TYPE);
        
        
        if (mealType == null) {
            Log.e(TAG, "mealType is null! Intent extras: " + getIntent().getExtras());
            Toast.makeText(this, "Ошибка: тип приема пищи не указан", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        
        
        View barcodeButton = findViewById(R.id.barcode_scan_button);
        barcodeButton.setOnClickListener(v -> {
            
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100);
                
                Intent intent = new Intent(FoodSelectionActivity.this, BarcodeScannerActivity.class);
                
                intent.putExtra(Constants.EXTRA_MEAL_TYPE, mealType);
                barcodeScanLauncher.launch(intent);
            }).start();
        });
        
        try {
            foodManager = FoodManager.getInstance(this);
            
            setupPortionSizeLauncher();
            setupBarcodeScanLauncher();
            setupRecyclerView();
            setupSearchView();
            
            
            loadAllFoods();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing FoodSelectionActivity: " + e.getMessage());
            Toast.makeText(this, "Ошибка при инициализации", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupSearchView() {
        searchView = findViewById(R.id.search_edit);
        searchView.setIconifiedByDefault(false);
        searchView.setQueryHint("Поиск продуктов");
        
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    
                    showPopularFoods();
                } else {
                    
                    performSearch(newText);
                }
                return true;
            }
        });
    }

    private void setupRecyclerView() {
        foodsList = findViewById(R.id.food_recycler);
        foodsList.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new FoodAdapter(new ArrayList<>(), food -> {
            Intent intent = new Intent(this, PortionSizeActivity.class);
            intent.putExtra(Constants.EXTRA_FOOD, food);
            intent.putExtra(Constants.EXTRA_MEAL_TYPE, mealType);
            
            String selectedDateStr = foodManager.getSelectedDateFormatted();
            intent.putExtra(Constants.EXTRA_SELECTED_DATE, selectedDateStr);
            portionSizeLauncher.launch(intent);
        });
        
        foodsList.setAdapter(adapter);
    }

    
    private void loadAllFoods() {
        new Thread(() -> {
            try {
                
                allFoods = foodManager.getAllFoods();
                
                
                if (allFoods != null && !allFoods.isEmpty()) {
                    
                    int count = Math.min(allFoods.size(), 10);
                    for (int i = 0; i < count; i++) {
                        Food food = allFoods.get(i);
                        
                    }
                    
                    
                    
                    int kolaCount = 0;
                    for (Food food : allFoods) {
                        if (food.getName().toLowerCase().contains("кола")) {
                            
                            kolaCount++;
                        }
                    }
                    
                }
                
                
                showPopularFoods();
                
                
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при загрузке продуктов: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this, "Ошибка при загрузке данных", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    
    private void showPopularFoods() {
        if (allFoods == null || allFoods.isEmpty()) {
            return;
        }
        
        
        List<Food> popularFoods = new ArrayList<>(allFoods);
        popularFoods.sort((food1, food2) -> {
            
            Integer p1 = food1.getPopularity();
            Integer p2 = food2.getPopularity();
            
            
            if (p1 == null && p2 == null) {
                return 0;
            }
            
            
            if (p1 == null) {
                return 1;
            }
            
            
            if (p2 == null) {
                return -1;
            }
            
            
            return Integer.compare(p2, p1);
        });
        
        
        if (popularFoods.size() > 10) {
            popularFoods = popularFoods.subList(0, 10);
        }
        
        updateFoodList(popularFoods);
        
        
        runOnUiThread(() -> {
            findViewById(R.id.empty_results_text).setVisibility(View.GONE);
            foodsList.setVisibility(View.VISIBLE);
        });
    }

    
    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            
            adapter.updateFoods(foodManager.getPopularFoods());
            return;
        }

        

        
        List<Food> localResults = foodManager.searchFoods(query);
        if (!localResults.isEmpty()) {
            
            updateFoodList(localResults);
            
            
            runOnUiThread(() -> {
                findViewById(R.id.empty_results_text).setVisibility(View.GONE);
            });
            return;
        }

        
        
        runOnUiThread(() -> {
            
            adapter.updateFoods(new ArrayList<>());
            findViewById(R.id.empty_results_text).setVisibility(View.VISIBLE);
            
            
            Toast.makeText(FoodSelectionActivity.this, 
                "Продукт не найден. Воспользуйтесь сканером штрих-кода или добавьте продукт вручную", 
                Toast.LENGTH_LONG).show();
        });
    }

    private void setupPortionSizeLauncher() {
        portionSizeLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                
            });
    }

    private void setupBarcodeScanLauncher() {
        barcodeScanLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.hasExtra(Constants.EXTRA_FOOD)) {
                        Food scannedFood = data.getParcelableExtra(Constants.EXTRA_FOOD);
                        if (scannedFood != null) {
                            
                            Intent intent = new Intent(this, PortionSizeActivity.class);
                            intent.putExtra(Constants.EXTRA_FOOD, scannedFood);
                            intent.putExtra(Constants.EXTRA_MEAL_TYPE, mealType);
                            
                            String selectedDateStr = foodManager.getSelectedDateFormatted();
                            intent.putExtra(Constants.EXTRA_SELECTED_DATE, selectedDateStr);
                            portionSizeLauncher.launch(intent);
                        }
                    }
                }
            });
    }

    private void updateFoodList(List<Food> foodList) {
        
        runOnUiThread(() -> {
            adapter.updateFoods(foodList);
            
            foodsList.scrollToPosition(0);
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
} 