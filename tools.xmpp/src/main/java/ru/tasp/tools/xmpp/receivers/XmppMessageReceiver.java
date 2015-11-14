package ru.tasp.tools.xmpp.receivers;

/**
 * Created by the28awg on 26.10.15.
 */
public interface XmppMessageReceiver {

    void onMessage(XmppMessage message);
}
