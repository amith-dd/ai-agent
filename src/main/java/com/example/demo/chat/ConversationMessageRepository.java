package com.example.demo.chat;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

	@Query("""
			select message
			from ConversationMessage message
			where message.conversation.id = :conversationId
			order by message.createdAt asc, message.id asc
			""")
	List<ConversationMessage> findAllForConversation(@Param("conversationId") Long conversationId);

	@Query("""
			select message
			from ConversationMessage message
			where message.conversation.id = :conversationId
			order by message.createdAt desc, message.id desc
			""")
	List<ConversationMessage> findRecentForConversation(@Param("conversationId") Long conversationId, Pageable pageable);

	long countByConversation_Id(Long conversationId);
}
