package com.wp.org.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wp.org.entity.Wallet;

@Repository
public interface WalletRepo extends JpaRepository<Wallet,Long> {

	Wallet findByUserEmail(String email);
}
