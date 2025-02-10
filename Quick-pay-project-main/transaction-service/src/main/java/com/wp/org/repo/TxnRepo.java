package com.wp.org.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wp.org.entity.Transaction;

@Repository
public interface TxnRepo extends JpaRepository<Transaction,Long> {
    Transaction findByTxnId(String txnId);
}
