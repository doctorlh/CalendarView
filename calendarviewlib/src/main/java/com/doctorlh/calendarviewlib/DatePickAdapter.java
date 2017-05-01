package com.doctorlh.calendarviewlib;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by 14074533 on 2017/4/27.
 */
public class DatePickAdapter extends RecyclerView.Adapter<DatePickAdapter.ViewHolder> implements DateView.OnDayClickListener {
    protected static final int MONTHS_IN_YEAR = 12;
    private final TypedArray typedArray;
    private final Context mContext;
    private final DatePickerController mController;             // 回调
    private Calendar calendar;
    private SelectedDays<CalendarDay> rangeDays;                // 选择日期范围
    private int mLeastDaysNum;                                  // 至少选择几天
    private int mMostDaysNum;                                   // 至多选择几天
    private DatePikerView.DataModel dataModel;

    public DatePickAdapter(Context context, TypedArray typedArray, DatePickerController datePickerController, DatePikerView.DataModel dataModel) {
        mContext = context;
        this.typedArray = typedArray;
        mController = datePickerController;
        this.dataModel = dataModel;
        initData();
    }

    /**
     * 初始化数据
     */
    private void initData() {
        calendar = Calendar.getInstance();

        if (dataModel.selectedDays == null) {
            dataModel.selectedDays = new SelectedDays<>();
        }

        if (dataModel.yearStart <= 0) {
            dataModel.yearStart = calendar.get(Calendar.YEAR);
        }
        if (dataModel.monthStart <= 0) {
            dataModel.monthStart = calendar.get(Calendar.MONTH);
        }

        if (dataModel.leastDaysNum <= 0) {
            dataModel.leastDaysNum = 0;
        }

        if (dataModel.mostDaysNum <= 0) {
            dataModel.mostDaysNum = 100;
        }

        if (dataModel.leastDaysNum > dataModel.mostDaysNum) {
            Log.e("error", "可选择的最小天数不能小于最大天数");
            throw new IllegalArgumentException("可选择的最小天数不能小于最大天数");
        }

        if (dataModel.monthCount <= 0) {
            dataModel.monthCount = 12;
        }

        mLeastDaysNum = dataModel.leastDaysNum;
        mMostDaysNum = dataModel.mostDaysNum;
        rangeDays = dataModel.selectedDays;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        final DateView simpleMonthView = new DateView(mContext, typedArray, dataModel);
        return new ViewHolder(simpleMonthView, this);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        final DateView v = viewHolder.simpleMonthView;
        final HashMap<String, Object> drawingParams = new HashMap<String, Object>();
        int month;          // 月份
        int year;           // 年份

        int monthStart = dataModel.monthStart;
        int yearStart = dataModel.yearStart;

        month = (monthStart + (position % MONTHS_IN_YEAR)) % MONTHS_IN_YEAR;
        year = position / MONTHS_IN_YEAR + yearStart + ((monthStart + (position % MONTHS_IN_YEAR)) / MONTHS_IN_YEAR);

        drawingParams.put(DateView.VIEW_PARAMS_SELECTED_BEGIN_DATE, rangeDays.getFirst());
        drawingParams.put(DateView.VIEW_PARAMS_SELECTED_LAST_DATE, rangeDays.getLast());
        drawingParams.put(DateView.VIEW_PARAMS_YEAR, year);
        drawingParams.put(DateView.VIEW_PARAMS_MONTH, month);
        drawingParams.put(DateView.VIEW_PARAMS_WEEK_START, calendar.getFirstDayOfWeek());
        v.setMonthParams(drawingParams);
        v.invalidate();
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return dataModel.monthCount;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final DateView simpleMonthView;

        public ViewHolder(View itemView, DateView.OnDayClickListener onDayClickListener) {
            super(itemView);
            simpleMonthView = (DateView) itemView;
            simpleMonthView.setLayoutParams(new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            simpleMonthView.setClickable(true);
            simpleMonthView.setOnDayClickListener(onDayClickListener);
        }
    }

    @Override
    public void onDayClick(DateView simpleMonthView, CalendarDay calendarDay) {
        if (calendarDay != null) {
            setRangeSelectedDay(calendarDay);
        }
    }

    /**
     * 范围选时对点击的日期的处理
     *
     * @param calendarDay
     */
    public void setRangeSelectedDay(CalendarDay calendarDay) {
        // 选择结束日期
        if (rangeDays.getFirst() != null && rangeDays.getLast() == null) {
            // 所选结束日期在开始日期之前,重置开始日期
            if (calendarDay.getDate().before(rangeDays.getFirst().getDate())) {
                rangeDays.setFirst(calendarDay);
                rangeDays.setLast(null);
                if (mController != null) {
                    mController.onDayOfMonthSelected(rangeDays.getFirst(), rangeDays.getLast());
                }
                notifyDataSetChanged();
                return;
            }

            int dayDiff = dateDiff(rangeDays.getFirst(), calendarDay);
            // 所选的日期范围不能小于最小限制
            if (dayDiff > 1 && mLeastDaysNum > dayDiff) {
                if (mController != null) {
                    mController.alertSelectedFail(DatePickerController.FailEven.NO_REACH_LEAST_DAYS);
                }
                return;
            }
            // 所选日期范围不能大于最大限制
            if (dayDiff > 1 && mMostDaysNum < dayDiff) {
                if (mController != null) {
                    mController.alertSelectedFail(DatePickerController.FailEven.NO_REACH_MOST_DAYS);
                }
                return;
            }
            rangeDays.setLast(calendarDay);
        } else if (rangeDays.getLast() != null) {   // 重新选择开始日期
            rangeDays.setFirst(calendarDay);
            rangeDays.setLast(null);
        } else {        // 第一次选择开始日期
            rangeDays.setFirst(calendarDay);
        }

        if (mController != null) {
            mController.onDayOfMonthSelected(rangeDays.getFirst(), rangeDays.getLast());
        }
        notifyDataSetChanged();
    }

    /**
     * 两个日期中间隔多少天
     *
     * @param first
     * @param last
     * @return
     */
    protected int dateDiff(CalendarDay first, CalendarDay last) {
        long dayDiff = (last.getDate().getTime() - first.getDate().getTime()) / (1000 * 3600 * 24);
        return Integer.valueOf(String.valueOf(dayDiff)) + 1;
    }

    /**
     * 设置数据集
     *
     * @param dataModel
     */
    protected void setDataModel(DatePikerView.DataModel dataModel) {
        this.dataModel = dataModel;
    }

    public static class CalendarDay implements Serializable, Comparable<CalendarDay> {
        private static final long serialVersionUID = -5456695978688356202L;
        private Calendar calendar;

        public int day;
        public int month;
        public int year;
        public String tag;

        public CalendarDay(Calendar calendar, String tag) {
            year = calendar.get(Calendar.YEAR);
            month = calendar.get(Calendar.MONTH);
            day = calendar.get(Calendar.DAY_OF_MONTH);
            this.tag = tag;
        }

        public CalendarDay() {
            setTime(System.currentTimeMillis());
        }

        public CalendarDay(int year, int month, int day) {
            setDay(year, month, day);
        }

        public CalendarDay(long timeInMillis) {
            setTime(timeInMillis);
        }

        public CalendarDay(Calendar calendar) {
            year = calendar.get(Calendar.YEAR);
            month = calendar.get(Calendar.MONTH);
            day = calendar.get(Calendar.DAY_OF_MONTH);
        }

        public CalendarDay(Date date) {
            if (date == null) {
                date = new Date();
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            year = calendar.get(Calendar.YEAR);
            month = calendar.get(Calendar.MONTH);
            day = calendar.get(Calendar.DAY_OF_MONTH);
        }


        private void setTime(long timeInMillis) {
            if (calendar == null) {
                calendar = Calendar.getInstance();
            }
            calendar.setTimeInMillis(timeInMillis);
            month = this.calendar.get(Calendar.MONTH);
            year = this.calendar.get(Calendar.YEAR);
            day = this.calendar.get(Calendar.DAY_OF_MONTH);
        }

        public void set(CalendarDay calendarDay) {
            year = calendarDay.year;
            month = calendarDay.month;
            day = calendarDay.day;
        }

        public void setDay(int year, int month, int day) {
            this.year = year;
            this.month = month;
            this.day = day;
        }

        public Date getDate() {
            if (calendar == null) {
                calendar = Calendar.getInstance();
            }
            calendar.clear();
            calendar.set(year, month, day);
            return calendar.getTime();
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        @Override
        public String toString() {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{ year: ");
            stringBuilder.append(year);
            stringBuilder.append(", month: ");
            stringBuilder.append(month);
            stringBuilder.append(", day: ");
            stringBuilder.append(day);
            stringBuilder.append(" }");

            return stringBuilder.toString();
        }

        /**
         * 只比较年月日
         *
         * @param calendarDay
         * @return
         */
        @Override
        public int compareTo(CalendarDay calendarDay) {
            if (calendarDay == null) {
                throw new IllegalArgumentException("被比较的日期不能是null");
            }

            if (year == calendarDay.year && month == calendarDay.month && day == calendarDay.day) {
                return 0;
            }

            if (year < calendarDay.year ||
                    (year == calendarDay.year && month < calendarDay.month) ||
                    (year == calendarDay.year && month == calendarDay.month && day < calendarDay.day)) {
                return -1;
            }
            return 1;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof CalendarDay) {
                CalendarDay calendarDay = (CalendarDay) o;
                if (compareTo(calendarDay) == 0) {
                    return true;
                }
            }
            return false;
        }

        /**
         * 大于比较的日期（只比较年月日）
         *
         * @param o
         * @return
         */
        public boolean after(Object o) {
            if (o instanceof CalendarDay) {
                CalendarDay calendarDay = (CalendarDay) o;
                if (compareTo(calendarDay) == 1) {
                    return true;
                }
            }
            return false;
        }

        /**
         * 小于比较的日期（只比较年月日）
         *
         * @param o
         * @return
         */
        public boolean before(Object o) {
            if (o instanceof CalendarDay) {
                CalendarDay calendarDay = (CalendarDay) o;
                if (compareTo(calendarDay) == -1) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class SelectedDays<K> implements Serializable {
        private static final long serialVersionUID = 3942549765282708376L;
        private K first;
        private K last;

        public SelectedDays() {
        }

        public SelectedDays(K first, K last) {
            this.first = first;
            this.last = last;
        }

        public K getFirst() {
            return first;
        }

        public void setFirst(K first) {
            this.first = first;
        }

        public K getLast() {
            return last;
        }

        public void setLast(K last) {
            this.last = last;
        }
    }
}