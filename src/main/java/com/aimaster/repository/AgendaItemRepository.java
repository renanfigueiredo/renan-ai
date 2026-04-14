package com.aimaster.repository;

import com.aimaster.model.AgendaItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AgendaItemRepository extends JpaRepository<AgendaItem, Long> {
    List<AgendaItem> findAllByOrderByCreatedAtAsc();
}
