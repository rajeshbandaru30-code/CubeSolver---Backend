package com.cubesolve.service;

import com.cubesolve.dto.CreateSolveRequest;
import com.cubesolve.dto.SolveRecordDto;
import com.cubesolve.dto.UserStatsDto;
import com.cubesolve.engine.CubeMoves;
import com.cubesolve.engine.CubeState;
import com.cubesolve.model.SolveRecord;
import com.cubesolve.model.User;
import com.cubesolve.repository.SolveRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for retrieving and recording solve history records.
 */
@Service
public class SolveHistoryService {

    private static final Logger log = LoggerFactory.getLogger(SolveHistoryService.class);

    private final SolveRecordRepository solveRecordRepository;

    @Autowired
    public SolveHistoryService(SolveRecordRepository solveRecordRepository) {
        this.solveRecordRepository = solveRecordRepository;
    }

    /**
     * Records a completed solve from Practice Mode.
     * Enforces that the session is genuinely completed and solves the cube.
     */
    @Transactional
    public SolveRecordDto recordSolve(CreateSolveRequest request, User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must be authenticated to record a solve.");
        }
        if (request == null) {
            throw new IllegalArgumentException("Solve request cannot be null.");
        }
        if (request.getMoveCount() <= 0) {
            throw new IllegalArgumentException("Move count must be at least 1.");
        }
        if (request.getSolveTimeMs() <= 0) {
            throw new IllegalArgumentException("Solve time must be positive.");
        }
        if (request.getSolutionMoves() == null || request.getSolutionMoves().isBlank()) {
            throw new IllegalArgumentException("Solution moves cannot be blank.");
        }

        // Verify that the solve is actually complete!
        // Start from solved state, apply scramble (if provided), then apply user's solution moves.
        CubeState state = new CubeState();
        if (request.getScrambleMoves() != null && !request.getScrambleMoves().isBlank()) {
            state = CubeMoves.applyMoves(state, request.getScrambleMoves());
        }
        state = CubeMoves.applyMoves(state, request.getSolutionMoves());

        if (!state.isSolved()) {
            log.warn("Attempted to record an incomplete solve session for user '{}'. Moves did not solve the cube.", user.getUsername());
            throw new IllegalArgumentException("Cannot save incomplete solve session: the moves do not result in a solved cube.");
        }

        SolveRecord record = new SolveRecord();
        record.setCubeState(state.toStorageString());
        record.setScrambleMoves(request.getScrambleMoves() != null ? request.getScrambleMoves().trim() : "");
        record.setSolutionMoves(request.getSolutionMoves().trim());
        record.setMoveCount(request.getMoveCount());
        record.setSolveTimeMs(request.getSolveTimeMs());
        record.setUser(user);

        SolveRecord saved = solveRecordRepository.save(record);
        log.info("Recorded completed practice solve id={} for user '{}' ({} moves, {}ms)",
                saved.getId(), user.getUsername(), saved.getMoveCount(), saved.getSolveTimeMs());

        return toDto(saved);
    }

    /** Get user dashboard statistics computed directly from MySQL. */
    @Transactional(readOnly = true)
    public UserStatsDto getUserStats(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }
        List<SolveRecord> solves = solveRecordRepository.findByUserOrderByCreatedAtDesc(user);
        long totalSolves = solves.size();
        Long bestTimeMs = solves.isEmpty() ? null : solves.stream().mapToLong(SolveRecord::getSolveTimeMs).min().orElse(0L);
        Double averageTimeMs = solves.isEmpty() ? null : Math.round(solves.stream().mapToLong(SolveRecord::getSolveTimeMs).average().orElse(0.0) * 10.0) / 10.0;
        Integer bestMoveCount = solves.isEmpty() ? null : solves.stream().mapToInt(SolveRecord::getMoveCount).min().orElse(0);
        List<SolveRecordDto> recentSolves = solves.stream().limit(5).map(this::toDto).collect(Collectors.toList());

        return new UserStatsDto(totalSolves, bestTimeMs, averageTimeMs, bestMoveCount, recentSolves);
    }

    /** Get the most recent solves (paginated, latest first). */
    @Transactional(readOnly = true)
    public List<SolveRecordDto> getRecentSolves(int page, int size) {
        return solveRecordRepository
            .findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    /** Get all solves for a specific user. */
    @Transactional(readOnly = true)
    public List<SolveRecordDto> getSolvesForUser(User user) {
        if (user == null) {
            return List.of();
        }
        return solveRecordRepository.findByUserOrderByCreatedAtDesc(user)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    /**
     * Get a single solve record by ID, verifying that it belongs to the authenticated user.
     * Prevents Insecure Direct Object Reference (IDOR) attacks.
     */
    @Transactional(readOnly = true)
    public Optional<SolveRecordDto> getSolveByIdForUser(Long id, User user) {
        if (user == null || id == null) {
            return Optional.empty();
        }
        return solveRecordRepository.findByIdAndUser(id, user)
            .map(this::toDto);
    }

    private SolveRecordDto toDto(SolveRecord record) {
        SolveRecordDto dto = new SolveRecordDto();
        dto.setId(record.getId());
        dto.setCubeState(record.getCubeState());
        dto.setScrambleMoves(record.getScrambleMoves());
        dto.setSolutionMoves(record.getSolutionMoves());
        dto.setMoveCount(record.getMoveCount());
        dto.setSolveTimeMs(record.getSolveTimeMs());
        dto.setCreatedAt(record.getCreatedAt());
        if (record.getUser() != null) {
            dto.setUsername(record.getUser().getUsername());
        }
        return dto;
    }
}
