package com.martist.vitamove.utils;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.martist.vitamove.R;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.util.Collection;
import java.util.HashSet;

public class WorkoutDayDecorator implements DayViewDecorator {
    private final HashSet<CalendarDay> dates;
    private final int color;

    public WorkoutDayDecorator(Context context, Collection<CalendarDay> dates) {
        this.color = ContextCompat.getColor(context, R.color.orange_500);
        this.dates = new HashSet<>(dates);
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return dates.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        
        view.addSpan(new DotSpan(6, color));
    }

    public void setDates(Collection<CalendarDay> dates) {
        this.dates.clear();
        this.dates.addAll(dates);
    }
} 