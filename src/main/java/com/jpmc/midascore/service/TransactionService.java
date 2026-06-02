package com.jpmc.midascore.service;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import com.jpmc.midascore.foundation.Incentive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final UserRepository userRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final RestTemplate restTemplate;
    private final String incentiveApiUrl;

    public TransactionService(UserRepository userRepository, TransactionRecordRepository transactionRecordRepository, RestTemplate restTemplate, @Value("${incentive.api.url:http://localhost:8080/incentive}") String incentiveApiUrl) {
        this.userRepository = userRepository;
        this.transactionRecordRepository = transactionRecordRepository;
        this.restTemplate = restTemplate;
        this.incentiveApiUrl = incentiveApiUrl;
    }

    @Transactional
    public boolean recordTransaction(Transaction transaction) {
        UserRecord sender = userRepository.findById(transaction.getSenderId());
        UserRecord recipient = userRepository.findById(transaction.getRecipientId());

        if (sender == null || recipient == null) {
            return false;
        }

        if (sender.getBalance() < transaction.getAmount()) {
            return false;
        }

        // Call incentives API
        float incentiveAmount = 0.0f;
        try {
            Incentive incentive = restTemplate.postForObject(incentiveApiUrl, transaction, Incentive.class);
            if (incentive != null) {
                incentiveAmount = incentive.getAmount();
            }
        } catch (Exception e) {
            // If incentive API fails, treat incentive as 0 and continue
            incentiveAmount = 0.0f;
        }

        sender.setBalance(sender.getBalance() - transaction.getAmount());
        recipient.setBalance(recipient.getBalance() + transaction.getAmount() + incentiveAmount);

        TransactionRecord record = new TransactionRecord(sender, recipient, transaction.getAmount());
        record.setIncentive(incentiveAmount);
        transactionRecordRepository.save(record);
        userRepository.save(sender);
        userRepository.save(recipient);

        logger.info("Stored transaction: senderId={} senderBalance={} recipientId={} recipientBalance={} amount={} incentive={}", sender.getId(), sender.getBalance(), recipient.getId(), recipient.getBalance(), transaction.getAmount(), incentiveAmount);

        return true;
    }
}
