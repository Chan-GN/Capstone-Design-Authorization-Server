package com.hansung.hansungauthorizationserver.repository;

import com.hansung.hansungauthorizationserver.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.studentId =:studentId")
    Optional<User> findByStudentId(String studentId);
}
