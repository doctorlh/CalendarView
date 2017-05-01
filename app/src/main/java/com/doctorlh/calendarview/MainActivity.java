package com.doctorlh.calendarview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;

import com.doctorlh.calendarviewlib.DatePickAdapter;
import com.doctorlh.calendarviewlib.DatePickerController;
import com.doctorlh.calendarviewlib.DatePikerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private DatePikerView mDayPikerView;
    private TextView tvStartDate, tvEndDate;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        mDayPikerView = (DatePikerView) findViewById(R.id.dpv_calendar);
        tvStartDate = (TextView) findViewById(R.id.tvStartDate);
        tvEndDate = (TextView) findViewById(R.id.tvEndDate);
        DatePikerView.DataModel dataModel = new DatePikerView.DataModel();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -12);
        dataModel.yearStart = calendar.get(Calendar.YEAR);
        dataModel.monthStart = calendar.get(Calendar.MONTH) + 1;
        dataModel.monthCount = 12;
        dataModel.leastDaysNum = 1;
        dataModel.mostDaysNum = 366;
        dataModel.selectedDays = new DatePickAdapter.SelectedDays<>();
        dataModel.selectedDays.setFirst(new DatePickAdapter.CalendarDay());
        dataModel.selectedDays.setLast(new DatePickAdapter.CalendarDay());
        mDayPikerView.setParameter(dataModel, datePickerController);
    }

    /**
     * 回调
     */
    private DatePickerController datePickerController = new DatePickerController() {
        @Override
        public void onDayOfMonthSelected(DatePickAdapter.CalendarDay startDay, DatePickAdapter.CalendarDay endDay) {
            if (startDay != null) {
                tvStartDate.setText(String.format("%s-%02d-%02d", startDay.year, startDay.month + 1, startDay.day));
            } else {
                tvStartDate.setText("");
            }
            if (endDay != null) {
                tvEndDate.setText(String.format("%s-%02d-%02d", endDay.year, endDay.month + 1, endDay.day));
            } else {
                tvEndDate.setText("");
            }
        }

        @Override
        public void alertSelectedFail(FailEven even) {

        }
    };
}
