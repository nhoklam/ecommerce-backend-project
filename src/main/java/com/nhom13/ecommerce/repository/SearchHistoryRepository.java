package com.nhom13.ecommerce.repository;

import com.nhom13.ecommerce.entity.SearchHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    
    // Lấy lịch sử, nhóm theo query và lấy lần tìm kiếm mới nhất
    @Query("SELECT sh.query FROM SearchHistory sh WHERE sh.user.id = :userId GROUP BY sh.query ORDER BY MAX(sh.timestamp) DESC")
    List<String> findRecentQueriesByUserId(Long userId, Pageable pageable);

    Optional<SearchHistory> findFirstByUserIdAndQueryOrderByTimestampDesc(Long userId, String query);
}