package ru.tasp.tools.xmpp.receivers;

import ru.tasp.tools.logger.L;

/**
 * Created by the28awg on 28.10.15.
 */
public class DefaultXmppMessageReceiver implements XmppMessageReceiver {

    @Override
    public void onMessage(XmppMessage message) {
        L.info("%s: from = %s, message = %s", message.getAccount().getIdentifier(), message.getFrom(), message.getMessage());
    }
}
