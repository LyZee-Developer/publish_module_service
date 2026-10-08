package org.module.publish_service.util;

import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.Objects;

/**
 * @Author : Ly LeangSeng
 * @Email : lyleangseng712@gmail.com
 * @Date : 10/3/2026 5:55 PM
 */
@Component
public final class DateUtil {

    // =========================================================
    // CONSTANTS
    // =========================================================

    public static final ZoneId PHNOM_PENH_ZONE = ZoneId.of("Asia/Phnom_Penh");

    public static final String DATE_PATTERN = "yyyy-MM-dd";

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static final String DISPLAY_DATE_PATTERN = "dd/MM/yyyy";

    public static final String DISPLAY_DATE_TIME_PATTERN = "dd/MM/yyyy hh:mm a";

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    // =========================================================
    // CURRENT DATE / TIME
    // =========================================================

    public static LocalDate today() {
        return LocalDate.now();
    }

    public static LocalDate today(ZoneId zoneId) {
        return LocalDate.now(zoneId);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    public static LocalDateTime now(ZoneId zoneId) {
        return LocalDateTime.now(zoneId);
    }

    public static ZonedDateTime zonedNow() {
        return ZonedDateTime.now(PHNOM_PENH_ZONE);
    }

    public static ZonedDateTime zonedNow(ZoneId zoneId) {
        return ZonedDateTime.now(zoneId);
    }

    public static Instant instantNow() {
        return Instant.now();
    }

    // =========================================================
    // YESTERDAY / TOMORROW
    // =========================================================

    public static LocalDate yesterday() {
        return today().minusDays(1);
    }

    public static LocalDate tomorrow() {
        return today().plusDays(1);
    }

    public static LocalDateTime yesterdayDateTime() {
        return now().minusDays(1);
    }

    public static LocalDateTime tomorrowDateTime() {
        return now().plusDays(1);
    }

    // =========================================================
    // ADD DATE / TIME
    // =========================================================

    public static LocalDate addDays(LocalDate date, long days) {
        return requireDate(date).plusDays(days);
    }

    public static LocalDate addWeeks(LocalDate date, long weeks) {
        return requireDate(date).plusWeeks(weeks);
    }

    public static LocalDate addMonths(LocalDate date, long months) {
        return requireDate(date).plusMonths(months);
    }

    public static LocalDate addYears(LocalDate date, long years) {
        return requireDate(date).plusYears(years);
    }

    public static LocalDateTime addHours(LocalDateTime dateTime, long hours) {
        return requireDateTime(dateTime).plusHours(hours);
    }

    public static LocalDateTime addMinutes(LocalDateTime dateTime, long minutes) {
        return requireDateTime(dateTime).plusMinutes(minutes);
    }

    public static LocalDateTime addSeconds(LocalDateTime dateTime, long seconds) {
        return requireDateTime(dateTime).plusSeconds(seconds);
    }

    // =========================================================
    // SUBTRACT DATE / TIME
    // =========================================================

    public static LocalDate subtractDays(LocalDate date, long days) {
        return requireDate(date).minusDays(days);
    }

    public static LocalDate subtractWeeks(LocalDate date, long weeks) {
        return requireDate(date).minusWeeks(weeks);
    }

    public static LocalDate subtractMonths(LocalDate date, long months) {
        return requireDate(date).minusMonths(months);
    }

    public static LocalDate subtractYears(LocalDate date, long years) {
        return requireDate(date).minusYears(years);
    }

    public static LocalDateTime subtractHours(LocalDateTime dateTime, long hours) {
        return requireDateTime(dateTime).minusHours(hours);
    }

    public static LocalDateTime subtractMinutes(LocalDateTime dateTime, long minutes) {
        return requireDateTime(dateTime).minusMinutes(minutes);
    }

    public static LocalDateTime subtractSeconds(LocalDateTime dateTime, long seconds) {
        return requireDateTime(dateTime).minusSeconds(seconds);
    }

    // =========================================================
    // START / END OF DAY
    // =========================================================

    public static LocalDateTime startOfDay(LocalDate date) {
        return requireDate(date).atStartOfDay();
    }

    public static LocalDateTime endOfDay(LocalDate date) {
        return requireDate(date).atTime(LocalTime.MAX);
    }

    public static LocalDateTime startOfToday() {
        return startOfDay(today());
    }

    public static LocalDateTime endOfToday() {
        return endOfDay(today());
    }

    // =========================================================
    // START / END OF MONTH
    // =========================================================

    public static LocalDate startOfMonth(LocalDate date) {
        return requireDate(date).with(TemporalAdjusters.firstDayOfMonth());
    }

    public static LocalDate endOfMonth(LocalDate date) {
        return requireDate(date).with(TemporalAdjusters.lastDayOfMonth());
    }

    public static LocalDate startOfCurrentMonth() {
        return startOfMonth(today());
    }

    public static LocalDate endOfCurrentMonth() {
        return endOfMonth(today());
    }

    // =========================================================
    // START / END OF YEAR
    // =========================================================

    public static LocalDate startOfYear(LocalDate date) {
        return requireDate(date).with(TemporalAdjusters.firstDayOfYear());
    }

    public static LocalDate endOfYear(LocalDate date) {
        return requireDate(date).with(TemporalAdjusters.lastDayOfYear());
    }

    public static LocalDate startOfCurrentYear() {
        return startOfYear(today());
    }

    public static LocalDate endOfCurrentYear() {
        return endOfYear(today());
    }

    // =========================================================
    // DATE COMPARISON
    // =========================================================

    public static boolean isToday(LocalDate date) {
        return Objects.equals(date, today());
    }

    public static boolean isYesterday(LocalDate date) {
        return Objects.equals(date, yesterday());
    }

    public static boolean isTomorrow(LocalDate date) {
        return Objects.equals(date, tomorrow());
    }

    public static boolean isBefore(LocalDate date1, LocalDate date2) {
        return requireDate(date1).isBefore(requireDate(date2));
    }

    public static boolean isAfter(LocalDate date1, LocalDate date2) {
        return requireDate(date1).isAfter(requireDate(date2));
    }

    public static boolean isEqual(LocalDate date1, LocalDate date2) {
        return requireDate(date1).isEqual(requireDate(date2));
    }

    public static boolean isBetween(LocalDate date, LocalDate start, LocalDate end) {

        requireDate(date);
        requireDate(start);
        requireDate(end);

        return !date.isBefore(start) && !date.isAfter(end);
    }

    // =========================================================
    // DIFFERENCE BETWEEN DATES
    // =========================================================

    public static long daysBetween(LocalDate start, LocalDate end) {
        return Duration.between(requireDate(start).atStartOfDay(), requireDate(end).atStartOfDay()).toDays();
    }

    public static long hoursBetween(LocalDateTime start, LocalDateTime end) {
        return Duration.between(requireDateTime(start), requireDateTime(end)).toHours();
    }

    public static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        return Duration.between(requireDateTime(start), requireDateTime(end)).toMinutes();
    }

