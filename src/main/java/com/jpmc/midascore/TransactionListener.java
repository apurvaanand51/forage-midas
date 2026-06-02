package com.jpmc.midascore;

import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TransactionListener {

    private static final Logger logger = LoggerFactory.getLogger(TransactionListener.class);
    private final TransactionService transactionService;
    private final List<Transaction> received = new ArrayList<>();

    public TransactionListener(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @KafkaListener(topics = "${general.kafka-topic}", groupId = "midas-core-group", containerFactory = "kafkaListenerContainerFactory")
    public void receive(Transaction transaction) {
        received.add(transaction);
        boolean stored = transactionService.recordTransaction(transaction);
        if (stored) {
            if (received.size() <= 4) {
                logger.info("Received transaction {}: amount={} senderId={} recipientId={}", received.size(), transaction.getAmount(), transaction.getSenderId(), transaction.getRecipientId());
            } else {
                logger.debug("Received additional transaction: {}", transaction);
            }
        } else {
            logger.warn("Discarded invalid transaction: {}", transaction);
        }
    }

    public List<Transaction> getReceived() {
        return new ArrayList<>(received);
    }
}
