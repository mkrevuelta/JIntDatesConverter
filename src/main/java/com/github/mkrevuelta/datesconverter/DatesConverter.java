/*
    DatesConverter.java

    Copyright (c) 2020, Martin Knoblauch Revuelta
    See accompanying LICENSE

    https://github.com/mkrevuelta/JavaIntDatesConverter
*/

package com.github.mkrevuelta.datesconverter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class translates calendar dates to and from Excel dates
 * &mdash;number of days since the start of 1900 (*)&mdash; using plain-old,
 * lightning-fast, integer arithmetics.
 * <p>
 * It uses also a couple of lookup tables which are initialized
 * statically.
 * <p>
 * (*): Notes about Excel dates:
 * <ul>
 * <li> <b>1900-01-00</b> is translated as <b>0</b>. Though, it is
 *      considered invalid by
 *      {@link DateOnly#isValid() DateOnly.isValid()}.
 * <li> <b>1900-01-01</b> is day <b>1</b>.
 * <li> <b>1900-02-29</b>, though not a real day, is considered valid
 *      for backward compatibility with Lotus123. It is translated as
 *      <b>60</b>, Wednesday.
 * <li> <b>1900-03-01</b> is day <b>61</b>. It was a Thursday.
 * <li> <b>9999-12-31</b> is day <b>2958465</b>. This is the maximum
 *      date for Excel. Though, this library considers valid and
 *      translates correctly all following dates up to...
 * <li> <b>5881510-07-10</b>, which is day Integer.MAX_VALUE. That is,
 *      2147483647. A pretty nice Sunday, I hope.
 * </ul>
 * 
 * @author Mart&iacute;n Knoblauch Revuelta
 * @version 1
 */
public abstract class DatesConverter
{
    /**
     * This class represents a date and only a date &mdash;no hours,
     * minutes, seconds etc.&mdash;.
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
     * a map, just use the Excel date stored in a plain old
     * <b><code>int</code></b>. This whole library is here to
     * provide a fast translation of that to/from calendar date.
     * <p>
     * Note that there is no constructor. Two "factory" methods are
     * provided instead. Their names are intended to reduce the risk
     * of messing the order of the parameters.
     * 
     * @author Mart&iacute;n Knoblauch Revuelta
     * @version 1
     */
    public static class DateOnly
    {
        public int year;  // { 1900 ... }
        public int month; // { 1 ... 12 }
        public int day;   // { 1 ... 31 }

        public static DateOnly fromYearMonthDay (
                int year,
                int month,
                int day)
        {
            DateOnly date = new DateOnly();
            date.year = year;
            date.month = month;
            date.day = day;
            return date;
        }

        public static DateOnly fromDayMonthYear (
                int day,
                int month,
                int year)
        {
            DateOnly date = new DateOnly();
            date.year = year;
            date.month = month;
            date.day = day;
            return date;
        }
        
        public boolean isValid ()
        {
            if (    year < startingYear || year > maxintYear ||
                    month < 1 || month > 12 ||
                    day < 0)
                return false;

            if (    year == maxintYear &&
                    ( month > maxintMonth ||
                      ( month == maxintMonth && day > maxintDay)))
                return false;

            if (    day >= 1 &&
                    day <= monthDays[month-1])
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

    public static Integer getExcelDayFromString_YYYY_dash_MM_dash_DD (
            String date,
            Integer defaultValue)
    {
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

    public static String getString_YYYY_dash_MM_dash_DD_FromExcelDay (
            Integer excelDay)
    {
        DateOnly date = getDateFromExcelDay (
                            excelDay != null ? excelDay : 0);

        return String.format (
                "%04d-%02d-%02d",
                date.year, date.month, date.day);
    }
}
