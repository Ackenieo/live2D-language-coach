package com.ackenieo.init_pro.conversation.infrastructure.persistence;

import com.ackenieo.init_pro.conversation.domain.entity.ChatSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 对话会话 Mapper
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    @Select("""
            SELECT *
            FROM t_chat_session
            WHERE user_id = #{userId}
              AND ended_at IS NOT NULL
              AND deleted = 0
            ORDER BY created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<ChatSession> selectEndedSessionsByUserId(@Param("userId") String userId,
                                                  @Param("offset") int offset,
                                                  @Param("limit") int limit);

    @Select("""
            SELECT *
            FROM t_chat_session
            WHERE ended_at IS NOT NULL
              AND deleted = 0
            ORDER BY created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<ChatSession> selectAllEndedSessions(@Param("offset") int offset,
                                             @Param("limit") int limit);
}
