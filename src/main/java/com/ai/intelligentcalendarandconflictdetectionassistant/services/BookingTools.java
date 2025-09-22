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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class BookingTools {

	@Autowired
	private FlightBookingService flightBookingService;

	@JsonInclude(Include.NON_NULL)
	public record BookingDetails(String eventId, String name, LocalDate date, BookingStatus bookingStatus,
									 String from, String to, String bookingClass) {
	}

	public record CancelBookingRequest(String eventId, String name) {
	}

	@Bean
	@Description("取消日程")
	public Function<CancelBookingRequest, String> cancelBooking() {
		return request -> {
			flightBookingService.cancelBooking(request.eventId(), request.name());
			return "";
		};
	}


	public record FindCalendarEventRequest(String eventId, String name) {
		// 添加默认构造函数支持只传name
		public FindCalendarEventRequest(String name) {
			this(null, name);
		}
	}

	@Bean
	@Description("查找用户日程")
	public Function<FindCalendarEventRequest, List<BookingDetails>> findCalendarEvent() {
		return request -> {
			try {
				String username = getCurrentUsername();
				if (request.eventId() != null && !request.eventId().isEmpty()) {
					// 查询单个事件
					BookingDetails details = flightBookingService.getBookingDetails(request.eventId(), username);
					return List.of(details);
				} else {
					// 查询用户所有事件
					return flightBookingService.getBookingsByUsername(username);
				}
			} catch (Exception e) {
				log.warn("Find calendar event: {}", NestedExceptionUtils.getMostSpecificCause(e).getMessage());
				return List.of();
			}
		};
	}



	@Bean
	@Description("获取日程详细信息")
	public Function<BookingDetailsRequest, BookingDetails> getBookingDetails() {
		return request -> {
			try {
				String username = getCurrentUsername();
				return flightBookingService.getBookingDetails(request.eventId(), username);
			}
			catch (Exception e) {
				log.warn("Booking details: {}", NestedExceptionUtils.getMostSpecificCause(e).getMessage());
				return new BookingDetails(request.eventId(), request.name(), null, null, null, null, null);
			}
		};
	}

	public record BookingDetailsRequest(String eventId, String name) {
	}

	@Bean
	@Description("修改日程的信息")
	public Function<ChangeBookingDatesRequest, String> changeBooking() {
		return request -> {
			flightBookingService.changeBooking(request.eventId(), request.name(), request.date(), request.from(),
					request.to());
			return "";
		};
	}

	public record ChangeBookingDatesRequest(String eventId, String name,String date, String from, String to) {
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

	public record AllBookingsRequest() {
	}
	@Bean
	@Description("获取所有日程")
	public Function<AllBookingsRequest, List<BookingDetails>> getAllBookings() {
		return request -> {
			try {
				String username = getCurrentUsername();
				return flightBookingService.getBookingsByUsername(username);
			} catch (Exception e) {
				log.warn("获取所有日程失败: {}", NestedExceptionUtils.getMostSpecificCause(e).getMessage());
				return List.of();
			}
		};
	}

	// 获取当前登录用户的用户名
	private String getCurrentUsername() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.isAuthenticated()) {
			return authentication.getName();
		}
		throw new SecurityException("用户未认证");
	}
}
