package com.xtechcn.common.binlog.core.publisher;

import com.xtechcn.common.binlog.core.message.BinlogMessage;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息发送
 *
 * @author Alay
 * @date 2023-05-25 13:07
 */
public class DelegatingMessagePublisher implements MessagePublisher {

    private final List<MessagePublisher> messagePublishers;


    public DelegatingMessagePublisher(MessagePublisher... messagePublishers) {
        Assert.notEmpty(messagePublishers, "messagePublishers cannot be empty");
        this.messagePublishers = new ArrayList<>(messagePublishers.length);
        for (MessagePublisher messagePublisher : messagePublishers) {
            this.messagePublishers.add(messagePublisher);
        }
    }

    @Override
    public void publish(BinlogMessage message) {
        for (MessagePublisher publisher : messagePublishers) {
            publisher.publish(message);
        }
    }


    public void addPublisher(MessagePublisher... publishers) {
        Assert.notNull(publishers, "publisher cannot be empty");
        List<MessagePublisher> addPublishers = new ArrayList<>(publishers.length);
        this.messagePublishers.addAll(addPublishers);
    }

}
