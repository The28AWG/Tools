package ru.tasp.tools.xmpp.accounts.providers.preference;

import android.content.SharedPreferences;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ru.tasp.tools.Tools;
import ru.tasp.tools.Validator;
import ru.tasp.tools.configuration.Configuration;
import ru.tasp.tools.logger.L;
import ru.tasp.tools.xmpp.XmppTools;
import ru.tasp.tools.xmpp.accounts.XmppAccount;
import ru.tasp.tools.xmpp.accounts.providers.IProvider;

/**
 * Created by the28awg on 25.10.15.
 */
public class PreferenceProvider implements IProvider {

    private static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_NOW);
    private Validator validator = new Validator(XmppTools.IDENTIFIER_PATTERN);
    private IdentifierGenerator generator = new IdentifierGenerator();
    private List<String> accountIdentifiers;
    private LinkedHashMap<String, XmppAccount> cache = new LinkedHashMap<>();

    private XmppAccountPreference preference = null;
    public PreferenceProvider() {
        preference = Configuration.proxy(XmppAccountPreference.class, Tools.getContext());
        accountIdentifiers = XmppTools.toList(preference.getAccountIdentifiers(""), validator);
    }

    @Override
    public List<String> listIdentifier() {
        return accountIdentifiers;
    }

    @Override
    public XmppAccount select(String identifier) {
        if (identifier == null) {
            exception("ID == null");
        }
        if (!validator.validate(identifier)) {
            exception("not valid ID: " + identifier);
        }
        if (!XmppTools.exist(accountIdentifiers, identifier)) {
            exception("ID not found");
        }

        if (L.isDebug()) {
            L.debug("identifier: " + identifier);
        }
        if (cache.containsKey(identifier)) {
            return cache.get(identifier);
        }
        XmppAccount account = new XmppAccount();
        account.setIdentifier(identifier);
        account.setUsername(preference.getUsername("anonymous", identifier));
        account.setHost(preference.getHost("tasp.ufalinux.ru", identifier));
        account.setPassword(preference.getPassword("anonymous", identifier));
        account.setPort(preference.getPort(5222, identifier));
        account.setState(preference.getState(0, identifier));
        account.setResource(preference.getResource("anonymous", identifier));
        Date creationDate = null;
        try {
            creationDate = DATE_FORMAT.parse(preference.getCreationDate("", identifier));
        } catch (ParseException ignore) {
            creationDate = Calendar.getInstance().getTime();
        }
        account.setCreationDate(creationDate);
        Date lastConnectionDate = null;

        try {
            lastConnectionDate = DATE_FORMAT.parse(preference.getLastConnectionDate("", identifier));
        } catch (ParseException ignore) {
        }
        account.setLastConnectionDate(lastConnectionDate);
        cache.put(identifier, account);
        return account;
    }

    @Override
    public String insert(XmppAccount account) {
        String id = generateId();
        String creationDate = "";
        if (account.getCreationDate() != null) {
            creationDate = DATE_FORMAT.format(account.getCreationDate());
        } else {
            Date date = Calendar.getInstance().getTime();
            account.setCreationDate(date);
            creationDate = DATE_FORMAT.format(date);
        }
        String lastConnectionDate = "";
        if (account.getLastConnectionDate() != null) {
            lastConnectionDate = DATE_FORMAT.format(account.getLastConnectionDate());
        }
        preference.setUsername(account.getUsername(), id);
        preference.setPassword(account.getPassword(), id);
        preference.setHost(account.getHost(), id);
        preference.setPort(account.getPort(), id);
        preference.setResource(account.getResource(), id);
        preference.setState(account.getState(), id);
        preference.setCreationDate(creationDate, id);
        preference.setLastConnectionDate(lastConnectionDate, id);
        accountIdentifiers.add(id);
        preference.setAccountIdentifiers(XmppTools.toString(accountIdentifiers, validator));
        account.setIdentifier(id);
        if (L.isDebug()) {
            L.debug("identifier: " + id);
        }
        cache.put(id, account);
        return id;
    }

    @Override
    public void update(XmppAccount account) {
        if (account.getIdentifier() == null) {
            exception("ID == null");
        }
        if (!validator.validate(account.getIdentifier())) {
            exception("not valid ID: " + account.getIdentifier());
        }
        if (!XmppTools.exist(accountIdentifiers, account.getIdentifier())) {
            exception("ID not found");
        }
        if (L.isDebug()) {
            L.debug("identifier: " + account.getIdentifier());
        }
        String creationDate = "";
        if (account.getCreationDate() != null) {
            creationDate = DATE_FORMAT.format(account.getCreationDate());
        } else {
            Date date = Calendar.getInstance().getTime();
            account.setCreationDate(date);
            creationDate = DATE_FORMAT.format(date);
        }
        String lastConnectionDate = "";
        if (account.getLastConnectionDate() != null) {
            lastConnectionDate = DATE_FORMAT.format(account.getLastConnectionDate());
        }
        preference.setUsername(account.getUsername(), account.getIdentifier());
        preference.setPassword(account.getPassword(), account.getIdentifier());
        preference.setHost(account.getHost(), account.getIdentifier());
        preference.setPort(account.getPort(), account.getIdentifier());
        preference.setResource(account.getResource(), account.getIdentifier());
        preference.setState(account.getState(), account.getIdentifier());
        preference.setCreationDate(creationDate, account.getIdentifier());
        preference.setLastConnectionDate(lastConnectionDate, account.getIdentifier());
        if (cache.containsKey(account.getIdentifier())) {
            cache.remove(account.getIdentifier());
            cache.put(account.getIdentifier(), account);
        }
    }

    @Override
    public void delete(XmppAccount account) {
        if (account.getIdentifier() == null) {
            exception("ID == null");
        }
        if (!validator.validate(account.getIdentifier())) {
            exception("not valid ID: " + account.getIdentifier());
        }
        if (!XmppTools.exist(accountIdentifiers, account.getIdentifier())) {
            exception("ID not found");
        }
        if (L.isDebug()) {
            L.debug("identifier: " + account.getIdentifier());
        }
        accountIdentifiers.remove(account.getIdentifier());
        preference.setAccountIdentifiers(XmppTools.toString(accountIdentifiers, validator));
        String namespace = XmppAccountPreference.class.getCanonicalName().toLowerCase();
        String prefix = account.getIdentifier() + ".";
        List<String> delete = new ArrayList<String>();
        for (Map.Entry<String, ?> entry : Configuration.getPreferences().getAll().entrySet()) {
            if (entry.getKey().contains(namespace + "." + prefix)) {
                delete.add(entry.getKey());
            }
        }
        SharedPreferences.Editor editor = Configuration.getPreferences().edit();
        for (String key: delete) {
            editor.remove(key);
        }
        editor.apply();
        if (cache.containsKey(account.getIdentifier())) {
            cache.remove(account.getIdentifier());
        }
    }

    private void exception(String message) {
        throw new RuntimeException(message);
    }

    private String generateId() {
        String id = generator.nextIdentifier();
        if (XmppTools.exist(accountIdentifiers, id)) {
            String tmpId = id;
            for (int s = 0; s < 100; s++) {
                tmpId = generator.nextIdentifier();
                if (!XmppTools.exist(accountIdentifiers, tmpId)) {
                    break;
                }
            }
            if (id.equalsIgnoreCase(tmpId)) {
                exception("ID generate failed.");
            } else {
                id = tmpId;
            }
        }
        return id;
    }
}
