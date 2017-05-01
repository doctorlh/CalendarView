package com.doctorlh.calendarviewlib;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.view.MotionEvent;
import android.view.View;

import java.security.InvalidParameterException;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

/**
 * 每个月作为一个ItemView
 * Created by 14074533 on 2017/4/27.
 */
class DateView extends View {

    public static final String VIEW_PARAMS_SELECTED_BEGIN_DATE = "selected_begin_date";
    public static final String VIEW_PARAMS_SELECTED_LAST_DATE = "selected_last_date";

    public static final String VIEW_PARAMS_MONTH = "month";
    public static final String VIEW_PARAMS_YEAR = "year";
    public static final String VIEW_PARAMS_WEEK_START = "week_start";

    protected static int DEFAULT_HEIGHT = 32;                           // 默认一行的高度
    protected static int MINI_DAY_NUMBER_TEXT_SIZE;                     // 日期字体的最小尺寸
    private static int TAG_TEXT_SIZE;                                   // 标签字体大小
    protected static int MONTH_HEADER_SIZE;                             // 头部的高度（包括年份月份，星期几）
    protected static int YEAR_MONTH_TEXT_SIZE;                         // 头部年份月份的字体大小
    protected static int WEEK_TEXT_SIZE;                                // 头部年份月份的字体大小

    protected int mPadding = 0;

    protected Paint mWeekTextPaint;                     // 头部星期几的字体画笔
    protected Paint mDayTextPaint;
    protected Paint mTagPaint;
    protected Paint mYearMonthPaint;                    // 头部的画笔
    protected Paint mSelectedDayBgPaint;
    // 开始结束之间的背景
    protected Paint mSelectedBetweenBgPaint;
    protected int mCurrentDayTextColor;                 // 今天的字体颜色
    protected int mYearMonthTextColor;                  // 头部年份和月份字体颜色
    protected int mWeekTextColor;                       // 头部星期几字体颜色
    protected int mDayTextColor;                        // 日期字体颜色
    protected int mSelectedDayTextColor;                // 被选中的日期字体颜色
    protected int mSelectedDaysBgColor;                 // 选中的日期背景颜色

    private final StringBuilder mStringBuilder;

    protected boolean mHasToday = false;
    protected int mToday = -1;
    protected int mWeekStart = 1;               // 一周的第一天（不同国家的一星期的第一天不同）
    protected int mNumDays = 7;                 // 一行几列
    protected int mNumCells;                    // 一个月有多少天
    private int mDayOfWeekStart = 0;            // 日期对应星期几
    protected int mRowWidth = DEFAULT_HEIGHT;
    protected int mRowHeight = DEFAULT_HEIGHT;  // 行高
    protected int mWidth;                       // simpleMonthView的宽度

    protected int mYear;
    protected int mMonth;
    final Time today;

    private final Calendar mCalendar;
    private final Calendar mDayLabelCalendar;           // 用于显示星期几
    private final Boolean isPrevDayEnabled;             // 今天以前的日期是否能被操作

    private int mNumRows;

    private DateFormatSymbols mDateFormatSymbols = new DateFormatSymbols();

    private OnDayClickListener mOnDayClickListener;

    DatePickAdapter.CalendarDay mStartDate;          // 开始日期
    DatePickAdapter.CalendarDay mEndDate;            // 结束日期

    DatePickAdapter.CalendarDay cellCalendar;        // cell的对应的日期

    /**
     * @param context
     * @param typedArray
     * @param dataModel
     */
    public DateView(Context context, TypedArray typedArray, DatePikerView.DataModel dataModel) {
        super(context);
        mDayLabelCalendar = Calendar.getInstance();
        mCalendar = Calendar.getInstance();
        today = new Time(Time.getCurrentTimezone());
        today.setToNow();
        mCurrentDayTextColor = typedArray.getColor(R.styleable.DatePikerView_colorCurrentDay, Color.BLACK);
        mYearMonthTextColor = typedArray.getColor(R.styleable.DatePikerView_colorYearMonthText, Color.BLACK);
        mWeekTextColor = typedArray.getColor(R.styleable.DatePikerView_colorWeekText, Color.BLACK);
        mDayTextColor = typedArray.getColor(R.styleable.DatePikerView_colorNormalDayText, Color.BLACK);
        mSelectedDaysBgColor = typedArray.getColor(R.styleable.DatePikerView_colorSelectedDayBackground, Color.YELLOW);
        mSelectedDayTextColor = typedArray.getColor(R.styleable.DatePikerView_colorSelectedDayText, Color.WHITE);

        mStringBuilder = new StringBuilder(50);

        MINI_DAY_NUMBER_TEXT_SIZE = typedArray.getDimensionPixelSize(R.styleable.DatePikerView_textSizeDay, 28);
        TAG_TEXT_SIZE = typedArray.getDimensionPixelSize(R.styleable.DatePikerView_textSizeTag, 20);
        YEAR_MONTH_TEXT_SIZE = typedArray.getDimensionPixelSize(R.styleable.DatePikerView_textSizeYearMonth, 32);
        WEEK_TEXT_SIZE = typedArray.getDimensionPixelSize(R.styleable.DatePikerView_textSizeWeek, 20);
        MONTH_HEADER_SIZE = typedArray.getDimensionPixelOffset(R.styleable.DatePikerView_headerMonthHeight, 100);
        mRowHeight = ((typedArray.getDimensionPixelSize(R.styleable.DatePikerView_rowHeight, 110)));
        isPrevDayEnabled = typedArray.getBoolean(R.styleable.DatePikerView_enablePreviousDay, false);

        cellCalendar = new DatePickAdapter.CalendarDay();

        initView();
    }

