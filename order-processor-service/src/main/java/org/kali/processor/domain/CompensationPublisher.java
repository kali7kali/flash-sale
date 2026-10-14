package org.kali.processor.domain;

public interface CompensationPublisher {

    void publish(CompensationMessage message);
}