    public static long secondsBetween(LocalDateTime start, LocalDateTime end) {
        return Duration.between(requireDateTime(start), requireDateTime(end)).getSeconds();
    }

    // =========================================================
    // FORMAT DATE
    // =========================================================

    public static String format(LocalDate date) {
        if (date == null) {
            return null;
        }

        return date.format(DATE_FORMATTER);
    }

    public static String format(LocalDate date, String pattern) {
        if (date == null) {
            return null;
        }

        return date.format(DateTimeFormatter.ofPattern(pattern));
    }

    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        return dateTime.format(DATE_TIME_FORMATTER);
    }

    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return null;
        }

        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    // =========================================================
    // PARSE STRING -> DATE
    // =========================================================

    public static LocalDate parseDate(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return LocalDate.parse(value, DATE_FORMATTER);
    }

    public static LocalDate parseDate(String value, String pattern) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return LocalDate.parse(value, DateTimeFormatter.ofPattern(pattern));
    }

    public static LocalDateTime parseDateTime(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
    }

    public static LocalDateTime parseDateTime(String value, String pattern) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return LocalDateTime.parse(value, DateTimeFormatter.ofPattern(pattern));
    }

    // =========================================================
    // SAFE PARSING
    // =========================================================

    public static boolean isValidDate(String value, String pattern) {

        if (value == null || value.isBlank()) {
            return false;
        }

        try {

            LocalDate.parse(value, DateTimeFormatter.ofPattern(pattern));

            return true;

        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    // =========================================================
    // LOCALDATE <-> LOCALDATETIME
    // =========================================================

    public static LocalDateTime toLocalDateTime(LocalDate date) {
        return requireDate(date).atStartOfDay();
    }

    public static LocalDate toLocalDate(LocalDateTime dateTime) {
        return requireDateTime(dateTime).toLocalDate();
    }

    // =========================================================
    // JAVA UTIL DATE CONVERSION
    // =========================================================

    public static Date toDate(LocalDateTime localDateTime) {

        if (localDateTime == null) {
            return null;
        }

        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDateTime toLocalDateTime(Date date) {

        if (date == null) {
            return null;
        }

        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    public static LocalDate toLocalDate(Date date) {

        if (date == null) {
            return null;
        }

        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    // =========================================================
    // INSTANT CONVERSION
    // =========================================================

    public static Instant toInstant(LocalDateTime dateTime, ZoneId zoneId) {

        return requireDateTime(dateTime).atZone(zoneId).toInstant();
    }

    public static LocalDateTime toLocalDateTime(Instant instant, ZoneId zoneId) {

        if (instant == null) {
            return null;
        }

        return LocalDateTime.ofInstant(instant, zoneId);
    }

    // =========================================================
    // TIMESTAMP
    // =========================================================

    public static long currentTimestampMillis() {
        return System.currentTimeMillis();
    }

    public static long currentTimestampSeconds() {
        return Instant.now().getEpochSecond();
    }

    // =========================================================
    // AGE
    // =========================================================

    public static int calculateAge(LocalDate birthDate) {

        requireDate(birthDate);

        return Period.between(birthDate, today()).getYears();
    }

    // =========================================================
    // NULL VALIDATION
    // =========================================================

    private static LocalDate requireDate(LocalDate date) {
        return Objects.requireNonNull(date, "Date must not be null");
    }

    private static LocalDateTime requireDateTime(LocalDateTime dateTime) {

        return Objects.requireNonNull(dateTime, "DateTime must not be null");
    }
}
