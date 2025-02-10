package com.wp.org.service;

import com.wp.org.TxnInitPayload;
import com.wp.org.dto.TxnRequestDto;
import com.wp.org.dto.TxnStatusDto;
import com.wp.org.entity.Transaction;
import com.wp.org.enums.TxnStatusEnum;
import com.wp.org.repo.TxnRepo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Service
public class TransactionService {

    private static Logger LOGGER = LoggerFactory.getLogger(TransactionService.class);

    @Autowired
    private TxnRepo txnRepo;

    @Value("${txn.init.topic}")
    private String txnInitTopic;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public String initTransaction(TxnRequestDto txnRequestDto, String userEmail) throws ExecutionException, InterruptedException {
        Transaction transaction = new Transaction();
        transaction.setFromUserEmail(userEmail);
        transaction.setToUserEmail(txnRequestDto.getToUserEmail());
        transaction.setAmount(txnRequestDto.getAmount());
        transaction.setComment(txnRequestDto.getComment());
        transaction.setTxnId(UUID.randomUUID().toString());
        transaction.setStatus(TxnStatusEnum.PENDING);
        transaction = txnRepo.save(transaction);

        TxnInitPayload txnInitPayload = new TxnInitPayload();
        txnInitPayload.setId(transaction.getId());
        txnInitPayload.setFromUserEmail(userEmail);
        txnInitPayload.setToUserEmail(transaction.getToUserEmail());
        txnInitPayload.setAmount(transaction.getAmount());
        txnInitPayload.setRequestId(MDC.get("requestId"));
        Future<SendResult<String,Object>> future  = kafkaTemplate.send(txnInitTopic,transaction.getFromUserEmail().toString(),txnInitPayload);
        LOGGER.info("Pushed txnInitPayload to kafka: {}",future.get());
        return transaction.getTxnId();
    }

    public TxnStatusDto getStatus(String txnId){
        Transaction transaction = txnRepo.findByTxnId(txnId);
        TxnStatusDto txnStatusDto = new TxnStatusDto();
        if(transaction != null){
            txnStatusDto.setStatus(transaction.getStatus().toString());
            txnStatusDto.setReason(transaction.getReason());
        }
        return txnStatusDto;
    }
}
