package ru.tasp.tools.xmpp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import ru.tasp.tools.Tools;
import ru.tasp.tools.Validator;
import ru.tasp.tools.configuration.Configuration;
import ru.tasp.tools.logger.L;
import ru.tasp.tools.xmpp.accounts.XmppAccount;
import ru.tasp.tools.xmpp.accounts.XmppAccountManager;
import ru.tasp.tools.xmpp.receivers.DefaultXmppMessageReceiver;
import ru.tasp.tools.xmpp.receivers.XmppMessage;
import ru.tasp.tools.xmpp.receivers.XmppMessageReceiver;

/**
 * Created by the28awg on 25.10.15.
 */
public class Xmpp {
    private static final String KEY_RECEIVERS = "ru.tasp.tools.xmpp.receivers";
    private List<XmppMessageReceiver> receivers = new ArrayList<>();
    private LinkedHashMap<String, XmppContext> contexts = new LinkedHashMap<>();
    private Monitor monitor;
    private Sender sender;
    private Validator validator = new Validator(XmppTools.VALID_JAVA_IDENTIFIER);
    private XmppConfiguration configuration;
    private boolean initialize = false;

    private Xmpp() {
        configuration = Configuration.proxy(XmppConfiguration.class);
        initialize();
        monitor = new Monitor(this);
        sender = new Sender(this);
    }

    public static Xmpp get() {
        return Holder.XMPP;
    }

    public void initialize() {
        if (!initialize) {
            if (Tools.getManifestProperty().containsKey(KEY_RECEIVERS)) {
                String sClassList = Tools.getManifestProperty().getString(KEY_RECEIVERS);
                for (String sClass : XmppTools.toList(sClassList, validator)) {
                    Class aClass = XmppTools.forName(sClass);
                    if (aClass != null) {
                        try {
                            Object o = aClass.newInstance();
                            if (o instanceof XmppMessageReceiver) {
                                setReceiver((XmppMessageReceiver) o);
                                if (L.isDebug()) {
                                    L.debug("Receiver %s initialize", sClass);
                                }
                                initialize = true;
                            } else {
                                if (L.isDebug()) {
                                    L.debug("Class not provider: %s", sClass);
                                }
                            }
                        } catch (InstantiationException ignore) {
                        } catch (IllegalAccessException ignore) {
                        }
                    } else {
                        if (L.isDebug()) {
                            L.debug("Class not found: %s", sClass);
                        }
                    }
                }
            }
        }
        if (!initialize) {
            setReceiver(new DefaultXmppMessageReceiver());
            if (L.isDebug()) {
                L.debug("Receiver %s initialize", "DefaultXmppMessageReceiver");
            }
            initialize = true;
        }
    }

    public void send(XmppMessage message) {
        sender.getSendQueue().add(message);
    }

    private void exception(String message) {
        throw new RuntimeException(message);
    }

