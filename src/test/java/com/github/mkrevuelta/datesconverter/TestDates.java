/*
    TestDates.java

    Copyright (c) 2020, Martin Knoblauch Revuelta
    See accompanying LICENSE

    https://github.com/mkrevuelta/JIntDatesConverter
*/

package com.github.mkrevuelta.datesconverter;

import org.junit.Test;
import org.junit.Assert;

public class TestDates
{
    @Test
    public void doTest ()
    {
        // Check default value behavior

        // Default to a number
        Assert.assertEquals ((Integer)42, getExcelDate ("foo", 42));

        // Default to null
        Assert.assertEquals ((Integer)null, getExcelDate ("foo", null));

        // Negative Excel date to default
        Assert.assertEquals ((Integer)42, getExcelDate ("1000-01-01", 42));

        // Check isValid() against a few corner cases

        int[][] invalidDates =
        {
            { -1, 1, 1 },
            { 0, 1, 1 },
            { 1899, 12, 31 },
            { 1900, 1, 0 },      // "Zero" date
            { 1900, 0, 1 },
            { 1900, 1, -1 },
            { 1900, -1, 1 },
            { 1900, 13, 1 },
            { 5881510, 2, 29 },
            { 5881510, 7, 11 },  // The day after Integer.MAXVALUE
            { 5881510, 7, 32 },
            { 5881510, 8, 1 },
            { 5881511, 1, 1 }
        };

        for (int i=0; i<invalidDates.length; i++)
            checkYearMonthDayIsValidDate (
                            invalidDates[i][0],
                            invalidDates[i][1],
                            invalidDates[i][2],
                            false);

        int[][] validDates =
        {
            { 5881508, 2, 29 },
            { 5881509, 12, 31 },
            { 5881510, 1, 1 },
            { 5881510, 2, 28 },
            { 5881510, 6, 30 },
            { 5881510, 7, 1 },
            { 5881510, 7, 10 }   // Day Integer.MAXVALUE
        };

        for (int i=0; i<validDates.length; i++)
            checkYearMonthDayIsValidDate (
                            validDates[i][0],
                            validDates[i][1],
                            validDates[i][2],
                            true);

        // Check translation of some special dates

        String[][] datePairs =
        {
                { "1900-01-00", "0" },       // Weird Excel initial dates
                { "1900-01-01", "1" },
                { "1900-01-02", "2" },
                { "1900-01-31", "31" },
                { "1900-02-01", "32" },

                { "1900-02-28", "59" },
                { "1900-02-29", "60" },      // Lotus123 bug
                { "1900-03-01", "61" },      // (1900 considered a leap year)

                { "1900-12-30", "365" },
                { "1900-12-31", "366" },
                { "1901-01-01", "367" },

                { "1999-12-31", "36525" },
                { "2000-01-01", "36526" },
                { "2000-02-28", "36584" },
                { "2000-02-29", "36585" },   // 2000 is multiple of 400
                { "2000-03-01", "36586" },   // (thus, leap year)

                { "2020-01-01", "43831" },
                { "2020-12-31", "44196" },

                { "2036-11-21", "50000" },
                { "2091-08-25", "70000" },

                { "2099-12-31", "73050" },
                { "2100-01-01", "73051" },
                { "2100-02-28", "73109" },   // 2000 is multiple of 100
                { "2100-03-01", "73110" },   // (not a leap year)

                { "2173-10-14", "100000" },

                { "2299-12-30", "146097" },
                { "2299-12-31", "146098" },
                { "2300-01-01", "146099" },  // 400 years after 1900
                { "2300-02-28", "146157" },
                { "2300-03-01", "146158" },  // (not a leap year)

                { "2447-07-30", "200000" },
                { "3268-12-12", "500000" },
                { "4637-11-26", "1000000" },
                { "7375-10-23", "2000000" },

                { "9999-12-31", "2958465" }, // Maximum Excel date

                { "5881510-07-10", Integer.toString (Integer.MAX_VALUE) }
        };

        for (int i=0; i<datePairs.length; i++)
            Assert.assertEquals (
                    (Integer)Integer.parseInt(datePairs[i][1]),
                    getExcelDate (datePairs[i][0], null));

        for (int i=0; i<datePairs.length; i++)
            Assert.assertEquals (
                    datePairs[i][0],
                    fromExcelDate (Integer.parseInt(datePairs[i][1])));

        // Slightly invalid dates

        Assert.assertEquals ((Integer)43892, getExcelDate ("2020-02-31", null));
        Assert.assertEquals ((Integer)43892, getExcelDate ("2020-03-02", null));

        // Check every valid date in Excel dates range

        for (int excelDay=1, year=1900; year<=9999; year++)
            for (int month=1; month<=12; month++)
            {
                int monthDays =
                        month==2 ? (year==1900 ||  // Lotus123 bug
                                    year%400==0 ||
                                    (year%4==0 && year%100!=0) ? 29 : 28) :
                        month<=7 ? (month%2==0 ? 30 : 31) :
                                   (month%2==0 ? 31 : 30);

                checkYearMonthDayIsValidDate (year, month, 0, false);
                checkYearMonthDayIsValidDate (year, month, monthDays+1, false);

                checkYearMonthDayIsValidDate (year, month, 1, true);
                checkYearMonthDayIsValidDate (year, month, monthDays, true);

                for (int day=1; day<=monthDays; day++, excelDay++)
                {
                    String date = String.format (
                                    "%04d-%02d-%02d",
                                    year, month, day);

                    String computedDate = fromExcelDate (excelDay);

                    if ( ! computedDate.equals (date))
                        Assert.fail ("Excel-to-date failure at day " +
                                     excelDay + " making " + computedDate +
                                     " instead of " + date);

                    int computedExcelDay = getExcelDate (date, null);

                    if (computedExcelDay != excelDay)
                        Assert.fail ("Date-to-Excel failure at date " + date +
                                     " computing " + computedExcelDay +
                                     " instead of " + excelDay);
                }
            }

        // Check self-consistency exhaustively beyond Excel valid dates range

        final int exhaustiveTopValue = 5000000;
        for (int i=2958466; i<exhaustiveTopValue; i++)
            checkDoubleConversion (i);

        // Continue in exponential intervals until nearly Integer.MAX_VALUE

        final int rate = 10000;
        final int topvalue = Integer.MAX_VALUE - Integer.MAX_VALUE/rate;
        for (int i=exhaustiveTopValue; i<topvalue; i+=i/rate)
            checkDoubleConversion (i);

        Assert.assertEquals ((Integer)(-1), getExcelDate ("5881510-07-11", -1));
        Assert.assertEquals ((Integer)(-1), getExcelDate ("5881510-08-01", -1));
        Assert.assertEquals ((Integer)(-1), getExcelDate ("5881511-01-01", -1));
    }

    private static void checkDoubleConversion (int excelDay)
    {
        String date = fromExcelDate (excelDay);

        int computedExcelDay = getExcelDate (date, 0);

        if (computedExcelDay != excelDay)
            Assert.fail ("Double conversion failure: " +
                            excelDay + " -> " +
                            date + " -> " +
                            computedExcelDay);
    }

    private static Integer getExcelDate (
            String date,
            Integer defaultValue)
    {
        return DatesConverter.getExcelDayFromString_YYYY_dash_MM_dash_DD (
                date,
                defaultValue);
    }

    private static String fromExcelDate (
            Integer excelDate)
    {
        return DatesConverter.getString_YYYY_dash_MM_dash_DD_FromExcelDay (
                excelDate);
    }
    
    private static void checkYearMonthDayIsValidDate (
            int year,
            int month,
            int day,
            boolean expectedResult)
    {
        boolean obtainedResult = DatesConverter.DateOnly.
                fromYearMonthDay(
                        year,
                        month,
                        day ).isValid();

        if (obtainedResult != expectedResult)
            Assert.fail ("Year " + year + ", month " + month + ", day " + day +
                    ", is reported as " + (obtainedResult?"valid":"invalid") +
                    " by DatesConverter.DateOnly.isValid()");
    }
}

