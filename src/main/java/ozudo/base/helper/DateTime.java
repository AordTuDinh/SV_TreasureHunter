package ozudo.base.helper;

import ozudo.base.log.Logs;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DateTime {
    public static final long SECOND2_MILLI_SECOND = 1000;
    public static final long MIN_MILLI_SECOND = 60 * SECOND2_MILLI_SECOND;
    public static final long HOUR_MILLI_SECOND = 60 * MIN_MILLI_SECOND;
    public static final long DAY_MILLI_SECOND = 24 * HOUR_MILLI_SECOND;
    public static final long WEEK_MILLI_SECOND = 7 * DAY_MILLI_SECOND;
    //
    public static final long MIN_SECOND = 60;
    public static final long HOUR_SECOND = 60 * MIN_SECOND;
    public static final long DAY_SECOND = 24 * HOUR_SECOND;
    //
    public static final long HOUR_MIN = 60;
    public static final long DAY_MIN = 24 * HOUR_MIN;
    public static final long WEEK_MIN = 7 * DAY_MIN;
    //
    public static final long DAY_HOUR = 24;
    public static final long WEEK_HOUR = 7 * DAY_HOUR;

    /**
     * @param tpl: date template : yyyy-MM-dd HH:mm:ss
     * @return
     */
    public static boolean beforeDate(String tpl) {
        try {
            return Calendar.getInstance().getTime().before(fullDate.parse(tpl));
        } catch (Exception ex) {
            Logs.error(ex);
        }
        return true;
    }

    /**
     * @param tpl: date template : yyyy-MM-dd HH:mm:ss
     * @return
     */
    public static boolean afterDate(String tpl) {
        try {
            return Calendar.getInstance().getTime().after(fullDate.parse(tpl));
        } catch (Exception ex) {
            Logs.error(ex);
        }
        return true;
    }

    public static long numberDayPassed(Date date) {
        return (Calendar.getInstance().getTimeInMillis() - date.getTime()) / DAY_MILLI_SECOND;
    }

    public static long numberDayPassed(long milli) {
        return (Calendar.getInstance().getTimeInMillis() - milli) / DAY_MILLI_SECOND;
    }

    public static long numberDayPassed(Date fromDate, Date toDate) {
        return (toDate.getTime() - fromDate.getTime()) / DAY_MILLI_SECOND;
    }

    public static long numberSecondsPassed(long ms) {
        return (Calendar.getInstance().getTimeInMillis() - ms) / SECOND2_MILLI_SECOND;
    }

    public static int getSeconds() {
        return (int) (Calendar.getInstance().getTimeInMillis() / 1000);
    }

    // gen event day không  bị trùng lặp theo năm
    public static int getNumberDay() {
        int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        int year = Calendar.getInstance().get(Calendar.YEAR);
        return Integer.parseInt(year + "" + day);
    }

    // gen event week không  bị trùng lặp theo năm
    public static int getNumberWeek() {
        int week = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);
        int year = Calendar.getInstance().get(Calendar.YEAR);
        return Integer.parseInt(year + "" + week);
    }

    public static Calendar getCalendar(int timeType, int numberTime) {
        return getCalendar(Calendar.getInstance().getTime(), timeType, numberTime);
    }

    public static Calendar getCalendar(Date date, int timeType, int numberTime) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(timeType, numberTime);
        return cal;
    }
    // truyền vào mốc thời gian reset, thời gian reset
    public static boolean isAfterTime(long timeCheck, float timeSecond) {
        return System.currentTimeMillis() - timeCheck > (timeSecond * SECOND2_MILLI_SECOND);
    }

    public static boolean isDate(String strDate) {
        return getDateyyyyMMddCross(Calendar.getInstance().getTime()).equals(strDate);
    }

    public static int businessDays(Date startDate, Date endDate) {
        return businessDays(startDate, endDate, false);
    }

    public static String formatTime(long seconds) {
        long min = seconds / 60, sec = seconds % 60;
        long hour = min / 60;
        min = min % 60;
        if (min == 0 && hour == 0) return sec + " s";
        if (hour == 0) return String.format("%s m %s s", min, sec);
        return String.format("%s h %s m", hour, min);
    }

    /**
     * Seconds until end of current day
     */
    public static long secondsUntilEndDay() {
        Calendar ca = Calendar.getInstance();
        int hour = 23 - ca.get(Calendar.HOUR_OF_DAY);
        int minute = 59 - ca.get(Calendar.MINUTE);
        int second = 60 - ca.get(Calendar.SECOND);
        return hour * HOUR_SECOND + minute * 60 + second;
    }

    public static long msUntilEndDay() {
        return secondsUntilEndDay()* DateTime.SECOND2_MILLI_SECOND;
    }

    public static int minuteToSeconds(int minute) {
        return minute * 60;
    }

    /**
     * Get first monday of month
     */
    public static Calendar firstMonthDayOfMonth(Calendar calendar) {
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.set(Calendar.DAY_OF_WEEK_IN_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar;
    }
    // The first day of week is Monday

    /**
     * Business Days
     * <p/>
     * Get number of working days between 2 dates
     * <p/>
     *
     * @param startDate   date in the format of Y-m-d
     * @param endDate     date in the format of Y-m-d
     * @param weekendDays returns the number of weekends
     * @return int  returns the total number of days
     */
    public static int businessDays(Date startDate, Date endDate, boolean weekendDays) {
        Calendar begin = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        begin.setTime(startDate);
        end.setTime(endDate);

        if (begin.after(end)) {
            return 0;
        }

        int numDays = 0;
        int weekends = 0;

        while (begin.before(end)) {
            numDays++; // no of days in the given interval
            if (begin.get(Calendar.DAY_OF_WEEK) > 5) { // 6 and 7 are weekend days
                weekends++;
            }
            begin.add(Calendar.DATE, 1);
        }

        if (weekendDays) {
            return weekends;
        }

        int working_days = numDays - weekends;
        return working_days;
    }

    public static List<Date> businessDates(Date startDate, Date endDate) {
        return businessDates(startDate, endDate, 6);
    }

    /**
     * get an array of dates between 2 dates (not including weekends)
     *
     * @param startDate start date
     * @param endDate   end date
     * @param nonWork   day of week(int) where weekend begins - 5 = fri -> sun, 6 = sat -> sun, 7 = sunday
     * @return array   list of dates between $startDate and $endDate
     */
    public static List<Date> businessDates(Date startDate, Date endDate, int nonWork) {
        Calendar begin = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        begin.setTime(startDate);
        end.setTime(endDate);

        if (begin.after(end)) {
            return new ArrayList<Date>();
        }

        List<Date> aDate = new ArrayList<Date>();
        while (begin.before(end)) {
            if (begin.get(Calendar.DAY_OF_WEEK) < nonWork) {
                aDate.add(begin.getTime());
            }
            begin.add(Calendar.DATE, 1);
        }
        return aDate;
    }

    /**
     * Takes a month/year as input and returns the number of days
     * for the given month/year. Takes leap years into consideration.
     *
     * @param date
     * @return int
     */
    public static int daysInMonth(Date date) {
        Calendar ca = Calendar.getInstance();
        ca.setTime(date);
        return ca.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    public static long getMSFromMin(int minute) {
        return minute * 60 * 1000;
    }

    public static long getMSHour(int hour) {
        return hour * 3600 * 1000;
    }

    public static long getMSMin(int min) {
        return min * 60 * 1000;
    }

    public static long getMSSecond(int second) {
        return second * 1000;
    }

    public static long getNextMSFromMin(int minute) {
        return minute * 60 * 1000 + System.currentTimeMillis();
    }

    public static long getNextMSHour(int hour) {
        return hour * 3600 * 1000 + System.currentTimeMillis();
    }

    public static SimpleDateFormat MMdd = new SimpleDateFormat("MMdd");
    public static SimpleDateFormat yyyyMMddCross = new SimpleDateFormat("yyyy-MM-dd");
    public static SimpleDateFormat yyyyMMCross = new SimpleDateFormat("yyyy-MM");
    public static SimpleDateFormat yyyyMMddhh = new SimpleDateFormat("yyyy-MM-dd HH");
    private static SimpleDateFormat fullDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static SimpleDateFormat ddMM = new SimpleDateFormat("dd-MM");
    public static SimpleDateFormat hh = new SimpleDateFormat("HH");
    public static SimpleDateFormat ddMMyyyy = new SimpleDateFormat("dd-MM-yyyy");

    public static String getDateMMdd(Date date) {
        return MMdd.format(date);
    }

    public static SimpleDateFormat get_yyyyMMdd() {
        return new SimpleDateFormat("yyyyMMdd");
    }

    public static String getDateyyyyMMdd(Date date) {
        return get_yyyyMMdd().format(date);
    }

    public static String getDateyyyyMMCross(Date date) {
        return yyyyMMCross.format(date);
    }

    public static String getDateyyyyMMdd() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    public static String getDateyyyyMMddCross(Date date) {
        return yyyyMMddCross.format(date);
    }

    public static String getDateyyyyMMddhh(Date date) {
        return yyyyMMddhh.format(date);
    }

    public static String getFullDate() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
    }

    public static String getHour() {
        return new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime());
    }

    public static SimpleDateFormat getSDFFullDate() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    public static String getFullDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    public static long getEndOfDay(Date day) {
        Date date = getEndOfDay(day, Calendar.getInstance());
        return date.getTime();
    }

    public static Date getEndOfDay(Date day, Calendar cal) {
        if (day == null) day = Calendar.getInstance().getTime();
        cal.setTime(day);
        cal.set(Calendar.HOUR_OF_DAY, cal.getMaximum(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, cal.getMaximum(Calendar.MINUTE));
        cal.set(Calendar.SECOND, cal.getMaximum(Calendar.SECOND));
        cal.set(Calendar.MILLISECOND, cal.getMaximum(Calendar.MILLISECOND));
        return cal.getTime();
    }

    public static Date getCurrentDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static Date getStartOfDay(Date day) {
        if (day == null) day = Calendar.getInstance().getTime();
        Calendar cal = Calendar.getInstance();
        cal.setTime(day);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static long getDayDiff(Date fromDate, Date toDate) {
        if (fromDate == null || toDate == null) {
            return 0;
        }
        fromDate = getStartOfDay(fromDate);
        toDate = getStartOfDay(toDate);

        return numberDayPassed(fromDate, toDate);
    }

    public static int getDayToNumberDay(int numberDayFrom) {
        return Math.abs(numberDayFrom - DateTime.getNumberDay());
    }

    public static boolean equalsWeek(Date fromDate, Date toDate) {
        if (fromDate == null || toDate == null) {
            return false;
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(fromDate);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(toDate);
        return cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR);
    }

    public static boolean equalsMonth(Date fromDate, Date toDate) {
        if (fromDate == null || toDate == null) {
            return false;
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(fromDate);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(toDate);
        return cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH);
    }


    public static long getHourDiff(Date fromDate, Date toDate) {
        if (fromDate == null || toDate == null) {
            return 0;
        }

        return (toDate.getTime() - fromDate.getTime()) / HOUR_MILLI_SECOND;
    }

    public static Date getDateOffset(Date fromDate, int offset) {
        if (fromDate == null) return null;
        Date dateOffset = Calendar.getInstance().getTime();
        dateOffset.setTime(fromDate.getTime() + offset * DAY_MILLI_SECOND);

        return dateOffset;
    }

    public static Date getHourOffset(Date fromDate, int hourOffset) {
        if (fromDate == null) return null;
        Date dateOffset = Calendar.getInstance().getTime();
        dateOffset.setTime(fromDate.getTime() + hourOffset * HOUR_MILLI_SECOND);

        return dateOffset;
    }

    public static Date getSecondOffset(Date fromDate, long secondOffset) {
        if (fromDate == null) return null;
        Date dateOffset = Calendar.getInstance().getTime();
        dateOffset.setTime(fromDate.getTime() + secondOffset * 1000);

        return dateOffset;
    }

    public static Date getMilliSecondOffset(Date fromDate, long millisecondOffset) {
        if (fromDate == null) return null;
        Date dateOffset = Calendar.getInstance().getTime();
        dateOffset.setTime(fromDate.getTime() + millisecondOffset);

        return dateOffset;
    }

    public static long getSecondsToNextDay() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        return (c.getTime().getTime() - Calendar.getInstance().getTime().getTime()) / 1000;
    }

    public static long getSecondsToNextWeek() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, c.getActualMaximum(Calendar.DAY_OF_WEEK) - c.get(Calendar.DAY_OF_WEEK));
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        return (c.getTime().getTime() - Calendar.getInstance().getTime().getTime()) / 1000;
    }


    // tính từ hiện tại đến mốc time + thêm ngày -> kết quả tính bằng giây
    public static long getSecondsToNextMonth() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DATE, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        return (c.getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) / SECOND2_MILLI_SECOND;
    }


    public static String getDate_ddMMyyyy(Date date) {
        return ddMMyyyy.format(date);
    }

}
