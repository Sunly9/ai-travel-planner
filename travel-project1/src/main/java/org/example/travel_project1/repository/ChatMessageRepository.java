package org.example.travel_project1.repository;

import org.example.travel_project1.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByUserIdOrderByCreateTimeAsc(Long userId);

    // 派生删除方法需要显式事务，否则运行时报
    // "No EntityManager with actual transaction available for current thread"
    @Transactional
    void deleteByUserId(Long userId);

    long countByUserId(Long userId);

    /** 按角色统计（对话轮次 = role=user 的条数） */
    long countByUserIdAndRole(Long userId, String role);
}
