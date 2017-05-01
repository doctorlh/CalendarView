package com.doctorlh.calendarviewlib;

public interface DatePickerController {

    enum FailEven {
        CONTAIN_NO_SELECTED, CONTAIN_INVALID, NO_REACH_LEAST_DAYS, NO_REACH_MOST_DAYS, END_MT_START;
    }

    void onDayOfMonthSelected(DatePickAdapter.CalendarDay startDay, DatePickAdapter.CalendarDay endDay); // 点击日期回调函数，月份记得加1

    void alertSelectedFail(FailEven even);
}