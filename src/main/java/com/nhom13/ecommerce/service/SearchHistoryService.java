package com.nhom13.ecommerce.service;

import com.nhom13.ecommerce.entity.SearchHistory;
import com.nhom13.ecommerce.entity.User;
import com.nhom13.ecommerce.repository.SearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation; // <--- THÊM IMPORT NÀY

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
// XÓA @Transactional ở cấp độ class
public class SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;
    private static final long DEBOUNCE_MINUTES = 5;

    // SỬA LỖI Ở ĐÂY:
    // Thêm (propagation = Propagation.REQUIRES_NEW)
    // Bắt buộc tạo một giao dịch MỚI (có thể ghi)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addSearchQuery(User user, String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        
        Optional<SearchHistory> lastSearch = searchHistoryRepository
          .findFirstByUserIdAndQueryOrderByTimestampDesc(user.getId(), query);
        
        if (lastSearch.isPresent()) {
            LocalDateTime lastSearchTime = lastSearch.get().getTimestamp();
            if (lastSearchTime.plusMinutes(DEBOUNCE_MINUTES).isAfter(LocalDateTime.now())) {
                return;
            }
        }
        
        SearchHistory newSearch = new SearchHistory(user, query);
        searchHistoryRepository.save(newSearch);
    }

    // Thêm @Transactional(readOnly = true) cho hàm đọc
    @Transactional(readOnly = true)
    public List<String> getSearchHistory(Long userId, int limit) {
        return searchHistoryRepository.findRecentQueriesByUserId(userId, PageRequest.of(0, limit));
    }
}