package ru.tasp.tools.xmpp;

/**
 * Created by the28awg on 28.10.15.
 */
public interface XmppConfiguration {

    boolean getServiceStart(boolean serviceStart);
    void setServiceStart(boolean serviceStart);

    boolean getAutoConnect(boolean autoConnect);
    void setAutoConnect(boolean autoConnect);
}
