package com.lms.blogservice.repository;

import com.lms.blogservice.entity.Vote;
import com.lms.blogservice.enums.TargetType;
import com.lms.blogservice.enums.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    Optional<Vote> findByUserIdAndTargetIdAndTargetType(String userId,
                                                        Long targetId,
                                                        TargetType targetType);
    long countByTargetIdAndTargetTypeAndVoteType(Long targetId,
                                                 TargetType targetType,
                                                 VoteType voteType);
    boolean existsByUserIdAndTargetIdAndTargetType(String userId,
                                                   Long targetId,
                                                   TargetType targetType);

    @Query("SELECT v.targetId, COUNT(v) FROM Vote v " +
           "WHERE v.targetId IN :ids AND v.targetType = :type AND v.voteType = :voteType " +
           "GROUP BY v.targetId")
    List<Object[]> countByTargetIdsAndTypeAndVoteType(
            @Param("ids") Collection<Long> ids,
            @Param("type") TargetType type,
            @Param("voteType") VoteType voteType);
}
