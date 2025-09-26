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

@Slf4j
@Configuration
public class BookingTools {

	@Autowired
	private FlightBookingService flightBookingService;

	@JsonInclude(Include.NON_NULL)
	public record BookingDetails(String eventId, String name, LocalDate date, BookingStatus bookingStatus,
								 String from, String to, String bookingClass, String title) {
	}

	// 基于用户ID的请求记录
	public record CancelBookingRequest(String eventId, Long userId) {
	}

	@Bean
	@Description("取消日程")
	public Function<CancelBookingRequest, String> cancelBooking() {
		return request -> {
			try {
				log.info("开始取消日程，事件ID: {}", request.eventId());
				
				Long userId = getCurrentUserId();
				if (userId == null) {
					userId = request.userId();
					log.warn("无法从认证上下文获取用户ID，使用请求参数中的用户ID: {}", userId);
				} else {
					log.info("成功获取当前用户ID: {}", userId);
				}
				
				log.info("调用FlightBookingService取消日程，用户ID: {}, 事件ID: {}", userId, request.eventId());
				flightBookingService.cancelBookingByUserId(request.eventId(), userId);
				return "日程取消成功";
			} catch (Exception e) {
				return "日程取消失败: " + e.getMessage();
			}
		};
	}

	public record FindCalendarEventRequest(String eventId, Long userId) {
		// 添加默认构造函数支持只传userId
		public FindCalendarEventRequest(Long userId) {
			this(null, userId);
		}
	}

	@Bean
	@Description("查找用户日程")
	public Function<FindCalendarEventRequest, List<BookingDetails>> findCalendarEvent() {
		return request -> {
			try {
				log.info("开始查找用户日程，事件ID: {}, 用户ID: {}", request.eventId(), request.userId());
				
				Long userId = getCurrentUserId();
				
				// 如果无法从认证上下文获取用户ID，使用请求参数中的userId
				if (userId == null) {
					userId = request.userId();
					log.warn("无法从认证上下文获取用户ID，使用请求参数中的用户ID: {}", userId);
				} else {
					log.info("成功获取当前用户ID: {}", userId);
				}
				
				if (request.eventId() != null && !request.eventId().isEmpty()) {
					// 查询单个事件
					log.info("查询单个事件，事件ID: {}, 用户ID: {}", request.eventId(), userId);
					BookingDetails details = flightBookingService.getBookingDetailsByUserId(request.eventId(), userId);
					log.info("成功查询到事件详情");
					return List.of(details);
				} else {
					// 查询用户所有事件
					log.info("查询用户所有事件，用户ID: {}", userId);
					List<BookingDetails> bookings = flightBookingService.getBookingsByUserId(userId);
					log.info("成功查询到 {} 个事件", bookings.size());
					return bookings;
				}
			} catch (Exception e) {
				log.warn("查找用户日程失败: {}", NestedExceptionUtils.getMostSpecificCause(e).getMessage());
				return List.of();
			}
		};
	}

	@Bean
	@Description("获取日程详细信息")
	public Function<BookingDetailsRequest, BookingDetails> getBookingDetails() {
		return request -> {
			try {
				log.info("开始获取日程详细信息，事件ID: {}", request.eventId());
				
				Long userId = getCurrentUserId();
				
				// 如果无法从认证上下文获取用户ID，使用请求参数中的userId
				if (userId == null) {
					userId = request.userId();
					log.warn("无法从认证上下文获取用户ID，使用请求参数中的用户ID: {}", userId);
				} else {
					log.info("成功获取当前用户ID: {}", userId);
				}
				
				log.info("调用FlightBookingService获取日程详细信息，用户ID: {}, 事件ID: {}", userId, request.eventId());
				BookingDetails details = flightBookingService.getBookingDetailsByUserId(request.eventId(), userId);
				log.info("成功获取日程详细信息: {}", details);
				return details;
			}
			catch (Exception e) {
				log.warn("获取日程详细信息失败: {}", NestedExceptionUtils.getMostSpecificCause(e).getMessage());
				return new BookingDetails(request.eventId(), "Unknown User", null, null, null, null, null, null);
			}
		};
	}

	public record BookingDetailsRequest(String eventId, Long userId) {
	}

	@Bean
	@Description("修改日程的信息")
	public Function<ChangeBookingDatesRequest, String> changeBooking() {
		return request -> {
			try {
				log.info("开始修改日程，事件ID: {}, 日期: {}, 从: {}, 到: {}", 
						request.eventId(), request.date(), request.from(), request.to());
				
				Long userId = getCurrentUserId();
				if (userId == null) {
					userId = request.userId();
					log.warn("无法从认证上下文获取用户ID，使用请求参数中的用户ID: {}", userId);
				} else {
					log.info("成功获取当前用户ID: {}", userId);
				}
				
				log.info("调用FlightBookingService修改日程，用户ID: {}, 事件ID: {}", userId, request.eventId());
				flightBookingService.changeBookingByUserId(request.eventId(), userId, request.date(), request.from(),
						request.to());
				log.info("日程修改成功");
				return "日程修改成功";
			} catch (Exception e) {
				log.error("修改日程失败: ", e);
				return "日程修改失败: " + e.getMessage();
			}
		};
	}

