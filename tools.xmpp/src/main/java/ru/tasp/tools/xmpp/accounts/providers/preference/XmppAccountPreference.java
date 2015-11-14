package ru.tasp.tools.xmpp.accounts.providers.preference;

/**
 * Created by the28awg on 25.10.15.
 */
public interface XmppAccountPreference {

    String getHost(String host, String prefix);
    void setHost(String host, String prefix);

    String getResource(String resource, String prefix);
    void setResource(String resource, String prefix);

    int getPort(int port, String prefix);
    void setPort(int port, String prefix);

    String getUsername(String username, String prefix);
    void setUsername(String username, String prefix);

    String getPassword(String password, String prefix);
    void setPassword(String password, String prefix);

    String getLastConnectionDate(String lastConnectionDate, String prefix);
    void setLastConnectionDate(String lastConnectionDate, String prefix);

    String getCreationDate(String creationDate, String prefix);
    void setCreationDate(String creationDate, String prefix);

    int getState(int state, String prefix);
    void setState(int state, String prefix);

    String getAccountIdentifiers(String accountIdentifiers);
    void setAccountIdentifiers(String accountIdentifiers);
}
