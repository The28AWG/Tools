package ru.tasp.tools.xmpp.accounts.providers;

import java.util.List;

import ru.tasp.tools.xmpp.accounts.XmppAccount;

/**
 * Created by the28awg on 25.10.15.
 */
public interface IProvider {

    List<String> listIdentifier();

    XmppAccount select(String identifier);

    String insert(XmppAccount account);

    void update(XmppAccount account);

    void delete(XmppAccount account);
}
