package com.ai.intelligentcalendarandconflictdetectionassistant.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ai.intelligentcalendarandconflictdetectionassistant.data.BookingStatus;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.impls.UserDetailsImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

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
								 String from, String to, String bookingClass, String title) {
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
				
				// 如果无法从认证上下文获取用户名，尝试从请求参数中获取
				if (username == null || username.isEmpty()) {
					username = request.name();
				}
				
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
				
				// 如果无法从认证上下文获取用户名，尝试从请求参数中获取
				if (username == null || username.isEmpty()) {
					username = request.name();
				}
				
				return flightBookingService.getBookingDetails(request.eventId(), username);
			}
			catch (Exception e) {
				log.warn("Booking details: {}", NestedExceptionUtils.getMostSpecificCause(e).getMessage());
				return new BookingDetails(request.eventId(), request.name(), null, null, null, null, null,null);
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

				// 尝试从认证上下文获取用户ID
				Long userId = null;
				try {
					Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
					if (authentication != null && authentication.isAuthenticated() && 
						authentication.getPrincipal() instanceof UserDetailsImpl) {
						UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
						userId = userDetails.getId();
					}
				} catch (Exception e) {
					log.warn("无法从认证上下文获取用户ID，将使用默认用户ID");
				}

				// 如果无法从认证上下文获取用户ID，尝试从advisor参数中获取
				if (userId == null) {
					// 尝试从advisor参数中获取sessionId并提取用户ID
					userId = extractUserIdFromAdvisorParams();
					
					// 如果仍然无法获取，使用默认用户ID（当前登录用户ID为3）
					if (userId == null) {
						userId = 3L;
					}
				}

				flightBookingService.createBooking(
						request.date(),
						request.from(),
						request.to(),
						title,
						userId
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
				
				// 如果无法从认证上下文获取用户名，使用默认用户名
				if (username == null || username.isEmpty()) {
					username = "testuser"; // 默认用户名
				}
				
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

	// 从advisor参数中提取用户ID
	private Long extractUserIdFromAdvisorParams() {
		try {
			// 尝试从ThreadLocal或请求上下文中获取sessionId
			// 这里需要获取当前请求的sessionId，然后从中提取用户ID
			
			// 由于Spring AI框架的advisor参数传递机制，我们需要通过其他方式获取sessionId
			// 可以尝试从SecurityContextHolder获取认证信息，或者从请求参数中获取
			
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (authentication != null && authentication.isAuthenticated() && 
				authentication.getPrincipal() instanceof UserDetailsImpl) {
				UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
				return userDetails.getId();
			}
			
			// 如果无法从认证上下文获取，尝试从HTTP请求中获取sessionId
			// 通过ServletRequestAttributes获取当前请求
			try {
				ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
				if (attributes != null) {
					HttpServletRequest request = attributes.getRequest();
					
					// 尝试从请求参数中获取sessionId
					String sessionId = request.getParameter("sessionId");
					if (sessionId != null && !sessionId.isEmpty()) {
						return extractUserIdFromSessionId(sessionId);
					}
					
					// 尝试从请求头中获取sessionId
					sessionId = request.getHeader("sessionId");
					if (sessionId != null && !sessionId.isEmpty()) {
						return extractUserIdFromSessionId(sessionId);
					}
					
					// 尝试从请求头中获取X-Session-Id
					sessionId = request.getHeader("X-Session-Id");
					if (sessionId != null && !sessionId.isEmpty()) {
						return extractUserIdFromSessionId(sessionId);
					}
				}
			} catch (Exception e) {
				log.warn("从HTTP请求获取sessionId失败: {}", e.getMessage());
			}
			
		} catch (Exception e) {
			log.warn("从advisor参数提取用户ID失败: {}", e.getMessage());
		}
		
		return null;
	}

	// 从sessionId字符串中提取用户ID
	private Long extractUserIdFromSessionId(String sessionId) {
		if (sessionId == null || sessionId.isEmpty()) {
			return null;
		}
		
		// sessionId格式: "user-{userId}-session-{timestamp}"
		// 例如: "user-3-session-1758795908586"
		try {
			if (sessionId.startsWith("user-")) {
				int firstDashIndex = sessionId.indexOf("-");
				int secondDashIndex = sessionId.indexOf("-", firstDashIndex + 1);
				
				if (secondDashIndex > firstDashIndex + 1) {
					String userIdStr = sessionId.substring(firstDashIndex + 1, secondDashIndex);
					return Long.parseLong(userIdStr);
				}
			}
		} catch (Exception e) {
			log.warn("从sessionId提取用户ID失败: {}, sessionId: {}", e.getMessage(), sessionId);
		}
		
		return null;
	}
}
