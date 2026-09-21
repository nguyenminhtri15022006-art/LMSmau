package com.lms.identityservice.repository;

import com.lms.identityservice.entity.User;
import com.lms.identityservice.enums.AuthProvider;
import com.lms.identityservice.enums.RoleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderIdAndAuthProvider(String providerId, AuthProvider authProvider);

    // Dùng @Modifying để chọc thẳng xuống DB, bypass qua cơ chế tự động của JPA Auditing
    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.lastLoginAt = :lastLoginAt WHERE u.username = :username")
    void updateLastLoginTime(@Param("username") String username, @Param("lastLoginAt") Instant lastLoginAt);

    @Query("SELECT u FROM User u WHERE " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:keyword IS NULL OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.fullname) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<User> findUsersByFilters(@Param("role") RoleType role,
                                   @Param("keyword") String keyword,
                                   Pageable pageable);

    @Query(value = """
            SELECT
                SUM(CASE WHEN role = 'STUDENT' THEN 1 ELSE 0 END) AS total_students,
                SUM(CASE WHEN role = 'STUDENT' AND created_at >= :startOfWeek THEN 1 ELSE 0 END) AS new_students_this_week,
                SUM(CASE WHEN role = 'INSTRUCTOR' THEN 1 ELSE 0 END) AS total_instructors,
                SUM(CASE WHEN role = 'INSTRUCTOR' AND created_at >= :startOfMonth THEN 1 ELSE 0 END) AS new_instructors_this_month,
                SUM(CASE WHEN role = 'ADMIN' THEN 1 ELSE 0 END) AS total_admins
            FROM users
            """, nativeQuery = true)
    List<Object[]> countUserDashboardStats(@Param("startOfWeek") Instant startOfWeek,
                                           @Param("startOfMonth") Instant startOfMonth);
}
