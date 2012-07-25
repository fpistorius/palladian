package ws.palladian.helper.date.dates;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.nlp.StringHelper;

class DateParserLogic {
    
    int year = -1;
    int month = -1;
    int day = -1;
    int hour = -1;
    int minute = -1;
    int second = -1;
    String timezone = null;
    
    /**
     * Normalizes the date, if a format is given. <br>
     * If no day is given, it will be set to the 1st of month. <br>
     * Set the time to 00:00:00+00, if no time is given <br>
     * 
     * @return a date in format YYYY-MM-DD
     */
    void parse(String dateString, String format) {

        String[] tempArray = removeTimezone(dateString);
        timezone = tempArray[1];
        if (timezone != null) {
            dateString = tempArray[0];
        }

        String[] dateParts = new String[3];
        if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YMD_T.getFormat())) {
            String separator = "T";
            int index = dateString.indexOf(separator);
            if (index == -1) {
                separator = " ";
            }
            String[] temp = dateString.split(separator);
            setDateValues(temp[0].split(getSeparator(temp[0])), 0, 1, 2);
            setTimeValues(temp[1]);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YMD.getFormat())) {
            setDateValues(dateString.split(getSeparator(dateString)), 0, 1, 2);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YM.getFormat())) {
            setDateValues(dateString.split("-"), 0, 1, -1);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YWD.getFormat())) {
            setDateByWeekOfYear(dateString, true, true);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YWD_T.getFormat())) {
            String separator;
            if (dateString.contains("T")) {
                separator = "T";
            } else {
                separator = " ";
            }
            dateParts = dateString.split(separator);
            setDateByWeekOfYear(dateParts[0], true, true);
            setTimeValues(dateParts[1]);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YW.getFormat())) {
            setDateByWeekOfYear(dateString, false, true);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YD.getFormat())) {
            setDateByDayOfYear(dateString, true);
        } else if (format.equalsIgnoreCase(RegExp.DATE_URL_D.getFormat())) {
            setDateValues(dateString.split(getSeparator(dateString)), 0, 1, 2);
        } else if (format.equalsIgnoreCase(RegExp.DATE_URL_MMMM_D.getFormat())) {
            setDateValues(dateString.split("/"), 0, 1, 2);
        } else if (format.equalsIgnoreCase(RegExp.DATE_URL_SPLIT.getFormat())) {
            dateParts = dateString.split("/");
            int tempMonth = 0;
            try {
                year = normalizeYear(dateParts[0]);
                day = Integer.parseInt(dateParts[dateParts.length - 1]);
                tempMonth = -1;
            } catch (NumberFormatException exeption) {
                String lastField = dateParts[dateParts.length - 1];
                String[] tempDateParts = lastField.split(getSeparator(lastField));
                month = Integer.parseInt(tempDateParts[0]);
                day = Integer.parseInt(tempDateParts[1]);
            }
            if (tempMonth == -1) {
                month = Integer.parseInt(dateParts[dateParts.length - 2]);
            }

        } else if (format.equalsIgnoreCase(RegExp.DATE_URL.getFormat())) {
            setDateValues(dateString.split(getSeparator(dateString)), 0, 1, -1);
        } else if (format.equalsIgnoreCase(RegExp.DATE_EU_D_MM_Y.getFormat())) {
            String separator = getSeparator(dateString);
            setDateValues(dateString.split(separator), 2, 1, 0);
        } else if (format.equalsIgnoreCase(RegExp.DATE_USA_MM_D_Y.getFormat())) {
            setDateValues(dateString.split(getSeparator(dateString)), 2, 0, 1);
        } else if (format.equalsIgnoreCase(RegExp.DATE_EU_D_MMMM_Y.getFormat())) {
            if (dateString.contains("\\.")) {
                dateString = dateString.replaceAll("\\.", "");
            }
            if (dateString.contains("-")) {
                dateString = dateString.replaceAll("-", " ");
            }
            dateParts = dateString.split(" ");
            setDateValues(dateParts, 2, 1, 0);
        } else if (format.equalsIgnoreCase(RegExp.DATE_USA_MMMM_D_Y.getFormat())) {
            try {
                String[] parts = dateString.split(" ");
                if (parts.length == 2) {
                    String[] tempParts = new String[3];
                    tempParts[0] = parts[0].split("\\.")[0];
                    tempParts[1] = parts[0].split("\\.")[1];
                    tempParts[2] = parts[1];
                    parts = tempParts;
                }
                setDateValues(parts, 2, 0, 1);
            } catch (Exception e) {
            }

        } else if (format.equalsIgnoreCase(RegExp.DATE_USA_MMMM_D_Y_SEP.getFormat())) {
            setDateValues(dateString.split("-"), 2, 0, 1);
        } else if (format.equalsIgnoreCase(RegExp.DATE_EUSA_MMMM_Y.getFormat())) {
            setDateValues(dateString.split(" "), 1, 0, -1);
        } else if (format.equalsIgnoreCase(RegExp.DATE_EUSA_YYYY_MMM_D.getFormat())) {
            setDateValues(dateString.split("-"), 0, 1, 2);
        } else if (format.equalsIgnoreCase(RegExp.DATE_EU_MM_Y.getFormat())) {
            String separator = getSeparator(dateString);
            setDateValues(dateString.split(separator), 1, 0, -1);
        } else if (format.equalsIgnoreCase(RegExp.DATE_EU_D_MM.getFormat())) {
            String separator = getSeparator(dateString);
            setDateValues(dateString.split(separator), -1, 1, 0);
        } else if (format.equalsIgnoreCase(RegExp.DATE_EU_D_MMMM.getFormat())) {
            /*
             * int index = dateString.indexOf(".");
             * if (index == -1) {
             * setDateValues(dateString.split(" "), -1, 1, 0);
             * } else {
             * setDateValues(dateString.split("\\."), -1, 1, 0);
             * }
             */
            dateString = dateString.replaceAll("\\.", "");
            setDateValues(dateString.split(" "), -1, 1, 0);

        } else if (format.equalsIgnoreCase(RegExp.DATE_USA_MM_D.getFormat())) {
            setDateValues(dateString.split("/"), -1, 0, 1);
        } else if (format.equalsIgnoreCase(RegExp.DATE_USA_MMMM_D.getFormat())) {
            setDateValues(dateString.split(" "), -1, 0, 1);
        } else if (format.equalsIgnoreCase(RegExp.DATE_USA_MM_Y.getFormat())) {
            setDateValues(dateString.split("/"), 1, 0, -1);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ANSI_C.getFormat())) {
            dateParts = dateString.split(" ");
            setDateValues(dateParts, 4, 1, 2);
            setTimeValues(dateParts[3]);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ANSI_C_TZ.getFormat())) {
            dateParts = dateString.split(" ");
            setDateValues(dateParts, 4, 1, 2);
            setTimeValues(dateParts[3] + dateParts[5]);
        } else if (format.equalsIgnoreCase(RegExp.DATE_RFC_1123.getFormat())) {
            dateParts = dateString.split(" ");
            setDateValues(dateParts, 3, 2, 1);
            setTimeValues(dateParts[4]);
        } else if (format.equalsIgnoreCase(RegExp.DATE_RFC_1036.getFormat())) {
            String parts[] = dateString.split(" ");
            setDateValues(parts[1].split("-"), 2, 1, 0);
            setTimeValues(parts[2]);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YMD_NO.getFormat())) {
            year = Integer.parseInt(dateString.substring(0, 4));
            month = Integer.parseInt(dateString.substring(4, 6));
            day = Integer.parseInt(dateString.substring(6, 8));
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YWD_NO.getFormat())) {
            setDateByWeekOfYear(dateString, true, false);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YW_NO.getFormat())) {
            setDateByWeekOfYear(dateString, false, false);
        } else if (format.equalsIgnoreCase(RegExp.DATE_ISO8601_YD_NO.getFormat())) {
            setDateByDayOfYear(dateString, false);
        } else if (format.equalsIgnoreCase(RegExp.DATE_RFC_1123_UTC.getFormat())) {
            dateParts = dateString.split(" ");
            setDateValues(dateParts, 3, 2, 1);
            setTimeValues(dateParts[4] + dateParts[5]);
        } else if (format.equalsIgnoreCase(RegExp.DATE_RFC_1036_UTC.getFormat())) {
            String parts[] = dateString.split(" ");
            setDateValues(parts[1].split("-"), 2, 1, 0);
            setTimeValues(parts[2] + parts[3]);
        } else if (format.equalsIgnoreCase(RegExp.DATE_EU_D_MM_Y_T.getFormat())) {

            String meridiem = hasAmPm(dateString);
            if (meridiem != null) {
                dateString = removeAmPm(dateString, meridiem);
            }
            String[] parts = dateString.split(" ");

            String separator = getSeparator(parts[0]);
            String[] date = parts[0].split(separator);
            setDateValues(date, 2, 1, 0);
            StringBuilder builder = new StringBuilder();
            for (int i = 1; i < parts.length; i++) {
                if (!parts[i].contains("/")) {
                    builder.append(parts[i]);
                }
            }
            setTimeValues(builder.toString());
            set24h(meridiem);
        } else if (format.equalsIgnoreCase(RegExp.DATE_EU_D_MMMM_Y_T.getFormat())) {

            String meridiem = hasAmPm(dateString);
            if (meridiem != null) {
                dateString = removeAmPm(dateString, meridiem);
            }
            if (dateString.contains("-")) {
                dateString = dateString.replaceAll("-", " ");
            }
            String[] parts = dateString.split(" ");
            setDateValues(parts, 2, 1, 0);
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 3; i < parts.length; i++) {
                if (!parts[i].contains("/")) {
                    stringBuilder.append(parts[i]);
                }
            }
            setTimeValues(stringBuilder.toString());
            set24h(meridiem);
        } else if (format.equalsIgnoreCase(RegExp.DATE_USA_MM_D_Y_T.getFormat())) {

            String meridiem = hasAmPm(dateString);
            if (meridiem != null) {
                dateString = removeAmPm(dateString, meridiem);
            }
            String[] parts = dateString.split(" ");
            String separator = getSeparator(parts[0]);
            String[] date = parts[0].split(separator);
            setDateValues(date, 2, 0, 1);
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 1; i < parts.length; i++) {
                if (!parts[i].contains("/")) {
                    stringBuilder.append(parts[i]);
                }
            }
            setTimeValues(stringBuilder.toString());
            set24h(meridiem);
        } else if (format.equalsIgnoreCase(RegExp.DATE_USA_MMMM_D_Y_T.getFormat())) {

            String meridiem = hasAmPm(dateString);
            if (meridiem != null) {
                dateString = removeAmPm(dateString, meridiem);
            }
            String[] parts = dateString.split(" ");
            setDateValues(parts, 2, 0, 1);
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 3; i < parts.length; i++) {
                if (!parts[i].contains("/")) {
                    stringBuilder.append(parts[i]);
                }
            }
            setTimeValues(stringBuilder.toString());
            set24h(meridiem);
        } else if (format.equalsIgnoreCase(RegExp.DATE_CONTEXT_YYYY.getFormat())) {
            year = Integer.valueOf(dateString);
        }
    }
    
    /**
     * Removes PM and AM from a string and delete double whitespace.
     * 
     * @param text String to be cleared.
     * @param meridiem AM or PM
     * @return Cleared string.
     */
    private String removeAmPm(String text, String meridiem) {
        String newText = text.replaceAll(meridiem, "");
        return newText.replaceAll("  ", " ");
    }

    /**
     * Checks for AM and PM in a string and returns the found.
     * 
     * @param text String to b checked.
     * @return Am or PM.
     */
    private static String hasAmPm(String text) {
        if (text.contains("am")) {
            return "am";
        }
        if (text.contains("AM")) {
            return "AM";
        }
        if (text.contains("pm")) {
            return "pm";
        }
        if (text.contains("PM")) {
            return "PM";
        }
        return null;
    }

    /**
     * If this date has a hour and the date-string has AM or PM values, the hour will be changed in 24h system.
     * 
     * @param meridiem Am or PM
     */
    private void set24h(String meridiem) {
        if (hour != -1 && meridiem != null) {
            if (meridiem.equalsIgnoreCase("pm")) {
                if (hour > 0 && hour < 12) {
                    hour += 12;
                }
            } else if (meridiem.equalsIgnoreCase("am") && hour == 12) {
                hour = 0;
            }
        }
    }

    /**
     * If a date is given by week and day, the normal date (day, month and year) will be calculated.
     * If no day is given, the first day of week will be set.
     * Using ISO8601 standard ( First week of a year has four or more days; first day of a week is Monday)
     * 
     * @param withDay flag for the cases, that a day is given or not
     * @param withSeparator Is there a separator in the dateString?
     * 
     */
    private void setDateByWeekOfYear(String dateString, boolean withDay, boolean withSeparator) {
        String[] dateParts = new String[3];

        if (withSeparator) {
            dateParts = dateString.split("-");
        } else {
            dateParts[0] = dateString.substring(0, 4);
            dateParts[1] = dateString.substring(4, 7);
            if (withDay) {
                dateParts[2] = dateString.substring(7, 8);
            }
        }

        Calendar calendar = new GregorianCalendar();

        calendar.setMinimalDaysInFirstWeek(4);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(Calendar.YEAR, Integer.parseInt(dateParts[0]));
        calendar.set(Calendar.WEEK_OF_YEAR, Integer.parseInt(dateParts[1].substring(1)));
        if (withDay) {
            calendar.set(Calendar.DAY_OF_WEEK, Integer.parseInt(dateParts[2]));
        } else {
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        }
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH) + 1;
        if (withDay) {
            day = calendar.get(Calendar.DAY_OF_MONTH);
        }

    }

    /**
     * If a date is given by year and day of year, the date (day, month and year) will be calculated.
     * Using ISO8601 standard ( First week of a year has four or more days; first day of a week is Monday)
     * 
     * @param withSeparator Is there a separator in the dateString?
     * 
     * 
     */
    private void setDateByDayOfYear(String dateString, boolean withSeparator) {
        Calendar calendar = new GregorianCalendar();
        calendar.setMinimalDaysInFirstWeek(4);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        if (withSeparator) {
            String[] dateParts = dateString.split("-");
            calendar.set(Calendar.YEAR, Integer.parseInt(dateParts[0]));
            calendar.set(Calendar.DAY_OF_YEAR, Integer.parseInt(dateParts[1]));

        } else {
            calendar.set(Calendar.YEAR, Integer.parseInt(dateString.substring(0, 4)));
            calendar.set(Calendar.DAY_OF_YEAR, Integer.parseInt(dateString.substring(4)));
        }

        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH) + 1;
        day = calendar.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * <b>!!! IMPORTANT: call <u>AFTER</u> setting dateparts like year, month and day !!! </b><br>
     * <br>
     * 
     * Set hour, minute and second, if string is in one of the following forms: <br>
     * <i>HH:MM:SS</i>, <i>HH:MM</i> or <i>HH</i> and has optional a difference to UTC in on of the following formats: <br>
     * <i>+|- HH:MM</i>, <i>+|- HH</i> or "<i>Z</i>" (stands for UTC timezone).
     * 
     * @param time
     */
    private void setTimeValues(String time) {
        String actualTime = time;
        String diffToUtc = null;
        // int index = actualTime.indexOf('.');
        if (actualTime.contains(".")) {
            String regExp = "\\.(\\d)*";
            Pattern pattern = Pattern.compile(regExp);
            Matcher matcher = pattern.matcher(actualTime);
            if (matcher.find()) {
                actualTime = actualTime.replaceAll(regExp, "");
            }
        }

        String separator = null;
        if (time.contains("Z")) {
            separator = "Z";
        } else if (time.contains("+")) {
            separator = "\\+";
        } else if (time.contains("-")) {
            separator = "-";
        }
        
        String cleanedTime = actualTime;
        if (separator != null) {
            cleanedTime = actualTime.split(separator)[0];
            if (!separator.equalsIgnoreCase("Z")) {
                diffToUtc = actualTime.split(separator)[1];
            }
        }
        setActualTimeValues(cleanedTime);
        if (diffToUtc != null) {
            setTimeDiff(diffToUtc, separator);
        }
    }

    /**
     * Refreshes the time with given difference to UTC. <br>
     * E.g.: time is 14:00 and difference to UTC is +02:00 -> new time is 12:00. <br>
     * <br>
     * If only one of hour, day, month or year is not set, we can not calculate a difference to UTC, because we do not
     * know following or previous date. <br>
     * E.g.: if year is unknown: what day is following on February 28th? February 29th or March 1st? <br>
     * If day is unknown, what is following on 23:59:59?
     * 
     * @param time must have format: HH:MM or HH.
     * @param sign must be + or -
     */
    private void setTimeDiff(String time, String sign) {
        if (year == -1 || month == -1 || day == -1 || hour == -1) {
            return;
        }
        int tempHour;
        int tempMinute = 0;
        if (!time.contains(":")) {
            if (time.length() == 4) {
                tempHour = Integer.parseInt(time.substring(0, 2));
                tempMinute = Integer.parseInt(time.substring(2, 4));
            } else {
                tempHour = Integer.parseInt(time);
            }
        } else {
            String[] timeParts = time.split(":");
            tempHour = Integer.parseInt(timeParts[0]);
            tempMinute = Integer.parseInt(timeParts[1]);
        }

        int tempMinute2 = 0;
        if (minute != -1) {
            tempMinute2 = minute;

        }
        Calendar calendar = new GregorianCalendar(year, month - 1, day, hour, tempMinute2);

        if (sign.equalsIgnoreCase("-")) {
            calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) + tempHour);
            calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) + tempMinute);
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) - tempHour);
            calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) - tempMinute);
        }
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH) + 1;
        day = calendar.get(Calendar.DAY_OF_MONTH);
        hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (minute != -1 || tempMinute != 0) {
            minute = calendar.get(Calendar.MINUTE);
        }

    }

    /**
     * Sets hour, minute and second by extracting from time-string. <br>
     * There for the time-string must have one the following forms: HH:MM:SS or HH:MM or HH. <br>
     * Only the given parts will be set.
     * 
     * @param time must have one of the following forms: HH:MM:SS or HH:MM or HH.
     */
    private void setActualTimeValues(String time) {
        if (!time.isEmpty() && !time.contains(":")) {
            hour = Integer.parseInt(time);

        } else {
            String[] timeParts = time.trim().split(":");
            if (timeParts.length > 0 && !timeParts[0].isEmpty()) {
                hour = Integer.parseInt(timeParts[0]);
                if (timeParts.length > 1) {
                    minute = Integer.parseInt(timeParts[1]);
                    if (timeParts.length > 2) {
                        second = Integer.parseInt(timeParts[2]);
                    }
                }
            }
        }
    }

    /**
     * Sets the year, month and day of this date by getting a array with this values and the position of each value in
     * the array.
     * 
     * @param dateParts The array with date-parts.
     * @param yearPos Position of year in the date-array.
     * @param monthPos Position of month in the date-array.
     * @param dayPos Position of day in the date-array.
     */
    private void setDateValues(String[] dateParts, int yearPos, int monthPos, int dayPos) {
        if (yearPos != -1) {
            try {
                year = normalizeYear(dateParts[yearPos]);
            } catch (Exception e) {
                // LOGGER.error(e.getMessage());
            }
        }
        if (monthPos != -1) {
            dateParts[monthPos] = dateParts[monthPos].replace(" ", "");
            try {
                dateParts[monthPos] = String.valueOf(Integer.parseInt(dateParts[monthPos]));
            } catch (NumberFormatException e) {
                dateParts[monthPos] = DateHelper.monthNameToNumber(dateParts[monthPos]);
            }
            if (dateParts[monthPos] != null) {
                month = Integer.parseInt(dateParts[monthPos]);
            }

        }
        if (dayPos != -1) {
            day = Integer.parseInt(removeNoDigits(dateParts[dayPos]));
        }
    }

    /**
     * Normalizes a year. Removes apostrophe (e.g. '99) and makes it four digit.
     * 
     * @param year
     * @return A four digit year.
     */
    static int normalizeYear(String year) {
        return get4DigitYear(Integer.parseInt(removeNoDigits(year)));
    }

    /**
     * Sets the year in 4 digits format. <br>
     * E.g.: year = 12; current year = 2010 -> year > 10 -> 1912 <br>
     * year = 7; current year = 2010 -> year < 10 -> 2007 <br>
     * year = 10; current year = 2010 -> year > 10 -> 2010 <br>
     * year = 99; current year = 2010 -> year > 10 -> 1999
     * 
     * @param date
     * @return
     */
    static int get4DigitYear(int year) {
        int longYear = year;
        if (year < 100) {
            int currentYear = new GregorianCalendar().get(Calendar.YEAR);
            if (year > currentYear - 2000) {
                longYear = year + 1900;
            } else {
                longYear = year + 2000;
            }
        }
        return longYear;
    }

    /**
     * Removes the symbols "'" from Year '99 and "," from Day 03, June.
     * 
     * @param date
     * @return the entered date without the symbols
     */
    static String removeNoDigits(String datePart) {
        String result = datePart;
        int index = result.indexOf('\'');
        if (index != -1) {
            result = result.substring(index + 1, datePart.length());
        }
        index = result.indexOf(',');
        if (index != -1) {
            result = result.substring(0, index);
        }

        index = result.indexOf(".");
        if (index != -1) {
            result = result.substring(0, index);
        }

        index = result.indexOf("th");
        if (index == -1) {
            index = result.indexOf("st");
            if (index == -1) {
                index = result.indexOf("nd");
                if (index == -1) {
                    index = result.indexOf("rd");
                }
            }
        }
        if (index != -1) {
            result = result.substring(0, index);
        }

        // remove everything after a break
        result = result.replaceAll("\n.*", "");

        return result;
    }

    /**
     * Removes timezone acronyms.
     * 
     * @param dateString
     * @return
     */
    static String[] removeTimezone(String dateString) {
        return StringHelper.removeFirstStringpart(dateString, RegExp.TIMEZONE);
    }

    /**
     * 
     * @param text a date, where year, month and day are separated by . / or _
     * @return the separating symbol
     */
    static String getSeparator(String text) {
        if (text.contains(".")) {
            return "\\.";
        }
        if (text.contains("/")) {
            return "/";
        }
        if (text.contains("_")) {
            return "_";
        }
        if (text.contains("-")) {
            return "-";
        }
        return null;
    }

}
