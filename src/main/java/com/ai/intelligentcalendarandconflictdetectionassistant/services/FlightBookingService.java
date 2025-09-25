package com.ai.intelligentcalendarandconflictdetectionassistant.services;

import com.ai.intelligentcalendarandconflictdetectionassistant.data.BookingStatus;
import com.ai.intelligentcalendarandconflictdetectionassistant.mapper.CalendarEventMapper;
import com.ai.intelligentcalendarandconflictdetectionassistant.mapper.UserMapper;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.CalendarEvent;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.User;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.BookingTools.BookingDetails;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.impls.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FlightBookingService {

	private final UserMapper userMapper;
    private final CalendarEventMapper calendarEventMapper;

    public FlightBookingService(UserMapper userMapper, CalendarEventMapper calendarEventMapper) {
        this.userMapper = userMapper;
        this.calendarEventMapper = calendarEventMapper;
    }

    /**
     * 获取当前登录用户的ID
     * @return 当前用户ID
     * @throws SecurityException 如果用户未认证
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userDetails.getId();
        }
        throw new SecurityException("用户未认证");
    }

    /**
     * 获取当前登录用户的用户名
     * @return 当前用户名
     * @throws SecurityException 如果用户未认证
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        throw new SecurityException("用户未认证");
    }

	// 获取所有日历事件
	public List<BookingTools.BookingDetails> getBookings() {
		return calendarEventMapper.findAll().stream()
				.map(this::toBookingDetails)
				.collect(Collectors.toList());
	}

	// 根据事件ID查找日历事件
	public CalendarEvent findCalendarEvent(Long eventId, String username) {
		CalendarEvent event = calendarEventMapper.findById(eventId);
		if (event == null) {
			throw new IllegalArgumentException("Calendar event not found");
		}

		User user = userMapper.findById(event.getUserId());
		if (user == null || !user.getUsername().equalsIgnoreCase(username)) {
			throw new IllegalArgumentException("Calendar event not found");
		}

		return event;
	}

	// 根据事件ID查找日历事件
	public CalendarEvent findCalendarEventById(Long eventId) {
		CalendarEvent event = calendarEventMapper.findById(eventId);
		if (event == null) {
			throw new IllegalArgumentException("Calendar event not found");
		}
		return event;
	}

	// 根据用户名查找该用户的所有日程
	public List<BookingDetails> getBookingsByUsername(String username) {
		User user = userMapper.findByUsername(username);
		if (user == null) {
			return List.of(); // 用户不存在，返回空列表
		}

		return calendarEventMapper.findByUserId(user.getId()).stream()
				.map(this::toBookingDetails)
				.collect(Collectors.toList());
	}

	// 根据用户ID查找该用户的所有日程
	public List<BookingDetails> getBookingsByUserId(Long userId) {
		User user = userMapper.findById(userId);
		if (user == null) {
			return List.of(); // 用户不存在，返回空列表
		}

		return calendarEventMapper.findByUserId(userId).stream()
				.map(this::toBookingDetails)
				.collect(Collectors.toList());
	}


	// 根据事件ID和用户名查询事件详情
	public BookingDetails getBookingDetails(String eventId, String username) {
		var event = findCalendarEvent(Long.valueOf(eventId), username);
		return toBookingDetails(event);
	}

	// 更改日历事件
	public void changeBooking(String eventId, String username, String newDate, String location, String description) {
		CalendarEvent event;
		if(username!=null&&!username.isEmpty()){
			event = findCalendarEvent(Long.valueOf(eventId), username);
		}else {
			event = findCalendarEventById(Long.valueOf(eventId));
		}

		LocalDate newLocalDate = LocalDate.parse(newDate);
		event.setStartTime(newLocalDate.atStartOfDay());
		event.setEndTime(newLocalDate.atTime(23, 59));
		event.setLocation(location);
		event.setDescription(description);

		calendarEventMapper.update(event);
	}

	// 取消日历事件
	public void cancelBooking(String eventId, String username) {
		CalendarEvent event;
		if(username!=null&&!username.isEmpty()){
			event = findCalendarEvent(Long.valueOf(eventId), username);
		}else {
			event = findCalendarEventById(Long.valueOf(eventId));
		}
		event.setStatus(CalendarEvent.Status.CANCELLED);
		calendarEventMapper.update(event);
	}

	private BookingDetails toBookingDetails(CalendarEvent event) {
		User user = userMapper.findById(event.getUserId());
		if (user == null) {
			user = new User();
		}

		return new BookingDetails(
				String.valueOf(event.getId()),
				user.getUsername() != null ? user.getUsername() : "Unknown User",
				event.getStartTime().toLocalDate(),
				BookingStatus.valueOf(event.getStatus().name()),
				event.getLocation() != null ? event.getLocation() : "Unknown Location",
				event.getDescription() != null ? event.getDescription() : "No Description",
				event.getEventType() != null ? event.getEventType().name() : "MEETING",
				event.getTitle() != null ? event.getTitle() : "无标题会议"
		);
	}

	// 创建日历事件
	public void createBooking(String date, String location, String description, String title) {
		// 获取当前登录用户的ID和用户名
		Long userId = getCurrentUserId();
		String username = getCurrentUsername();
		
		// 查找用户，如果不存在则创建
		User user = userMapper.findByUsername(username);
		if (user == null) {
			// 用户不存在，创建新用户
			user = new User();
			user.setId(userId);
			user.setUsername(username);
			userMapper.insert(user);
			// 重新获取插入后的用户（包含生成的ID）
			user = userMapper.findByUsername(username);
		}

		LocalDate localDate = LocalDate.parse(date);
		CalendarEvent event = new CalendarEvent();
		event.setUserId(user.getId());
		event.setStartTime(localDate.atStartOfDay());
		event.setEndTime(localDate.atTime(23, 59));
		event.setLocation(location);
		event.setDescription(description);
		event.setStatus(CalendarEvent.Status.TENTATIVE);
		event.setEventType(CalendarEvent.EventType.MEETING);
		// 添加标题字段，使用描述或位置作为标题
		if (title == null || title.trim().isEmpty()) {
			title = "会议: " + (location != null ? location : "未指定地点");
		}
		event.setTitle(title);

		calendarEventMapper.insert(event);
	}

	// 创建日历事件（支持通过参数传递用户ID）
	public void createBooking(String date, String location, String description, String title, Long userId) {
		// 查找用户，如果不存在则创建
		User user = userMapper.findById(userId);
		if (user == null) {
			// 用户不存在，抛出异常
			throw new IllegalArgumentException("用户不存在，ID: " + userId);
		}

		LocalDate localDate = LocalDate.parse(date);
		CalendarEvent event = new CalendarEvent();
		event.setUserId(user.getId());
		event.setStartTime(localDate.atStartOfDay());
		event.setEndTime(localDate.atTime(23, 59));
		event.setLocation(location);
		event.setDescription(description);
		event.setStatus(CalendarEvent.Status.TENTATIVE);
		event.setEventType(CalendarEvent.EventType.MEETING);
		// 添加标题字段，使用描述或位置作为标题
		if (title == null || title.trim().isEmpty()) {
			title = "会议: " + (location != null ? location : "未指定地点");
		}
		event.setTitle(title);

		calendarEventMapper.insert(event);
	}
}
