package com.martist.vitamove.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;


public class Food implements Parcelable {
    private static final String TAG = "Food";
    

    static {
        try {


            System.setProperty("food.class.loading", "true");

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при инициализации класса Food: " + e.getMessage(), e);
        }
    }
    

    public static void ensureClassLoaded() {
        try {

            if (isClassLoaded()) {

                return;
            }
            

            Class<?> cls = Class.forName("com.martist.vitamove.models.Food");

            

            initCreator();
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "ClassNotFoundException при загрузке Food: " + e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при обеспечении загрузки класса Food: " + e.getMessage(), e);
        }
    }
    

    private static void initCreator() {
        try {
            if (CREATOR != null) {

                

                Food testFood = new Food.Builder()
                        .id(1)
                        .name("TestFood")
                        .calories(100)
                        .build();
                
                Parcel parcel = Parcel.obtain();
                testFood.writeToParcel(parcel, 0);
                parcel.setDataPosition(0);
                
                Food createdFood = CREATOR.createFromParcel(parcel);
                parcel.recycle();
                
                if (createdFood != null) {

                    System.setProperty("food.class.loaded", "true");
                }
            } else {

            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при инициализации CREATOR: " + e.getMessage(), e);
        }
    }
    

    public static boolean isClassLoaded() {
        return "true".equals(System.getProperty("food.class.loaded"));
    }
    
    private final long id;
    private final String name;
    private final String category;
    private final String subcategory;
    private final int calories;
    private final float proteins;
    private final float fats;
    private final float carbs;
    private final int popularity;
    private final float calcium;
    private final float iron;
    private final float magnesium;
    private final float phosphorus;
    private final float potassium;
    private final float sodium;
    private final float zinc;
    private final float vitaminA;
    private final float vitaminB1;
    private final float vitaminB2;
    private final float vitaminB3;
    private final float vitaminB5;
    private final float vitaminB6;
    private final float vitaminB9;
    private final float vitaminB12;
    private final float vitaminC;
    private final float vitaminD;
    private final float vitaminE;
    private final float vitaminK;
    private final float cholesterol;
    private final float saturatedFats;
    private final float transFats;
    private final float fiber;
    private final float sugar;
    private final int usefulness_index;
    private final String idUUID;
    private final boolean is_liquid;

    private Food(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.category = builder.category;
        this.subcategory = builder.subcategory;
        this.calories = builder.calories;
        this.proteins = builder.proteins;
        this.fats = builder.fats;
        this.carbs = builder.carbs;
        this.popularity = builder.popularity;
        this.calcium = builder.calcium;
        this.iron = builder.iron;
        this.magnesium = builder.magnesium;
        this.phosphorus = builder.phosphorus;
        this.potassium = builder.potassium;
        this.sodium = builder.sodium;
        this.zinc = builder.zinc;
        this.vitaminA = builder.vitaminA;
        this.vitaminB1 = builder.vitaminB1;
        this.vitaminB2 = builder.vitaminB2;
        this.vitaminB3 = builder.vitaminB3;
        this.vitaminB5 = builder.vitaminB5;
        this.vitaminB6 = builder.vitaminB6;
        this.vitaminB9 = builder.vitaminB9;
        this.vitaminB12 = builder.vitaminB12;
        this.vitaminC = builder.vitaminC;
        this.vitaminD = builder.vitaminD;
        this.vitaminE = builder.vitaminE;
        this.vitaminK = builder.vitaminK;
        this.cholesterol = builder.cholesterol;
        this.saturatedFats = builder.saturatedFats;
        this.transFats = builder.transFats;
        this.fiber = builder.fiber;
        this.sugar = builder.sugar;
        this.usefulness_index = builder.usefulness_index;
        this.idUUID = builder.idUUID;
        this.is_liquid = builder.is_liquid;
    }

    public static class Builder {
        private long id;
        private String name;
        private String category;
        private String subcategory;
        private int calories;
        private float proteins;
        private float fats;
        private float carbs;
        private int popularity;
        private float calcium;
        private float iron;
        private float magnesium;
        private float phosphorus;
        private float potassium;
        private float sodium;
        private float zinc;
        private float vitaminA;
        private float vitaminB1;
        private float vitaminB2;
        private float vitaminB3;
        private float vitaminB5;
        private float vitaminB6;
        private float vitaminB9;
        private float vitaminB12;
        private float vitaminC;
        private float vitaminD;
        private float vitaminE;
        private float vitaminK;
        private float cholesterol;
        private float saturatedFats;
        private float transFats;
        private float fiber;
        private float sugar;
        private int usefulness_index;
        private String idUUID;
        private boolean is_liquid;

        public Builder id(long id) { this.id = id; return this; }
        public Builder idUUID(String idUUID) { this.idUUID = idUUID; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder subcategory(String subcategory) { this.subcategory = subcategory; return this; }
        public Builder calories(int calories) { this.calories = calories; return this; }
        public Builder proteins(float proteins) { this.proteins = proteins; return this; }
        public Builder fats(float fats) { this.fats = fats; return this; }
        public Builder carbs(float carbs) { this.carbs = carbs; return this; }
        public Builder popularity(int popularity) { this.popularity = popularity; return this; }
        public Builder calcium(float calcium) { this.calcium = calcium; return this; }
        public Builder iron(float iron) { this.iron = iron; return this; }
        public Builder magnesium(float magnesium) { this.magnesium = magnesium; return this; }
        public Builder phosphorus(float phosphorus) { this.phosphorus = phosphorus; return this; }
        public Builder potassium(float potassium) { this.potassium = potassium; return this; }
        public Builder sodium(float sodium) { this.sodium = sodium; return this; }
        public Builder zinc(float zinc) { this.zinc = zinc; return this; }
        public Builder vitaminA(float vitaminA) { this.vitaminA = vitaminA; return this; }
        public Builder vitaminB1(float vitaminB1) { this.vitaminB1 = vitaminB1; return this; }
        public Builder vitaminB2(float vitaminB2) { this.vitaminB2 = vitaminB2; return this; }
        public Builder vitaminB3(float vitaminB3) { this.vitaminB3 = vitaminB3; return this; }
        public Builder vitaminB5(float vitaminB5) { this.vitaminB5 = vitaminB5; return this; }
        public Builder vitaminB6(float vitaminB6) { this.vitaminB6 = vitaminB6; return this; }
        public Builder vitaminB9(float vitaminB9) { this.vitaminB9 = vitaminB9; return this; }
        public Builder vitaminB12(float vitaminB12) { this.vitaminB12 = vitaminB12; return this; }
        public Builder vitaminC(float vitaminC) { this.vitaminC = vitaminC; return this; }
        public Builder vitaminD(float vitaminD) { this.vitaminD = vitaminD; return this; }
        public Builder vitaminE(float vitaminE) { this.vitaminE = vitaminE; return this; }
        public Builder vitaminK(float vitaminK) { this.vitaminK = vitaminK; return this; }
        public Builder cholesterol(float cholesterol) { this.cholesterol = cholesterol; return this; }
        public Builder saturatedFats(float saturatedFats) { this.saturatedFats = saturatedFats; return this; }
        public Builder transFats(float transFats) { this.transFats = transFats; return this; }
        public Builder fiber(float fiber) { this.fiber = fiber; return this; }
        public Builder sugar(float sugar) { this.sugar = sugar; return this; }
        public Builder usefulness_index(int usefulness_index) { this.usefulness_index = usefulness_index; return this; }
        public Builder is_liquid(boolean is_liquid) { this.is_liquid = is_liquid; return this; }

        public Food build() {
            return new Food(this);
        }
    }


    public long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getSubcategory() { return subcategory; }
    public int getCalories() { return calories; }
    public float getProteins() { return proteins; }
    public float getFats() { return fats; }
    public float getCarbs() { return carbs; }
    public Integer getPopularity() { return popularity; }
    public float getCalcium() { return calcium; }
    public float getIron() { return iron; }
    public float getMagnesium() { return magnesium; }
    public float getPhosphorus() { return phosphorus; }
    public float getPotassium() { return potassium; }
    public float getSodium() { return sodium; }
    public float getZinc() { return zinc; }
    public float getVitaminA() { return vitaminA; }
    public float getVitaminB1() { return vitaminB1; }
    public float getVitaminB2() { return vitaminB2; }
    public float getVitaminB3() { return vitaminB3; }
    public float getVitaminB5() { return vitaminB5; }
    public float getVitaminB6() { return vitaminB6; }
    public float getVitaminB9() { return vitaminB9; }
    public float getVitaminB12() { return vitaminB12; }
    public float getVitaminC() { return vitaminC; }
    public float getVitaminD() { return vitaminD; }
    public float getVitaminE() { return vitaminE; }
    public float getVitaminK() { return vitaminK; }
    public float getCholesterol() { return cholesterol; }
    public float getSaturatedFats() { return saturatedFats; }
    public float getTransFats() { return transFats; }
    public float getFiber() { return fiber; }
    public float getSugar() { return sugar; }
    public int getUsefulnessIndex() { return usefulness_index; }
    public String getIdUUID() { return idUUID; }
    public boolean isLiquid() { return is_liquid; }


    protected Food(Parcel in) {

        id = in.readLong();
        name = in.readString();

        category = in.readString();
        subcategory = in.readString();
        calories = in.readInt();
        proteins = in.readFloat();
        fats = in.readFloat();
        carbs = in.readFloat();
        popularity = in.readInt();
        calcium = in.readFloat();
        iron = in.readFloat();
        magnesium = in.readFloat();
        phosphorus = in.readFloat();
        potassium = in.readFloat();
        sodium = in.readFloat();
        zinc = in.readFloat();
        vitaminA = in.readFloat();
        vitaminB1 = in.readFloat();
        vitaminB2 = in.readFloat();
        vitaminB3 = in.readFloat();
        vitaminB5 = in.readFloat();
        vitaminB6 = in.readFloat();
        vitaminB9 = in.readFloat();
        vitaminB12 = in.readFloat();
        vitaminC = in.readFloat();
        vitaminD = in.readFloat();
        vitaminE = in.readFloat();
        vitaminK = in.readFloat();
        cholesterol = in.readFloat();
        saturatedFats = in.readFloat();
        transFats = in.readFloat();
        fiber = in.readFloat();
        sugar = in.readFloat();
        usefulness_index = in.readInt();
        idUUID = in.readString();
        is_liquid = in.readByte() != 0;

    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeLong(id);
        dest.writeString(name);
        dest.writeString(category);
        dest.writeString(subcategory);
        dest.writeInt(calories);
        dest.writeFloat(proteins);
        dest.writeFloat(fats);
        dest.writeFloat(carbs);
        dest.writeInt(popularity);
        dest.writeFloat(calcium);
        dest.writeFloat(iron);
        dest.writeFloat(magnesium);
        dest.writeFloat(phosphorus);
        dest.writeFloat(potassium);
        dest.writeFloat(sodium);
        dest.writeFloat(zinc);
        dest.writeFloat(vitaminA);
        dest.writeFloat(vitaminB1);
        dest.writeFloat(vitaminB2);
        dest.writeFloat(vitaminB3);
        dest.writeFloat(vitaminB5);
        dest.writeFloat(vitaminB6);
        dest.writeFloat(vitaminB9);
        dest.writeFloat(vitaminB12);
        dest.writeFloat(vitaminC);
        dest.writeFloat(vitaminD);
        dest.writeFloat(vitaminE);
        dest.writeFloat(vitaminK);
        dest.writeFloat(cholesterol);
        dest.writeFloat(saturatedFats);
        dest.writeFloat(transFats);
        dest.writeFloat(fiber);
        dest.writeFloat(sugar);
        dest.writeInt(usefulness_index);
        dest.writeString(idUUID);
        dest.writeByte((byte) (is_liquid ? 1 : 0));

    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Food> CREATOR = new Creator<Food>() {
        @Override
        public Food createFromParcel(Parcel in) {
            return new Food(in);
        }

        @Override
        public Food[] newArray(int size) {
            return new Food[size];
        }
    };
    

    static {
        try {


            initCreator();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при инициализации CREATOR в Food: " + e.getMessage(), e);
        }
    }
} 