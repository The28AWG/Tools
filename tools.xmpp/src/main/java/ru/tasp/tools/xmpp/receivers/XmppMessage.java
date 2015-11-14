package ru.tasp.tools.xmpp.receivers;

import java.util.Calendar;
import java.util.Date;

import ru.tasp.tools.xmpp.accounts.XmppAccount;
import ru.tasp.tools.xmpp.accounts.XmppAccountManager;

/**
 * Created by the28awg on 29.10.15.
 */
public class XmppMessage {

    private XmppAccount account;
    private String from;
    private String message;
    private Date creationDate;

    public XmppMessage(XmppAccount account, String from, String message) {
        this.account = account;
        this.from = from;
        this.message = message;
        creationDate = Calendar.getInstance().getTime();
    }

    public XmppMessage(String identifier, String from, String message) {
        this.account = XmppAccountManager.get().getProvider().select(identifier);
        this.from = from;
        this.message = message;
        creationDate = Calendar.getInstance().getTime();
    }

    public XmppAccount getAccount() {
        return account;
    }

    public String getFrom() {
        return from;
    }

    public String getMessage() {
        return message;
    }

    public Date getCreationDate() {
        return creationDate;
    }
}
