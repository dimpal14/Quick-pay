package com.wp.org;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.razorpay.RazorpayException;
import com.wp.org.service.WalletService;

@RestController
@RequestMapping("/wallet-service")
public class WalletController {

    @Autowired
    private WalletService walletService;
    
    @GetMapping("/payment-status-capture")
    public ResponseEntity<String> handleCallback( @RequestParam("razorpay_payment_link_id") String paymentLinkId,
            @RequestParam(value = "razorpay_payment_id", required = false) String paymentId,
            @RequestParam("razorpay_payment_link_status") String paymentStatus,
            @RequestParam("razorpay_signature") String signature,
            @RequestParam("razorpay_payment_link_reference_id") String paymentLinkReferenceId) throws JsonMappingException, JsonProcessingException, InterruptedException, ExecutionException, RazorpayException {
        	
    	 try {
             boolean isValid = walletService.verifyPaymentSignature(
                     paymentLinkId, paymentId, signature, paymentLinkReferenceId,paymentStatus);

             if (!isValid) {
                 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Signature");
             }

             String responseMessage = walletService.handlePaymentStatus(paymentLinkId, paymentStatus);
             return ResponseEntity.ok(responseMessage);

         } catch (Exception ex) {
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                     .body("Error processing callback: " + ex.getMessage());
         }
    }

    @PostMapping("/add-money-wallet")
    public ResponseEntity<String> addMoney(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader, @RequestParam double amount){
            String base64Credentials = authHeader.substring(6);
            String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
            String userEmail = credentials.split(":", 2)[0];
    	String url=walletService.createPaymentLink(userEmail,amount);
        return ResponseEntity.accepted().body(url);
  }

}
