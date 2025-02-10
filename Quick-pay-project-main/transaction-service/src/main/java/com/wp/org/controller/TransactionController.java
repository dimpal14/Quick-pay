package com.wp.org.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.wp.org.dto.TxnRequestDto;
import com.wp.org.dto.TxnStatusDto;
import com.wp.org.service.TransactionService;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/transaction-service")
public class TransactionController {

    private static Logger LOGGER = LoggerFactory.getLogger(TransactionController.class);

    @Autowired
    private TransactionService transactionService;

    @PostMapping("/txn")
    public ResponseEntity<URI> initTransaction(@RequestBody @Valid TxnRequestDto txnRequestDto, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) throws ExecutionException, InterruptedException, URISyntaxException {
        LOGGER.info("Starting transaction : {}",txnRequestDto);
        String base64Credentials = authHeader.substring(6);
        String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
        String userEmail = credentials.split(":", 2)[0];
        String txnId = transactionService.initTransaction(txnRequestDto,userEmail);
        String transactionStatusUrl = "http://localhost:8082/transaction-service/status/" + txnId;
        LOGGER.info("Transaction Status URL: {}", transactionStatusUrl);
        return ResponseEntity.created(new URI(transactionStatusUrl)).body(new URI(transactionStatusUrl));
    }

    @GetMapping("/status/{txnId}")
    public ResponseEntity<TxnStatusDto> getTxnStatus(@PathVariable String txnId){
        return ResponseEntity.ok(transactionService.getStatus(txnId));
    }

}
