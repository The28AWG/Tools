package ru.tasp.tools.xmpp.accounts;

import ru.tasp.tools.Tools;
import ru.tasp.tools.Validator;
import ru.tasp.tools.logger.L;
import ru.tasp.tools.xmpp.XmppTools;
import ru.tasp.tools.xmpp.accounts.providers.IProvider;
import ru.tasp.tools.xmpp.accounts.providers.preference.PreferenceProvider;

/**
 * Created by the28awg on 25.10.15.
 */
public class XmppAccountManager {
    private static final String KEY_PROVIDER = "ru.tasp.tools.xmpp.accounts.provider";
    private IProvider provider;
    private volatile boolean initialize = false;
    private Validator validator = new Validator(XmppTools.VALID_JAVA_IDENTIFIER);

    private XmppAccountManager() {
        initialize();
    }

    public void initialize() {
        if (!initialize) {
            try {
                if (Tools.getManifestProperty().containsKey(KEY_PROVIDER)) {
                    String sClass = Tools.getManifestProperty().getString(KEY_PROVIDER);
                    if (validator.validate(sClass)) {
                        Class aClass = forName(sClass);
                        if (aClass != null) {
                            try {
                                Object o = aClass.newInstance();
                                if (o instanceof IProvider) {
                                    setProvider((IProvider) o);
                                    if (L.isDebug()) {
                                        L.debug("Provider %s initialize", sClass);
                                    }
                                    initialize = true;
                                } else {
                                    L.debug("Class not provider: %s", sClass);
                                }
                            } catch (InstantiationException ignore) {
                            } catch (IllegalAccessException ignore) {
                            }
                        } else {
                            if (L.isDebug()) {
                                L.debug("Class not found: %s", sClass);
                            }
                        }
                    } else {
                        if (L.isDebug()) {
                            L.debug("Class not valid: %s", sClass);
                        }
                    }
                }
            } catch (NullPointerException ignore) {}
        }
        if (!initialize) {
            setProvider(new PreferenceProvider());
            if (L.isDebug()) {
                L.debug("Provider %s initialize", provider.getClass().getCanonicalName());
            }
            initialize = true;
        }

    }

    private Class<?> forName(String sClass) {
        try {

            return Class.forName(sClass);
        } catch (ClassNotFoundException ignore) {
        }
        return null;
    }

    public IProvider getProvider() {
        return provider;
    }

    public void setProvider(IProvider provider) {
        this.provider = provider;
    }

    public static XmppAccountManager get() {
        return Holder.XMPP_ACCOUNT_MANAGER;
    }

    private static class Holder {
        private static final XmppAccountManager XMPP_ACCOUNT_MANAGER = new XmppAccountManager();
    }
}
