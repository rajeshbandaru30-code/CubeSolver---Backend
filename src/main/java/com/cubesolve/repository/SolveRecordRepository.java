package com.cubesolve.repository;

import com.cubesolve.model.SolveRecord;
import com.cubesolve.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolveRecordRepository extends JpaRepository<SolveRecord, Long> {
    List<SolveRecord> findByUserOrderByCreatedAtDesc(User user);
    java.util.Optional<SolveRecord> findByIdAndUser(Long id, User user);
    Page<SolveRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByUser(User user);

    /**
     * Optimized single-row SQL aggregation to compute user stats directly in MySQL.
     * Prevents loading all solve entities across the network into memory.
     */
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(s), MIN(s.solveTimeMs), AVG(s.solveTimeMs), MIN(s.moveCount) FROM SolveRecord s WHERE s.user = :user")
    List<Object[]> getUserStatsAggregated(@org.springframework.data.repository.query.Param("user") User user);

    /**
     * Efficiently fetches only the 5 most recent solves using MySQL index.
     */
    List<SolveRecord> findTop5ByUserOrderByCreatedAtDesc(User user);
}
