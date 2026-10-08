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
}
