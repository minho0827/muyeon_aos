package com.muyeon.app.ui.chat.proposal

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * ★ 2026-11-14 약속잡기 시간 계산 — 전부 KST(서버·iOS 와 같은 기준). 화면 부품은 여기 값만 쓴다.
 */
internal object ProposalTime {
    private val KST: TimeZone = TimeZone.getTimeZone("Asia/Seoul")
    private val DATE = SimpleDateFormat("M/d(E)", Locale.KOREA).apply { timeZone = KST }
    private val TIME = SimpleDateFormat("a h:mm", Locale.KOREA).apply { timeZone = KST }

    /** 휠에 보일 날짜 범위(오늘부터) · 시각 간격. */
    private const val DAYS_AHEAD = 180
    private const val MINUTE_STEP = 10

    fun dateLabel(millis: Long): String = DATE.format(Date(millis))
    fun timeLabel(millis: Long): String = TIME.format(Date(millis))

    /** 하루 안 분(0..1439) → "오후 3:00". */
    fun minuteLabel(minuteOfDay: Int): String =
        timeLabel(cal(System.currentTimeMillis()).apply {
            set(Calendar.HOUR_OF_DAY, minuteOfDay / 60); set(Calendar.MINUTE, minuteOfDay % 60)
        }.timeInMillis)

    private fun cal(millis: Long): Calendar = Calendar.getInstance(KST).apply { timeInMillis = millis }

    /** 내일 오후 3시 — iOS 와 같은 기본값. */
    fun defaultStart(): Long = cal(System.currentTimeMillis()).apply {
        add(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 15); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** 그 날의 KST 자정. 날짜 휠의 값. */
    fun dayStart(millis: Long): Long = cal(millis).apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun minuteOfDay(millis: Long): Int = cal(millis).let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }

    fun dayOptions(): List<Long> {
        val today = dayStart(System.currentTimeMillis())
        return (0..DAYS_AHEAD).map { d -> cal(today).apply { add(Calendar.DAY_OF_MONTH, d) }.timeInMillis }
    }

    fun minuteOptions(): List<Int> = (0 until 24 * 60 step MINUTE_STEP).toList()

    /** 날짜만 바꾸고 시각은 그대로. */
    fun withDay(current: Long, day: Long): Long = cal(day).apply {
        val m = minuteOfDay(current)
        set(Calendar.HOUR_OF_DAY, m / 60); set(Calendar.MINUTE, m % 60)
    }.timeInMillis

    /** 시각만 바꾸고 날짜는 그대로. */
    fun withMinute(current: Long, minuteOfDay: Int): Long = cal(dayStart(current)).apply {
        set(Calendar.HOUR_OF_DAY, minuteOfDay / 60); set(Calendar.MINUTE, minuteOfDay % 60)
    }.timeInMillis
}
