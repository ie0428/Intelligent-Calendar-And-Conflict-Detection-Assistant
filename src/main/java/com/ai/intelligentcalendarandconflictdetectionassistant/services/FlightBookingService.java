package com.ai.intelligentcalendarandconflictdetectionassistant.services;

import com.ai.intelligentcalendarandconflictdetectionassistant.data.BookingStatus;
import com.ai.intelligentcalendarandconflictdetectionassistant.mapper.CalendarEventMapper;
import com.ai.intelligentcalendarandconflictdetectionassistant.mapper.UserMapper;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.CalendarEvent;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.User;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.BookingTools.BookingDetails;
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

	// 获取所有日历事件
	public List<BookingTools.BookingDetails> getBookings() {
		return calendarEventMapper.findAll().stream()
				.map(this::toBookingDetails)
				.collect(Collectors.toList());
	}

	// 根据事件ID和用户名查找日历事件
	private CalendarEvent findCalendarEvent(Long eventId, String username) {
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

	// 根据事件ID和用户名查询事件详情
	public BookingDetails getBookingDetails(String eventId, String username) {
		var event = findCalendarEvent(Long.valueOf(eventId), username);
		return toBookingDetails(event);
	}

	// 更改日历事件
	public void changeBooking(String eventId, String username, String newDate, String location, String description) {
		var event = findCalendarEvent(Long.valueOf(eventId), username);
		if (event.getStartTime().isBefore(LocalDateTime.now().plusDays(1))) {
			throw new IllegalArgumentException("Event cannot be changed within 24 hours of the start date.");
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
		var event = findCalendarEvent(Long.valueOf(eventId), username);
		if (event.getStartTime().isBefore(LocalDateTime.now().plusDays(2))) {
			throw new IllegalArgumentException("Event cannot be cancelled within 48 hours of the start date.");
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
				event.getEventType() != null ? event.getEventType().name() : "MEETING"
		);
	}

	// 创建日历事件
	public void createBooking(String username, String date, String location, String description, String title) {
		// 查找用户，如果不存在则创建
		User user = userMapper.findByUsername(username);
		if (user == null) {
			// 用户不存在，创建新用户
			user = new User();
			user.setUsername(username);
			// 这里可以设置默认用户ID，或者让数据库自动生成
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
		title = "会议: " + (location != null ? location : "未指定地点");
		event.setTitle(title);


		calendarEventMapper.insert(event);
	}
}
