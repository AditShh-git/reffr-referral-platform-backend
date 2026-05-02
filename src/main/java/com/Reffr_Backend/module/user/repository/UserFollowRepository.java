package com.Reffr_Backend.module.user.repository;

import com.Reffr_Backend.module.user.entity.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, UUID> {

    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    long deleteByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    @Query("SELECT uf.follower.id FROM UserFollow uf WHERE uf.following.id = :followingId")
    List<UUID> findFollowerIdsByFollowingId(@Param("followingId") UUID followingId);

    boolean existsByFollowerId(UUID followerId);
}
