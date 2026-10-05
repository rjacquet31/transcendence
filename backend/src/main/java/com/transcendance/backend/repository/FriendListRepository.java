package com.transcendance.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.transcendance.backend.model.FriendList;
import com.transcendance.backend.model.FriendshipStatus;

public interface FriendListRepository extends JpaRepository<FriendList, Long> {

    Optional<FriendList> findByUserId1AndUserId2(UUID userId1, UUID userId2);

    List<FriendList> findByUserId1OrUserId2(UUID userId1, UUID userId2);

    @Query("""
            SELECT CASE WHEN f.userId1 = :userId THEN f.userId2 ELSE f.userId1 END
            FROM FriendList f
            WHERE (f.userId1 = :userId OR f.userId2 = :userId)
              AND f.status = :status
            """)
    List<UUID> findFriendIdsByStatus(@Param("userId") UUID userId,
            @Param("status") FriendshipStatus status);
}
