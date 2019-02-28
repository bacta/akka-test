package io.bacta.objects;

import akka.japi.pf.ReceiveBuilder;

public class TangibleObjectActor extends ServerObjectActor {
    protected TangibleObjectActor(long id) {
        super(id);
    }

    @Override
    protected ReceiveBuilder appendReceiveHandlers(ReceiveBuilder receiveBuilder) {
        return super.appendReceiveHandlers(receiveBuilder);
    }
}