    /**
     * 计算每个月的日期占用的行数
     *
     * @return
     */
    private int calculateNumRows() {
        int offset = findDayOffset();
        int dividend = (offset + mNumCells) / mNumDays;
        int remainder = (offset + mNumCells) % mNumDays;
        return (dividend + (remainder > 0 ? 1 : 0));
    }

    /**
     * 绘制头部的一行星期几
     *
     * @param canvas
     */
    private void drawMonthDayLabels(Canvas canvas) {
        int y = MONTH_HEADER_SIZE - (WEEK_TEXT_SIZE / 2);
        // 一个cell的二分之宽度
        int dayWidthHalf = (mWidth - mPadding * 2) / (mNumDays * 2);

        for (int i = 0; i < mNumDays; i++) {
            int calendarDay = (i + mWeekStart) % mNumDays;
            int x = (2 * i + 1) * dayWidthHalf + mPadding;
            mDayLabelCalendar.set(Calendar.DAY_OF_WEEK, calendarDay);
            canvas.drawText(mDateFormatSymbols.getShortWeekdays()[mDayLabelCalendar.get(Calendar.DAY_OF_WEEK)].toUpperCase(Locale.getDefault()),
                    x, y, mWeekTextPaint);
        }
    }

    /**
     * 绘制头部（年份月份，星期几）
     *
     * @param canvas
     */
    private void drawMonthTitle(Canvas canvas) {
        int x = (mWidth + 2 * mPadding) / 2;
        int y = MONTH_HEADER_SIZE / 2 + (YEAR_MONTH_TEXT_SIZE / 3);
        StringBuilder stringBuilder = new StringBuilder(getMonthAndYearString().toLowerCase());
        stringBuilder.setCharAt(0, Character.toUpperCase(stringBuilder.charAt(0)));
        canvas.drawText(stringBuilder.toString(), x, y, mYearMonthPaint);
    }

    /**
     * 每个月第一天是星期几
     *
     * @return
     */
    private int findDayOffset() {
        return (mDayOfWeekStart < mWeekStart ? (mDayOfWeekStart + mNumDays) : mDayOfWeekStart)
                - mWeekStart;
    }

    /**
     * 获取年份和月份
     *
     * @return
     */
    private String getMonthAndYearString() {
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_NO_MONTH_DAY;
        mStringBuilder.setLength(0);
        long millis = mCalendar.getTimeInMillis();
        return DateUtils.formatDateRange(getContext(), millis, millis, flags);
    }

    private void onDayClick(DatePickAdapter.CalendarDay calendarDay) {
        if (mOnDayClickListener != null && (isPrevDayEnabled || !prevDay(calendarDay.day, today))) {
            mOnDayClickListener.onDayClick(this, calendarDay);
        }
    }

    private boolean sameDay(int monthDay, Time time) {
        return (mYear == time.year) && (mMonth == time.month) && (monthDay == time.monthDay);
    }

    /**
     * 判断是否是已经过去的日期
     *
     * @param monthDay
     * @param time
     * @return
     */
    private boolean prevDay(int monthDay, Time time) {
        return ((mYear < time.year)) || (mYear == time.year && mMonth < time.month) ||
                (mYear == time.year && mMonth == time.month && monthDay < time.monthDay);
    }

