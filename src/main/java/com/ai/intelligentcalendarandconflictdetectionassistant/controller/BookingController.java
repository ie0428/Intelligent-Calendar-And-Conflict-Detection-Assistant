package com.ai.intelligentcalendarandconflictdetectionassistant.controller;


import com.ai.intelligentcalendarandconflictdetectionassistant.services.BookingTools.BookingDetails;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.FlightBookingService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@CrossOrigin
public class BookingController {

	private final FlightBookingService flightBookingService;

	public BookingController(FlightBookingService flightBookingService) {
		this.flightBookingService = flightBookingService;
	}
	@CrossOrigin
	@GetMapping(value = "/booking/list")
	public List<BookingDetails> getBookings() {
		String username = getCurrentUsername();
		return flightBookingService.getBookingsByUsername(username);
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
