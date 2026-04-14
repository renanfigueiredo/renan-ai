package com.aimaster.service;

import com.aimaster.model.AgendaItem;
import com.aimaster.repository.AgendaItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AgendaItemService {

    private final AgendaItemRepository repository;

    public List<AgendaItem> findAll() {
        return repository.findAllByOrderByCreatedAtAsc();
    }

    public long count() {
        return repository.count();
    }

    @Transactional
    public AgendaItem save(AgendaItem item) {
        return repository.save(item);
    }

    @Transactional
    public Optional<AgendaItem> update(Long id, AgendaItem updated) {
        return repository.findById(id).map(existing -> {
            existing.setType(updated.getType());
            existing.setName(updated.getName());
            existing.setIcon(updated.getIcon());
            existing.setColor(updated.getColor());
            existing.setDayOfWeek(updated.getDayOfWeek());
            existing.setTime(updated.getTime());
            existing.setFrequency(updated.getFrequency());
            existing.setDate(updated.getDate());
            existing.setLocation(updated.getLocation());
            existing.setDescription(updated.getDescription());
            existing.setHighlight(updated.isHighlight());
            return repository.save(existing);
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (!repository.existsById(id)) return false;
        repository.deleteById(id);
        return true;
    }
}
