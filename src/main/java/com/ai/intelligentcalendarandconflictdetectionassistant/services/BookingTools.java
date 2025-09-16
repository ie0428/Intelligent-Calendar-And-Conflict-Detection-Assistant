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
	@Description("取消日程")
	public Function<CancelBookingRequest, String> cancelBooking() {
		return request -> {
			flightBookingService.cancelBooking(request.bookingNumber(), request.name());
			return "";
		};
	}

	@Bean
	@Description("获取日程详细信息")
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
	@Description("修改日程的信息")
	public Function<ChangeBookingDatesRequest, String> changeBooking() {
		return request -> {
			flightBookingService.changeBooking(request.bookingNumber(), request.name(), request.date(), request.from(),
					request.to());
			return "";
		};
	}

	public record ChangeBookingDatesRequest(String bookingNumber, String name,String date, String from, String to) {
	}


	// 在 BookingTools 类中添加
	public record CreateBookingRequest(String name, String date, String from, String to, String title) {
		// 无参构造函数，用于向后兼容
		public CreateBookingRequest(String name, String date, String from, String to) {
			this(name, date, from, to, null);
		}
	}

	// 更新 createBooking Bean 方法
	@Bean
	@Description("创建日程")
	public Function<CreateBookingRequest, String> createBooking() {
		return request -> {
			try {
				// 传递标题参数，如果没有提供则使用默认值
				String title = request.title();
				if (title == null || title.trim().isEmpty()) {
					title = "会议: " + (request.from() != null ? request.from() : "未指定地点");
				}

				flightBookingService.createBooking(
						request.name(),
						request.date(),
						request.from(),
						request.to(),
						title
				);
				return "日程创建成功";
			} catch (Exception e) {
				log.error("创建日程失败: ", e);
				return "日程创建失败: " + e.getMessage();
			}
		};
	}



}
