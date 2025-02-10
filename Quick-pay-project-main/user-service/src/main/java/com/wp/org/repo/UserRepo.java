package com.wp.org.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wp.org.entity.User;

@Repository
public interface UserRepo extends JpaRepository<User,Long> {

	User findByEmail(String email);
}
