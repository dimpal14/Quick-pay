package com.wp.org.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.wp.org.TxnCompletedPayload;
import com.wp.org.TxnInitPayload;
import com.wp.org.WalletUpdatedPayload;
import com.wp.org.entity.Wallet;
import com.wp.org.repo.WalletRepo;

@Service
public class WalletService {

    private static Logger LOGGER = LoggerFactory.getLogger(WalletService.class);

    @Autowired
    private WalletRepo walletRepo;

    @Value("${txn.completed.topic}")
    private String txnCompletedTopic;

    @Value("${wallet.updated.topic}")
    private String walletUpdatedTopic;
    
    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Value("${razorpay.callback-url}")
    private String callbackUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private KafkaTemplate<String,Object> kafkaTemplate;
    
    @Transactional
    public void wallerTxn(TxnInitPayload txnInitPayload) throws ExecutionException, InterruptedException {
        Wallet fromWallet = walletRepo.findByUserEmail(txnInitPayload.getFromUserEmail());
        Wallet toWallet = walletRepo.findByUserEmail(txnInitPayload.getToUserEmail());
        
        TxnCompletedPayload txnCompletedPayload = TxnCompletedPayload.builder()
                .id(txnInitPayload.getId())
                .requestId(txnInitPayload.getRequestId())
                .build();
        if(fromWallet==null)
        {
        	txnCompletedPayload.setSuccess(false);
            txnCompletedPayload.setReason("Register the Sender to Create his Wallet");
        }
        else if(fromWallet.getBalance() < txnInitPayload.getAmount()){
            txnCompletedPayload.setSuccess(false);
            txnCompletedPayload.setReason("Low Balance");
        }
        else if(toWallet==null)
        {
        	txnCompletedPayload.setSuccess(false);
            txnCompletedPayload.setReason("Register the Receiver to Create his Wallet");
        }
        else {
            fromWallet.setBalance(fromWallet.getBalance() - txnInitPayload.getAmount());
            toWallet.setBalance(toWallet.getBalance() + txnInitPayload.getAmount());
            txnCompletedPayload.setSuccess(true);

            WalletUpdatedPayload walletUpdatedPayload1 = WalletUpdatedPayload.builder()
                    .userEmail(fromWallet.getUserEmail())
                    .balance(fromWallet.getBalance())
                    .requestId(txnInitPayload.getRequestId())
                    .build();
            WalletUpdatedPayload walletUpdatedPayload2 = WalletUpdatedPayload.builder()
                    .userEmail(toWallet.getUserEmail())
                    .balance(toWallet.getBalance())
                    .requestId(txnInitPayload.getRequestId())
                    .build();

            Future<SendResult<String,Object>> walletUpdatedFuture1  = kafkaTemplate.send(walletUpdatedTopic,walletUpdatedPayload1.getUserEmail(),walletUpdatedPayload1);
            LOGGER.info("Pushed WalletUpdated to kafka: {}",walletUpdatedFuture1.get());

            Future<SendResult<String,Object>> walletUpdatedFuture2  = kafkaTemplate.send(walletUpdatedTopic,walletUpdatedPayload2.getUserEmail(),walletUpdatedPayload2);
            LOGGER.info("Pushed WalletUpdated to kafka: {}",walletUpdatedFuture1.get());
        }
        Future<SendResult<String,Object>> future  = kafkaTemplate.send(txnCompletedTopic,txnInitPayload.getFromUserEmail().toString(),txnCompletedPayload);
        LOGGER.info("Pushed TxnCompleted to kafka: {}",future.get());
    }
    
    public String createPaymentLink(String userEmail,double amount) {
    	LOGGER.info("CHECKING");
    	Wallet wallet = walletRepo.findByUserEmail(userEmail);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(keyId, keySecret);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> paymentLinkRequest = new HashMap<>();
        paymentLinkRequest.put("amount", amount * 100);
        paymentLinkRequest.put("currency", "INR");
        paymentLinkRequest.put("reference_id", UUID.randomUUID().toString());
        paymentLinkRequest.put("description", "Add money to wallet");
        paymentLinkRequest.put("callback_url", callbackUrl);
        paymentLinkRequest.put("callback_method", "get");

        Map<String, Object> customer = new HashMap<>();
        customer.put("name", userEmail);
        paymentLinkRequest.put("customer", customer);
        
        Map<String, String> notes = new HashMap<>();
        notes.put("userEmail", userEmail);
        notes.put("amount", String.valueOf(amount));
        paymentLinkRequest.put("notes", notes);
        
        long expiryDurationMinutes = 16;
        long expiryTimestamp = Instant.now().plusSeconds(expiryDurationMinutes * 60).getEpochSecond();
        paymentLinkRequest.put("expire_by", expiryTimestamp);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(paymentLinkRequest, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            "https://api.razorpay.com/v1/payment_links",
            requestEntity,
            Map.class
        );

        return response.getBody().get("short_url").toString();
    }

	public void paymentStatusSucess(String email, double amount) throws InterruptedException, ExecutionException {
		Wallet wallet = walletRepo.findByUserEmail(email);
        wallet.setBalance(wallet.getBalance() + amount);
        walletRepo.save(wallet);
        WalletUpdatedPayload walletUpdatedPayLoad=WalletUpdatedPayload.builder().userEmail(email)
                .balance(wallet.getBalance() + amount)
                .build();
        Future<SendResult<String,Object>> walletUpdatedFuture  = kafkaTemplate.send(walletUpdatedTopic,walletUpdatedPayLoad.getUserEmail(),walletUpdatedPayLoad);
        LOGGER.info("Pushed WalletUpdated to kafka: {}",walletUpdatedFuture.get());
	}

	public boolean verifyPaymentSignature(String paymentLinkId, String paymentId, String signature,
			String paymentLinkReferenceId, String paymentStatus) throws RazorpayException {
		RazorpayClient razorpayClient=new RazorpayClient(keyId,keySecret);

        JSONObject options = new JSONObject();
        options.put("payment_link_reference_id", paymentLinkReferenceId);
        options.put("razorpay_payment_id", paymentId);
        options.put("payment_link_status", paymentStatus);
        options.put("payment_link_id", paymentLinkId);
        options.put("razorpay_signature", signature);

        return Utils.verifyPaymentLink(options, keySecret);
	}

	public String handlePaymentStatus(String paymentLinkId, String paymentStatus) throws InterruptedException, ExecutionException {
		String apiUrl = "https://api.razorpay.com/v1/payment_links/" + paymentLinkId;
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(keyId,keySecret);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.GET, requestEntity, Map.class);
        Map<String, Object> responseBody = response.getBody();
        Map<String, String> notes = (Map<String, String>) responseBody.get("notes");
        String userEmail = notes.get("userEmail");
        double amount = Double.parseDouble(notes.get("amount"));
	    if ("paid".equals(paymentStatus)) {            	
        	paymentStatusSucess(userEmail, amount);
            return "Payment success processed";
        } else {
            return "Payment failure processed";
        }
	}
}
