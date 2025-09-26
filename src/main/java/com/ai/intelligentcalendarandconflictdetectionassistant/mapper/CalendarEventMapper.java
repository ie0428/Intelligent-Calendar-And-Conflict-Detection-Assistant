package com.ai.intelligentcalendarandconflictdetectionassistant.mapper;

import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.CalendarEvent;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CalendarEventMapper {

    @Select("SELECT * FROM calendar_events")
    List<CalendarEvent> findAll();

    @Select("SELECT * FROM calendar_events WHERE id = #{id}")
    CalendarEvent findById(Long id);

    @Select("SELECT * FROM calendar_events WHERE user_id = #{userId}")
    List<CalendarEvent> findByUserId(Long userId);

    @Insert("INSERT INTO calendar_events(user_id, title, description, location, start_time, end_time, timezone, event_type, status, created_at, updated_at) " +
            "VALUES(#{userId}, #{title}, #{description}, #{location}, #{startTime}, #{endTime}, #{timezone}, #{eventType}, #{status}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(CalendarEvent event);

    @Update("UPDATE calendar_events SET title=#{title}, description=#{description}, location=#{location}, " +
            "start_time=#{startTime}, end_time=#{endTime}, event_type=#{eventType}, status=#{status}, updated_at=#{updatedAt} " +
            "WHERE id=#{id}")
    void update(CalendarEvent event);

    @Delete("DELETE FROM calendar_events WHERE id = #{id}")
    void deleteById(Long id);

    @Delete("DELETE FROM calendar_events WHERE user_id = #{userId}")
    void deleteByUserId(Long userId);
}
