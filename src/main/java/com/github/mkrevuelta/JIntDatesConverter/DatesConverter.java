/*
    DatesConverter.java

    Copyright (c) 2020, Martin Knoblauch Revuelta
    See accompanying LICENSE

    https://github.com/mkrevuelta/JIntDatesConverter
*/

package com.github.mkrevuelta.JIntDatesConverter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class translates calendar dates to and from Excel&reg; dates
 * &mdash;number of days since the start of year 1900 (*)&mdash;
 * using plain-old, lightning-fast, integer arithmetics.
 * <p>
 * It uses also a Pattern object and a few lookup tables which are
 * initialized statically. They are never modified. Thus, the
 * library is safe for multithread use.
 * <p>
 * (*): Notes about Excel&reg; dates:
 * <ul>
 * <li> <b>1900-01-00</b> is translated as <b>0</b>. Though, it is
 *      considered invalid by
 *      {@link DateOnly#isValid() DateOnly.isValid()}.
 * <li> <b>1900-01-01</b> is day <b>1</b>.
 * <li> <b>1900-02-29</b>, though not a real day, is considered
 *      valid for backward compatibility with Lotus123. It is
 *      translated as <b>60</b>, Wednesday.
 * <li> <b>1900-03-01</b> is day <b>61</b>. It was a Thursday.
 * <li> <b>9999-12-31</b> is day <b>2958465</b>. This is the maximum
 *      date for Excel&reg;. Though, this library considers valid
 *      and translates correctly all following dates up to...
 * <li> <b>5881510-07-10</b>, which is day Integer.MAX_VALUE. That
 *      is, 2147483647. A pretty nice Sunday, I hope, for whoever
 *      that walks the Earth by then.
 * </ul>
 * <p>
 * See the <a target="_blank"
 * href="https://github.com/mkrevuelta/JIntDatesConverter">project
 * page</a> in GitHub
 * 
 * @author Mart&iacute;n Knoblauch Revuelta
 * @version 1
 */
public abstract class DatesConverter
{
    private DatesConverter()  // This is an abstract class!
    {}                        // Make it somehow final too

    /**
     * This class represents a date and only a date &mdash;no hours,
     * minutes, seconds, milliseconds...&mdash;.
     * <p>
     * I felt tempted to provide it with all the bells and whistles
     * (Comparable interface, toString() overload...). Then I
     * considered using a smaller type for month and day, just in
     * case a lot of objects are stored. That would raise the
     * problem of narrowing casts... But this is all beyond the
     * purpose of this class and, in fact, it is not necessary
     * at all.
     * <p>
     * If you want to store a lot of dates, or use them as keys in
     * a map, just use the Excel&reg; date stored in a plain old
     * <b><code>int</code></b>. This whole library is here to
     * provide a fast translation of that to/from calendar date.
     * <p>
     * Note that no public constructor accepts parameters. Two
     * "factory" methods are provided instead. Their names are
     * intended to mitigate the risk of messing parameters order.
     * 
     * @author Mart&iacute;n Knoblauch Revuelta
     * @version 1
     */
    public static class DateOnly
    {
        /**
         * Year number.
         * Valid year numbers range from 1900 to 5881510.
         */
        public int year;

        /**
         * Month of the year.
         * Valid month numbers range from 1 to 12.
         */
        public int month;

        /**
         * Day of the month.
         * Valid day numbers range from 1 to 31.
         */
        public int day;

        private DateOnly (int year, int month, int day)
        {
            this.year = year;
            this.month = month;
            this.day = day;
        }

        /**
         * Constructs an object initializing it with the default
         * date 1900-01-00, which is the Excel&reg; day number 0.
         */
        public DateOnly ()
        {
            this (startingYear, 1, 0);
        }

        /**
         * This function constructs and populates a
         * {@link DateOnly DateOnly} object
         *
         *  @param year Year number, from 1900 to 5881510
         *  @param month Month of the year, from 1 to 12
         *  @param day Day of the month, from 1 to 31
         *  @return {@link DateOnly DateOnly} object for the
         *          specified date
         */
        public static DateOnly fromYearMonthDay (
                int year,
                int month,
                int day)
        {
            return new DateOnly(year, month, day);
        }

        /**
         * This function constructs and populates a
         * {@link DateOnly DateOnly} object
         * 
         *  @param day Day of the month, from 1 to 31
         *  @param month Month of the year, from 1 to 12
         *  @param year Year number, from 1900 to 5881510
         *  @return {@link DateOnly DateOnly} object for the
         *          specified date
         */
        public static DateOnly fromDayMonthYear (
                int day,
                int month,
                int year)
        {
            return new DateOnly(year, month, day);
        }

        /**
         * This method checks whether the date is strictly a valid
         * calendar date considering all details, such as days of
         * each month and leap years.
         * <p>
         * Special remarks:
         * <ul>
         * <li> <b>1900-01-00</b> (day <b>0</b>) is <b>not</b> valid.
         * <li> <b>1900-02-29</b> (day <b>60</b>), though not a real
         *      day, is considered valid for backward compatibility
         *      with Lotus123.
         * </ul> 
         * @return <code>true</code> if the date is valid,
         *         <code>false</code> otherwise
         */
        public boolean isValid ()
        {
            if (    year < startingYear || year > maxintYear ||
                    month < 1 || month > 12 ||
                    day < 1)
                return false;

            if (    year == maxintYear &&
                    ( month > maxintMonth ||
                      ( month == maxintMonth && day > maxintDay)))
                return false;

            if (    day <= monthDays[month-1])
                return true;
            
            return month == 2 && day == 29 &&
                   ( isLeapYear (year) ||
                     year == startingYear ); // Embrace Lotus123 bug
        }
    }

    private static final int tetraCenturyDays =
            400 * 365
            + 100     // 1/4 of those 400 are leap years,
            - 4       // except multiples of 100, which are not,
            + 1;      // but the multiple of 400, which is

    private static final int startingYear = 1900;

    private static final int[] monthDays = new int[]
        { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

    private static final int[] monthOffsets = new int[12];
    static
    {
        monthOffsets[0] = 0;

        for (int i=1; i<12; i++)
            monthOffsets[i] = monthOffsets[i-1] + monthDays[i-1];
    }

    private static final int[] tetraCenturyOffsets = new int[400];
    static
    {
        tetraCenturyOffsets[0] = 0;

        for (int i=1; i<400; i++)
            tetraCenturyOffsets[i] = tetraCenturyOffsets[i-1] +
                                     ( isLeapYear(startingYear+i-1) ?
                                       366 : 365 );
    }

    private static boolean isLeapYear (int year)
    {
        return (year%4 == 0 && year%100 != 0) || year%400 == 0;
    }

    private static final int maxintYear = 5881510;
    private static final int maxintMonth = 7;
    private static final int maxintDay = 10;

    /**
     * This function translates a calendar date (year, month,
     * day) from a {@link DateOnly DateOnly} object to Excel&reg;
     * day number.
     * <p>
     * This translation is slightly tolerant with some forms of
     * invalid dates. The date 2020-02-31, for instance, is
     * translated as 43892, which corresponds to 2020-03-02.
     * <p>
     * Also, the special date 1900-01-00, though not strictly
     * valid, is translated as day <b>0</b>.
     * 
     * @param date the {@link DateOnly DateOnly} object to be
     *             translated
     * @return the resulting Excel&reg; day number if the input
     *         date is valid, -1 otherwise
     */
    public static int getExcelDayFromDate (DateOnly date)
    {
        int year = date.year;
        int month = date.month;
        int day = date.day;

        if (    year == startingYear && month == 1 && day == 0)
            return 0;

        if (    year < startingYear || year > maxintYear ||
                month < 1 || month > 12 ||
                day < 1 || day > 31)
            return -1;

        if (year == maxintYear)
        {
            if (    month > maxintMonth ||
                    (month == maxintMonth && day > maxintDay))
                return -1;
        }

        boolean isLeap = isLeapYear (year);

        month --;
        day --;
        year -= startingYear;

        int absDay = (year / 400) * tetraCenturyDays +
                     tetraCenturyOffsets[year%400];

        absDay += monthOffsets[month] + day;

        if (isLeap && month > 1)
            absDay ++;

        if (year > 0 || month > 1)  // Embrace Lotus123 bug
            absDay ++;

        return absDay + 1;
    }

    /**
     * Translates from an Excel day number to a calendar date
     * (year, month, day) in the form of a
     * {@link DateOnly DateOnly} object.
     * <p>
     * The Excel&reg; day 0 is translated as 1900-01-00, which is
     * not strictly valid.
     * <p>
     * Negative day numbers are also translated as 1900-01-00.
     * 
     * @param excelDay Excel&reg; day number
     * @return corresponding calendar date as a
     *         {@link DateOnly DateOnly} object if the input is
     *         greater than 0, the default
     *         {@link DateOnly DateOnly} (1900-01-00) otherwise
     */
    public static DateOnly getDateFromExcelDay (int excelDay)
    {
        if (excelDay < 1)
            return DateOnly.fromDayMonthYear (
                    0, 1, startingYear);

        if (excelDay <= 31)
            return DateOnly.fromDayMonthYear (
                    excelDay, 1, startingYear);

        // Embrace Lotus123 bug
        if (excelDay <= 60)
            return DateOnly.fromDayMonthYear (
                    excelDay-31, 2, startingYear);

        // 0-based day, considering also Lotus123 bug
        int day = excelDay - 2;

        int year = (day / tetraCenturyDays) * 400;
        day %= tetraCenturyDays;

        int tcYear = day / 366;
        
        if (tcYear < 399 && day >= tetraCenturyOffsets[tcYear+1])
            tcYear ++;

        year += tcYear;
        day -= tetraCenturyOffsets[tcYear];

        int month = 0;
        boolean isLeap = isLeapYear (year+startingYear);

        if (day >= 60 || !isLeap)
        {
            if (isLeap)
                day --;

            month = day / 31;

            if (month < 11 && day >= monthOffsets[month+1])
                month ++;

            day -= monthOffsets[month];
        }
        else if (day >= 31)
        {
            month = 1;
            day -= 31;
        }

        return DateOnly.fromDayMonthYear (
                day+1, month+1, year+startingYear);
    }

    private static final Pattern patternDashes =
            Pattern.compile ("^(\\d+)-(\\d+)-(\\d+)$");

    /**
     * This function is a convenience wrapper for
     * {@link #getExcelDayFromDate getExcelDayFromDate()}.
     * It works with dates in strings with the format
     * <i>yyyy-MM-dd</i> instead of {@link DateOnly DateOnly}
     * objects. In addition, the caller can specify the default
     * value to be returned when the parsing fails or the date is
     * invalid.
     *
     * @param date date string in <i>yyyy-MM-dd</i> format
     * @param defaultValue special value to be returned if the
     *                     parsing fails or the date is invalid
     * @return the resulting Excel&reg; day number if the input is
     *         a valid date, {@code defaultValue} otherwise
     * @see #getExcelDayFromDate
     */
    public static Integer
        getExcelDayFromString_YYYY_dash_MM_dash_DD (
            String date,
            Integer defaultValue)
    {
        if (date == null)
            return defaultValue;

        try
        {
            Matcher matcher = patternDashes.matcher (date);

            if ( ! matcher.matches())
                return defaultValue;

            int excelDay =
                    getExcelDayFromDate (
                        DateOnly.fromYearMonthDay (
                            Integer.parseInt(matcher.group(1)),
                            Integer.parseInt(matcher.group(2)),
                            Integer.parseInt(matcher.group(3))));

            return excelDay < 0 ? defaultValue : (Integer)excelDay;
        }
        catch (NumberFormatException ex)
        {
            return defaultValue;
        }
    }

    /**
     * This function is a convenience wrapper for
     * {@link #getDateFromExcelDay getDateFromExcelDay()}.
     * It returns dates in strings with the format
     * <i>yyyy-MM-dd</i> instead of {@link DateOnly DateOnly}
     * objects.
     *
     * @param excelDay Excel&reg; day number
     * @return corresponding calendar date string in
     *         <i>yyyy-MM-dd</i> format if the input is greater
     *         than 0, "1900-01-00" otherwise.
     * @see #getDateFromExcelDay
     */
    public static String
        getString_YYYY_dash_MM_dash_DD_FromExcelDay (
            Integer excelDay)
    {
        DateOnly date = getDateFromExcelDay (
                            excelDay != null ? excelDay : 0);

        return String.format (
                "%04d-%02d-%02d",
                date.year, date.month, date.day);
    }
}
