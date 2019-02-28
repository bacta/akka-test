package io.bacta.objects;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicInteger;

@Getter
public abstract class ServerObject {
    private static final AtomicInteger idGenerator = new AtomicInteger(1);

    protected final long id = idGenerator.getAndIncrement();
}