    /**
     * 绘制所有的cell
     *
     * @param canvas
     */
    protected void drawMonthCell(Canvas canvas) {
        int y = MONTH_HEADER_SIZE + mRowHeight / 2;
        int paddingDay = (mWidth - 2 * mPadding) / (2 * mNumDays);
        int dayOffset = findDayOffset();
        int day = 1;

        while (day <= mNumCells) {
            int x = paddingDay * (1 + dayOffset * 2) + mPadding;

            mDayTextPaint.setColor(mDayTextColor);

            cellCalendar.setDay(mYear, mMonth, day);

            // 已过去的日期
            boolean isPrevDay = false;
            if (!isPrevDayEnabled && prevDay(day, today)) {
                isPrevDay = true;
                canvas.drawText(String.format("%d", day), x, getTextYCenter(mDayTextPaint, y), mDayTextPaint);
            }

            // 开始时间和结束时间相同，则显示单日
            if (mStartDate != null && mEndDate != null && mStartDate.equals(mEndDate) && cellCalendar.equals(mStartDate)) {
                drawDayBg(canvas, x, y, mSelectedDayBgPaint);
                mDayTextPaint.setColor(mSelectedDayTextColor);
                canvas.drawText("单日", x, getTextYCenter(mDayTextPaint, y + mRowHeight / 4), mTagPaint);
            }

            // 绘制起始日期的方格
            if (mStartDate != null && cellCalendar.equals(mStartDate) && !mStartDate.equals(mEndDate)) {
                drawDayBg(canvas, x, y, mSelectedDayBgPaint);
                mDayTextPaint.setColor(mSelectedDayTextColor);
                canvas.drawText("开始", x, getTextYCenter(mDayTextPaint, y + mRowHeight / 4), mTagPaint);
            }

            // 绘制结束日期的方格
            if (mEndDate != null && cellCalendar.equals(mEndDate) && !mStartDate.equals(mEndDate)) {
                drawDayBg(canvas, x, y, mSelectedDayBgPaint);
                mDayTextPaint.setColor(mSelectedDayTextColor);
                canvas.drawText("结束", x, getTextYCenter(mDayTextPaint, y + mRowHeight / 4), mTagPaint);
            }

            // 在开始和结束之间的日期
            if (cellCalendar.after(mStartDate) && cellCalendar.before(mEndDate)) {
                drawDayBg(canvas, x, y, mSelectedBetweenBgPaint);
            }

            // 绘制日期
            if (!isPrevDay) {
                canvas.drawText(String.format("%d", day), x, getTextYCenter(mDayTextPaint, y), mDayTextPaint);
            }

            dayOffset++;
            if (dayOffset == mNumDays) {
                dayOffset = 0;
                y += mRowHeight;
            }
            day++;
        }
    }

    /**
     * 根据坐标获取对应的日期
     *
     * @param x
     * @param y
     * @return
     */
    public DatePickAdapter.CalendarDay getDayFromLocation(float x, float y) {
        int padding = mPadding;
        if ((x < padding) || (x > mWidth - mPadding)) {
            return null;
        }

        int yDay = (int) (y - MONTH_HEADER_SIZE) / mRowHeight;
        int day = 1 + ((int) ((x - padding) * mNumDays / (mWidth - padding - mPadding)) - findDayOffset()) + yDay * mNumDays;

        if (mMonth > 11 || mMonth < 0 || CalendarUtils.getDaysInMonth(mMonth, mYear) < day || day < 1)
            return null;

        DatePickAdapter.CalendarDay calendar = new DatePickAdapter.CalendarDay(mYear, mMonth, day);
        return calendar;
    }

