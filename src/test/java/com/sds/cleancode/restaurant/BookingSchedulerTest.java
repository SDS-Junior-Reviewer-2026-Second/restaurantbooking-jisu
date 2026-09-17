package com.sds.cleancode.restaurant;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingSchedulerTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    @Mock
    private Customer CUSTOMER_WITHOUT_EMAIL;
    @Mock(answer = Answers.RETURNS_MOCKS)
    private Customer CUSTOMER_WITH_EMAIL;
    private static final int HOUR_CAPACITY = 3;
    private static final int UNDER_CAPACITY = 1;
    private static final LocalDateTime ON_THE_HOUR = LocalDateTime.parse("2026/09/17 09:00", FORMATTER);
    private static final LocalDateTime SUNDAY = LocalDateTime.parse("2021/03/28 17:00", FORMATTER);
    private static final LocalDateTime MONDAY = LocalDateTime.parse("2021/03/29 17:00", FORMATTER);

    @Spy
    private BookingScheduler bookingScheduler;

    @Mock
    private SmsSender smsSender;
    @Mock
    private MailSender mailSender;
    
    public BookingSchedulerTest(){
        bookingScheduler = new BookingScheduler(HOUR_CAPACITY);
    }
    
    @BeforeEach
    public void setup(){
        bookingScheduler.setSmsSender(smsSender);
        bookingScheduler.setMailSender(mailSender);
    }

    @Test
    public void 예약은_정시에만_가능하다_정시가_아닌경우_예약불가() {
        LocalDateTime notOnTheHour = LocalDateTime.parse("2026/09/17 09:05", FORMATTER);
        Schedule schedule = new Schedule(notOnTheHour, UNDER_CAPACITY, CUSTOMER_WITHOUT_EMAIL);

        assertThatThrownBy(() -> bookingScheduler.addSchedule(schedule))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Booking should be on the hour.");
    }

    @Test
    public void 예약은_정시에만_가능하다_정시인_경우_예약가능() {
        Schedule schedule = new Schedule(ON_THE_HOUR, UNDER_CAPACITY, CUSTOMER_WITHOUT_EMAIL);
        bookingScheduler.addSchedule(schedule);

        assertThat(bookingScheduler.hasSchedule(schedule)).isTrue();
    }

    @Test
    public void 시간대별_인원제한이_있다_같은_시간대에_Capacity_초과할_경우_예외발생() {
        Schedule s1 = new Schedule(ON_THE_HOUR, HOUR_CAPACITY, CUSTOMER_WITHOUT_EMAIL);
        Schedule s2 = new Schedule(ON_THE_HOUR, UNDER_CAPACITY, CUSTOMER_WITH_EMAIL);

        bookingScheduler.addSchedule(s1);

        assertThatThrownBy(() -> bookingScheduler.addSchedule(s2))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Number of people is over restaurant capacity per hour");
    }

    @Test
    public void 시간대별_인원제한이_있다_같은_시간대가_다르면_Capacity_차있어도_스케쥴_추가_성공() {
        LocalDateTime nextHour = ON_THE_HOUR.plusHours(1);
        Schedule s1 = new Schedule(ON_THE_HOUR, HOUR_CAPACITY, CUSTOMER_WITHOUT_EMAIL);
        Schedule s2 = new Schedule(nextHour, UNDER_CAPACITY, CUSTOMER_WITH_EMAIL);

        bookingScheduler.addSchedule(s1);
        bookingScheduler.addSchedule(s2);
        assertThat(bookingScheduler.hasSchedule(s1)).isTrue();
        assertThat(bookingScheduler.hasSchedule(s2)).isTrue();
    }

    @Test
    public void 예약완료시_SMS는_무조건_발송() {
        Schedule schedule = new Schedule(ON_THE_HOUR, UNDER_CAPACITY, CUSTOMER_WITHOUT_EMAIL);
        bookingScheduler.addSchedule(schedule);

        verify(smsSender, times(1)).send(schedule);
    }

    @Test
    public void 이메일이_없는_경우에는_이메일_미발송() {
        Schedule schedule = new Schedule(ON_THE_HOUR, UNDER_CAPACITY, CUSTOMER_WITHOUT_EMAIL);
        bookingScheduler.addSchedule(schedule);

        verify(mailSender, times(0)).sendMail(schedule);
    }

    @Test
    public void 이메일이_있는_경우에는_이메일_발송() {
        Schedule schedule = new Schedule(ON_THE_HOUR, UNDER_CAPACITY, CUSTOMER_WITH_EMAIL);
        bookingScheduler.addSchedule(schedule);

        verify(mailSender, times(1)).sendMail(schedule);
    }

    @Test
    public void 현재날짜가_일요일인_경우_예약불가_예외처리() {
        when(bookingScheduler.getNow()).thenReturn(SUNDAY);
        Schedule schedule = new Schedule(ON_THE_HOUR, UNDER_CAPACITY, CUSTOMER_WITH_EMAIL);

        assertThatThrownBy(() -> bookingScheduler.addSchedule(schedule))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Booking system is not available on sunday");
    }

    @Test
    public void 현재날짜가_일요일이_아닌경우_예약가능() {
        when(bookingScheduler.getNow()).thenReturn(MONDAY);
        Schedule schedule = new Schedule(ON_THE_HOUR, UNDER_CAPACITY, CUSTOMER_WITH_EMAIL);
        bookingScheduler.addSchedule(schedule);

        assertThat(bookingScheduler.hasSchedule(schedule)).isTrue();
    }
}
