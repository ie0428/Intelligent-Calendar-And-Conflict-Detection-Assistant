package com.ai.intelligentcalendarandconflictdetectionassistant.mapper;

import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM users")
    List<User> findAll();

    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(Long id);

    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(String username);

    @Insert("INSERT INTO users(username, email, password_hash, display_name, timezone, language, avatar_url, is_active, created_at, updated_at) " +
            "VALUES(#{username}, #{email}, #{passwordHash}, #{displayName}, #{timezone}, #{language}, #{avatarUrl}, #{isActive}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);

    @Update("UPDATE users SET username=#{username}, email=#{email}, password_hash=#{passwordHash}, display_name=#{displayName}, " +
            "timezone=#{timezone}, language=#{language}, avatar_url=#{avatarUrl}, active=#{active}, updated_at=#{updatedAt} WHERE id=#{id}")
    void update(User user);

    @Delete("DELETE FROM users WHERE id = #{id}")
    void deleteById(Long id);
}