    /**
     * 初始化一些paint
     */
    protected void initView() {
        // 头部年份和月份的字体paint
        mYearMonthPaint = new Paint();
        mYearMonthPaint.setAntiAlias(true);
        mYearMonthPaint.setTextSize(YEAR_MONTH_TEXT_SIZE);
        mYearMonthPaint.setColor(mYearMonthTextColor);
        mYearMonthPaint.setTextAlign(Align.CENTER);
        mYearMonthPaint.setStyle(Style.FILL);

        // 头部星期几字体paint
        mWeekTextPaint = new Paint();
        mWeekTextPaint.setAntiAlias(true);
        mWeekTextPaint.setTextSize(WEEK_TEXT_SIZE);
        mWeekTextPaint.setColor(mWeekTextColor);
        mWeekTextPaint.setStyle(Style.FILL);
        mWeekTextPaint.setTextAlign(Align.CENTER);
        mWeekTextPaint.setFakeBoldText(true);

        // 被选中的日期背景paint
        mSelectedDayBgPaint = new Paint();
        mSelectedDayBgPaint.setFakeBoldText(true);
        mSelectedDayBgPaint.setAntiAlias(true);
        mSelectedDayBgPaint.setColor(mSelectedDaysBgColor);
        mSelectedDayBgPaint.setTextAlign(Align.CENTER);
        mSelectedDayBgPaint.setStyle(Style.FILL);

        //选中之间的背景
        mSelectedBetweenBgPaint = new Paint();
        mSelectedBetweenBgPaint.setFakeBoldText(true);
        mSelectedBetweenBgPaint.setAntiAlias(true);
        mSelectedBetweenBgPaint.setColor(mSelectedDaysBgColor);
        mSelectedBetweenBgPaint.setTextAlign(Align.CENTER);
        mSelectedBetweenBgPaint.setStyle(Style.FILL);
        mSelectedBetweenBgPaint.setAlpha(80);

        // 日期字体paint
        mDayTextPaint = new Paint();
        mDayTextPaint.setAntiAlias(true);
        mDayTextPaint.setColor(mDayTextColor);
        mDayTextPaint.setTextSize(MINI_DAY_NUMBER_TEXT_SIZE);
        mDayTextPaint.setStyle(Style.FILL);
        mDayTextPaint.setTextAlign(Align.CENTER);
        mDayTextPaint.setFakeBoldText(false);

        //开始结束的字体
        mTagPaint = new Paint();
        mTagPaint.setAntiAlias(true);
        mTagPaint.setColor(mSelectedDayTextColor);
        mTagPaint.setTextSize(TAG_TEXT_SIZE);
        mTagPaint.setStyle(Style.FILL);
        mTagPaint.setTextAlign(Align.CENTER);
        mTagPaint.setFakeBoldText(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawMonthTitle(canvas);
        drawMonthCell(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 设置simpleMonthView的宽度和高度
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), mRowHeight * mNumRows + MONTH_HEADER_SIZE);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;
        mRowWidth = mWidth / mNumDays;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            DatePickAdapter.CalendarDay calendarDay = getDayFromLocation(event.getX(), event.getY());
            if (calendarDay == null) {
                return true;
            }
            onDayClick(calendarDay);
        }
        return true;
    }

    /**
     * 设置传递进来的参数
     *
     * @param params
     */
    public void setMonthParams(HashMap<String, Object> params) {
        if (!params.containsKey(VIEW_PARAMS_MONTH) && !params.containsKey(VIEW_PARAMS_YEAR)) {
            throw new InvalidParameterException("You must specify month and year for this view");
        }
        setTag(params);

        if (params.containsKey(VIEW_PARAMS_SELECTED_BEGIN_DATE)) {
            mStartDate = (DatePickAdapter.CalendarDay) params.get(VIEW_PARAMS_SELECTED_BEGIN_DATE);
        }
        if (params.containsKey(VIEW_PARAMS_SELECTED_LAST_DATE)) {
            mEndDate = (DatePickAdapter.CalendarDay) params.get(VIEW_PARAMS_SELECTED_LAST_DATE);
        }

        mMonth = (int) params.get(VIEW_PARAMS_MONTH);
        mYear = (int) params.get(VIEW_PARAMS_YEAR);

        mHasToday = false;
        mToday = -1;

        mCalendar.set(Calendar.MONTH, mMonth);
        mCalendar.set(Calendar.YEAR, mYear);
        mCalendar.set(Calendar.DAY_OF_MONTH, 1);
        mDayOfWeekStart = mCalendar.get(Calendar.DAY_OF_WEEK);

        if (params.containsKey(VIEW_PARAMS_WEEK_START)) {
            mWeekStart = (int) params.get(VIEW_PARAMS_WEEK_START);
        } else {
            mWeekStart = mCalendar.getFirstDayOfWeek();
        }

        mNumCells = CalendarUtils.getDaysInMonth(mMonth, mYear);
        for (int i = 0; i < mNumCells; i++) {
            final int day = i + 1;
            if (sameDay(day, today)) {
                mHasToday = true;
                mToday = day;
            }
        }

        mNumRows = calculateNumRows();
    }

    public void setOnDayClickListener(OnDayClickListener onDayClickListener) {
        mOnDayClickListener = onDayClickListener;
    }

    public interface OnDayClickListener {
        void onDayClick(DateView simpleMonthView, DatePickAdapter.CalendarDay calendarDay);
    }

    /**
     * 绘制cell
     *
     * @param canvas
     * @param x
     * @param y
     */
    private void drawDayBg(Canvas canvas, int x, int y, Paint paint) {
        Rect rect = new Rect(x - mRowWidth / 2, y - mRowHeight / 2, x + mRowWidth / 2, y + mRowHeight / 2);
        canvas.drawRect(rect, paint);
    }

    /**
     * 在使用drawText方法时文字不能根据y坐标居中，所以重新计算y坐标
     *
     * @param paint
     * @param y
     * @return
     */
    private float getTextYCenter(Paint paint, int y) {
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float fontTotalHeight = fontMetrics.bottom - fontMetrics.top;
        float offY = fontTotalHeight / 2 - fontMetrics.bottom;
        return y + offY;
    }
}