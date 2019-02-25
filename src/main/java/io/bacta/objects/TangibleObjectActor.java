package io.bacta.objects;

import akka.japi.pf.ReceiveBuilder;

public class TangibleObjectActor extends ServerObjectActor {
    @Override
    protected ReceiveBuilder appendReceiveHandlers(ReceiveBuilder receiveBuilder) {
        return super.appendReceiveHandlers(receiveBuilder);
    }
}
