package com.ai.intelligentcalendarandconflictdetectionassistant.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ai.intelligentcalendarandconflictdetectionassistant.data.BookingStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.core.NestedExceptionUtils;

import java.time.LocalDate;
import java.util.function.Function;

@Slf4j
@Configuration
public class BookingTools {

	@Autowired
	private FlightBookingService flightBookingService;

	@JsonInclude(Include.NON_NULL)
	public record BookingDetails(String bookingNumber, String name, LocalDate date, BookingStatus bookingStatus,
								 String from, String to, String bookingClass) {
	}

	public record CancelBookingRequest(String bookingNumber, String name) {
	}

	@Bean
	@Description("取消机票预定")
	public Function<CancelBookingRequest, String> cancelBooking() {
		return request -> {
			flightBookingService.cancelBooking(request.bookingNumber(), request.name());
			return "";
		};
	}

	@Bean
	@Description("获取机票预定详细信息")
	public Function<BookingDetailsRequest, BookingDetails> getBookingDetails() {
		return request -> {
			try {
				return flightBookingService.getBookingDetails(request.bookingNumber(), request.name());
			}
			catch (Exception e) {
				log.warn("Booking details: {}", NestedExceptionUtils.getMostSpecificCause(e).getMessage());
				return new BookingDetails(request.bookingNumber(), request.name(), null, null, null, null, null);
			}
		};
	}

	public record BookingDetailsRequest(String bookingNumber, String name) {
	}

	@Bean
	@Description("修改机票预定日期")
	public Function<ChangeBookingDatesRequest, String> changeBooking() {
		return request -> {
			flightBookingService.changeBooking(request.bookingNumber(), request.name(), request.date(), request.from(),
					request.to());
			return "";
		};
	}

	public record ChangeBookingDatesRequest(String bookingNumber, String name,String date, String from, String to) {
	}


}
