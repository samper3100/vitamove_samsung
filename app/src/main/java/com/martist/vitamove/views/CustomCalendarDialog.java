package com.martist.vitamove.views;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.martist.vitamove.R;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.format.ArrayWeekDayFormatter;
import com.prolificinteractive.materialcalendarview.format.MonthArrayTitleFormatter;

import org.threeten.bp.DayOfWeek;

public class CustomCalendarDialog extends DialogFragment {
    public interface OnDateSelectedListener {
        void onDateSelected(CalendarDay date);
    }

    private OnDateSelectedListener listener;
    private CalendarDay initialDate;

    public CustomCalendarDialog(CalendarDay initialDate, OnDateSelectedListener listener) {
        this.initialDate = initialDate;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_custom_calendar, null);
        MaterialCalendarView calendarView = view.findViewById(R.id.calendarView);

        
        String[] monthsArray = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};
        String[] weekDaysArray = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        calendarView.setTitleFormatter(new MonthArrayTitleFormatter(monthsArray));
        calendarView.setWeekDayFormatter(new ArrayWeekDayFormatter(weekDaysArray));
        calendarView.state().edit().setFirstDayOfWeek(DayOfWeek.of(1)).commit();

        
        if (initialDate != null) {
            calendarView.setSelectedDate(initialDate);
            calendarView.setCurrentDate(initialDate);
        }

        Button btnOk = view.findViewById(R.id.btn_ok);
        Button btnCancel = view.findViewById(R.id.btn_cancel);

        btnOk.setOnClickListener(v -> {
            CalendarDay selected = calendarView.getSelectedDate();
            if (selected != null && listener != null) {
                listener.onDateSelected(selected);
            }
            dismiss();
        });
        btnCancel.setOnClickListener(v -> dismiss());

        builder.setView(view);
        return builder.create();
    }
} 