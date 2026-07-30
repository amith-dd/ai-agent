package com.example.demo.chat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

	Optional<ChatConversation> findByConversationDate(LocalDate conversationDate);

	List<ChatConversation> findAllByOrderByConversationDateDesc();
}