    public Set<String> getContexts() {
        return contexts.keySet();
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public XmppContext getContext(String identifier) {
        if (!XmppAccountManager.get().getProvider().listIdentifier().contains(identifier)) {
            exception("account not found!");
        }
        if (contexts.containsKey(identifier)) {
            return contexts.get(identifier);
        }
        XmppAccount account = XmppAccountManager.get().getProvider().select(identifier);
        XmppContext context = new XmppContext(account);
        contexts.put(identifier, context);
        monitor.watch(identifier);
        return contexts.get(identifier);
    }

    public List<XmppMessageReceiver> getReceivers() {
        return receivers;
    }

    public void setReceiver(XmppMessageReceiver receiver) {
        receivers.add(receiver);
    }

    public XmppConfiguration getConfiguration() {
        return configuration;
    }

    private static class Holder {
        private static final Xmpp XMPP = new Xmpp();
    }

    public class Sender {
        private BlockingQueue<XmppMessage> sendQueue;
        private Xmpp xmpp;
        private TimerTask runnable = new TimerTask() {

            @Override
            public void run() {
                for (String identifier : xmpp.getMonitor().getAuthenticated().keySet()) {
                    if (xmpp.getMonitor().getAuthenticated().get(identifier).get()) {
                        XmppMessage message;
                        while ((message = xmpp.getContext(identifier).getQueue().poll()) != null) {
                            for (XmppMessageReceiver receiver : xmpp.getReceivers()) {
                                receiver.onMessage(message);
                            }
                        }
                    }
                }
                XmppMessage message;
                while ((message = sendQueue.poll()) != null) {
                    xmpp.getContext(message.getAccount().getIdentifier()).send(message);
                }
            }
        };

        public Sender(Xmpp xmpp) {
            this.xmpp = xmpp;
            sendQueue = new PriorityBlockingQueue<XmppMessage>(10, new Comparator<XmppMessage>() {
                @Override
                public int compare(XmppMessage lhs, XmppMessage rhs) {
                    return lhs.getCreationDate().compareTo(rhs.getCreationDate());
                }
            });
            Timer timer = new Timer("XmppSender", false);
            timer.schedule(runnable, 1000, 1000);
        }

        public BlockingQueue<XmppMessage> getSendQueue() {
            return sendQueue;
        }
    }

    public class Monitor {
        private LinkedHashMap<String, AtomicBoolean> connected = new LinkedHashMap<>();
        private LinkedHashMap<String, AtomicBoolean> authenticated = new LinkedHashMap<>();
        private Xmpp xmpp;
        private TimerTask runnable = new TimerTask() {
            @Override
            public void run() {
                List<String> list = new ArrayList<String>();
                for (String identifier : connected.keySet()) {
                    try {
                        connected.get(identifier).set(xmpp.getContext(identifier).isConnected());
                    } catch (Exception ignore) {
                        list.add(identifier);
                    }
                }
                for (String identifier : authenticated.keySet()) {
                    try {
                        authenticated.get(identifier).set(xmpp.getContext(identifier).isAuthenticated());
                    } catch (Exception ignore) {
                        if (!list.contains(identifier)) {
                            list.add(identifier);
                        }
                    }
                }
                for (String unWatch : list) {
                    unWatch(unWatch);
                }

                if (L.isDebug()) {
                    for (String identifier : connected.keySet()) {
                        L.debug("%s: connected: %s, authenticated: %s, state: %s",
                                identifier,
                                connected.get(identifier).get(),
                                authenticated.get(identifier).get(),
                                xmpp.getContext(identifier).getAccount().getState());
                    }
                    L.debug("unWatch: %s", list.size());
                }
                if (xmpp.getConfiguration().getAutoConnect(false)) {
                    for (String identifier : connected.keySet()) {
                        if (xmpp.getContext(identifier).getAccount().getState() == 1) {
                            if (!connected.get(identifier).get()) {
                                xmpp.getContext(identifier).connect();
                            }
                        }
                    }
                    for (String identifier : XmppAccountManager.get().getProvider().listIdentifier()) {
                        XmppAccount account = XmppAccountManager.get().getProvider().select(identifier);
                        if (account.getState() == 1) {
                            if (!connected.containsKey(identifier)) {
                                xmpp.getContext(identifier).connect();
                            }
                        }
                    }
                }
            }
        };

        private Monitor(Xmpp xmpp) {
            this.xmpp = xmpp;
//            executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
//                @Override
//                public Thread newThread(Runnable r) {
//                    return new Thread(r, "XmppMonitor");
//                }
//            });
            //future = executor.scheduleAtFixedRate(runnable, 0, 10, TimeUnit.SECONDS);
            Timer timer = new Timer("XmppMonitor", false);
            timer.schedule(runnable, 10000, 10000);
        }

        private void watch(String identifier) {
            if (!xmpp.getContexts().contains(identifier)) {
                return;
            }
            if (!connected.containsKey(identifier)) {
                connected.put(identifier, new AtomicBoolean(false));
            }
            if (!authenticated.containsKey(identifier)) {
                authenticated.put(identifier, new AtomicBoolean(false));
            }
        }

        private void unWatch(String identifier) {
            if (!xmpp.getContexts().contains(identifier)) {
                return;
            }
            if (connected.containsKey(identifier)) {
                connected.remove(identifier);
            }
            if (authenticated.containsKey(identifier)) {
                authenticated.remove(identifier);
            }
        }

        public LinkedHashMap<String, AtomicBoolean> getAuthenticated() {
            return authenticated;
        }

        public LinkedHashMap<String, AtomicBoolean> getConnected() {
            return connected;
        }
    }
}
