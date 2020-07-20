# JIntDatesConverter
Convert calendar date to/from int (Excel&reg;) date using plain old, lightning fast, integer arithmetics.

# Table of Contents
1. [What is the int (Excel&reg;) date?](#What-is-the-int-Excel-date)
2. [Application examples](#Application-examples)
3. [Alternatives](#Alternatives)
4. [Why is this library better](#Why-is-this-library-better)
5. [How to import and use JIntDatesConverter in maven projects](#How-to-import-and-use-JIntDatesConverter-in-maven-projects)
6. [More details](#More-details)

## What is the int (Excel&reg;) date?

The int (Excel&reg;) date is the number of days since the beginning of 1900, with a few considerations:

* Day 0 is, somehow, January the 0<sup>th</sup>, 1900 (&#x2724;)
* Day 1 is January the 1<sup>st</sup>, 1900
* Day 60 is February the 29<sup>th</sup>, 1900 (&sext;)
* Day 61 is Thursday, March the 1<sup>st</sup>, 1900. Since that day everything is normal and correct

&#x2724;: Day 0 is generally used as a default date, meaning ``no date specified´´.

&sext;: 1900 was **not** a leap year, but Lotus 1-2-3 considered it a leap year by mistake. Microsoft&reg; Excel&reg; [perpetuates that tradition](https://docs.microsoft.com/en-us/office/troubleshoot/excel/wrongly-assumes-1900-is-leap-year) for backward compatibility.

You can play wit this conversion in Microsoft&reg; Excel&reg;:
* Write a number in a cell, and then change the cell format to short date.
* Write a date in a cell, and then change the cell format to number.

## Application examples

#### Problem 1: What's my exact age in days?

Let's say that I was born on February the 20<sup>th</sup>, 2000 &mdash;not my real birth date, but I do feel that young!&mdash;. That was day 36576.

Well, today is June the 28<sup>th</sup>, 2020, which is day 44010.

44010 - 36576 = **7434**. That would be my age in days!! Easy, isn't it?

#### Problem 2: How many days do I have to buy a new suit or loose 5Kg?

My friends Alice and Bob are going to get married on May, the 22<sup>nd</sup>, 2021. That's day 44338. As I said, today is day 44010...

I have 44338 - 44010 = **328** days. It seems a long time. In theory I could loose those 5Kg. But if we consider Christmas, birthday parties... I'd better make plans to buy a new suit in April!

#### Problem 3: When will Unix time end?

[Unix time](https://en.wikipedia.org/wiki/Unix_time) started on January the 1<sup>st</sup>, 1970, which is day 25569.

It will last 2<sup>32</sup> = 4294967296 seconds. The number of seconds per day is 24 * 60 * 60 = 86400. Therefore, Unix time lasts 4294967296 / 86400 &asymp; 49710.27 days.

25569 + 49710 = 75279, which is **February the 7<sup>th</sup>, 2106**, early in the morning. Don't count on me for that apocalypse. I don't feel *that* young.

Oh, but this is only valid if you consider the 32 bits unsigned! If it is treated as a signed 32-bits number, the overflow occurs at 2<sup>31</sup> seconds since the beginning of 1970. That leads to... 25569 + 49710<b>/2</b> = 50424, which is **January the 19<sup>th</sup>, 2038**. Oh my!

Don't worry about that, though. The world will end even sooner. [Network Time Protocol timestamps](https://en.wikipedia.org/wiki/Network_Time_Protocol#Timestamps) will experience their 32 bit (unsigned) overflow on February the 7<sup>th</sup>, 2036. That's exactly 70 years earlier than the overflow of the unsigned Unix time because NTP timestamps are based on year 1900 instead of 1970 &mdash;but I digress.

#### Problem 4: What day of the week will it be on October the 12<sup>th</sup>, 3000

October the 12<sup>th</sup>, 3000 will be day number 402053. The remainder of 402053 divided by 7 is 1. Hence, it will be <b>Sunday</b>.

## Alternatives

The class `java.util.Date` stores a date (*and time*) as the number of milliseconds since the beginning of 1970. It has specific methods to get and set the year month and day, but they are deprecated. Instead, you should use `java.util.Calendar`. A common practice is to use `java.text.SimpleDateFormat` to parse strings containing dates. You may need to adjust it to the Lotus 1-2-3 bug. Also, beware of time zones, daylight savings and leap seconds!

Since Java 8 we have the `java.time` package. It includes the class `LocalDate`, which stores a date (only a date!). You can construct a `LocalDate` with `LocalDate.of(year,month,day)` and then get the epoch day with the method `toEpochDay()`. For the opposite translation, you can construct a `LocalDate` with `LocalDate.ofEpochDay(dayNumber)` and then get the date with `getYear()`, `getMonthValue()` and `getDayOfMonth()`. Yo will need to adjust it to 1900-based day numbers, because `LocalDate` fixes its zero day in 1970. Also, you may need to adjust it to the Lotus 1-2-3 bug. However, if you enjoy Java 8 or later, this is a reasonably good solution.

[Apache POI](https://poi.apache.org/) has a `DateUtil` class with methods to translate between date and Excel&reg; day number as a **double**.

## Why is this library better

* It is easy to use correctly and difficult to use incorrectly
* It is simple and fast because it uses integer arithmetics
* It does ``The Right Thing´´ because it uses integer arithmetics and it does **not** mess with hours, minutes, seconds etc.
* You can use it with old Java versions
* It is small and has no dependencies (except junit, for testing)

## How to import and use JIntDatesConverter in maven projects

Simply add this dependency to your `pom.xml`:

```XML
<dependency>
    <groupId>com.github.mkrevuelta</groupId>
    <artifactId>JIntDatesConverter</artifactId>
    <version>1</version>
</dependency>
```

## More details

See:
* The javadoc [documentation](https://mkrevuelta.github.io/JIntDatesConverter/docs/index.html).
* The [project page](https://github.com/mkrevuelta/JIntDatesConverter) in GitHub.