	public record ChangeBookingDatesRequest(String eventId, Long userId, String date, String from, String to) {
	}

	// 在 BookingTools 类中添加
	public record CreateBookingRequest(String date, String from, String to, String title, String description, String location, String timezone) {
		// 简化构造函数，只传必要字段
		public CreateBookingRequest(String date, String from, String to, String title) {
			this(date, from, to, title, null, null, "UTC");
		}
		
		// 包含描述的构造函数
		public CreateBookingRequest(String date, String from, String to, String title, String description) {
			this(date, from, to, title, description, null, "UTC");
		}
	}

	// 更新 createBooking Bean 方法
	@Bean
	@Description("创建日程")
	public Function<CreateBookingRequest, String> createBooking() {
		return request -> {
			try {
				log.info("开始创建日程，请求参数: date={}, from={}, to={}, title={}", 
						request.date(), request.from(), request.to(), request.title());
				
				// 获取用户ID
				Long userId = getCurrentUserId();
				
				// 如果无法从认证上下文获取用户ID，使用默认用户ID
				if (userId == null) {
					userId = 3L; // 默认用户ID
					log.warn("无法从认证上下文获取用户ID，使用默认用户ID: {}", userId);
				} else {
					log.info("成功获取当前用户ID: {}", userId);
				}

				// 处理标题
				String title = request.title();
				if (title == null || title.trim().isEmpty()) {
					title = "会议: " + (request.from() != null ? request.from() : "未指定地点");
					log.info("标题为空，自动生成标题: {}", title);
				}

				// 处理描述
				String description = request.description();
				if (description == null || description.trim().isEmpty()) {
					description = request.to(); // 如果没有提供描述，使用to字段作为描述
					log.info("描述为空，使用to字段作为描述: {}", description);
				}

				// 处理地点
				String location = request.location();
				if (location == null || location.trim().isEmpty()) {
					location = request.from(); // 如果没有提供地点，使用from字段作为地点
					log.info("地点为空，使用from字段作为地点: {}", location);
				}

				// 处理时区
				String timezone = request.timezone();
				if (timezone == null || timezone.trim().isEmpty()) {
					timezone = "UTC"; // 默认时区
					log.info("时区为空，使用默认时区: {}", timezone);
				}

				log.info("调用FlightBookingService创建日程，参数: userId={}, date={}, location={}, description={}, title={}, timezone={}",
						userId, request.date(), location, description, title, timezone);

				flightBookingService.createBooking(
						request.date(),
						location,
						description,
						title,
						userId,
						timezone
				);
				log.info("日程创建成功");
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
				log.info("开始获取所有日程");
				
				Long userId = getCurrentUserId();

				// 如果无法从认证上下文获取用户ID，使用默认用户ID
				if (userId == null) {
					userId = 3L; // 默认用户ID
					log.warn("无法从认证上下文获取用户ID，使用默认用户ID: {}", userId);
				} else {
					log.info("成功获取当前用户ID: {}", userId);
				}
				
				log.info("调用FlightBookingService获取用户所有日程，用户ID: {}", userId);
				List<BookingDetails> bookings = flightBookingService.getBookingsByUserId(userId);
				log.info("成功获取到 {} 个日程", bookings.size());
				return bookings;
			} catch (Exception e) {
				log.warn("获取所有日程失败: {}", NestedExceptionUtils.getMostSpecificCause(e).getMessage());
				return List.of();
			}
		};
	}

	/**
	 * 获取当前登录用户的ID
	 * @return 当前用户ID，如果无法获取返回null
	 */
	private Long getCurrentUserId() {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			log.info("尝试从SecurityContext获取认证信息，认证对象: {}", authentication);
			
			if (authentication != null) {
				log.info("认证对象不为null，是否已认证: {}", authentication.isAuthenticated());
				log.info("认证主体类型: {}", authentication.getPrincipal() != null ? authentication.getPrincipal().getClass().getName() : "null");
				
				if (authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetailsImpl) {
					UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
					Long userId = userDetails.getId();
					log.info("成功从认证上下文获取用户ID: {}", userId);
					return userId;
				} else {
					log.warn("认证信息不符合要求：isAuthenticated={}, principal类型正确={}", 
							authentication.isAuthenticated(), 
							authentication.getPrincipal() instanceof UserDetailsImpl);
				}
			} else {
				log.warn("SecurityContext中未找到认证信息");
			}
		} catch (Exception e) {
			log.error("从认证上下文获取用户ID时发生异常: {}", e.getMessage(), e);
		}
		
		log.warn("无法从认证上下文获取用户ID，返回null");
		return null;
	}
}
