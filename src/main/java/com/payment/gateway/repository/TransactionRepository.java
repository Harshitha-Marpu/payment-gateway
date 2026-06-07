package com.payment.gateway.repository;

import com.payment.gateway.entity.Transaction;
import com.payment.gateway.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    // Find a transaction by idempotency key
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    // Find all transactions for a merchant
    List<Transaction> findByMerchantId(String merchantId);

    // Find all transactions in a particular status
    List<Transaction> findByStatus(TransactionStatus status);

    // Check if idempotency key already exists
    boolean existsByIdempotencyKey(String idempotencyKey);
}