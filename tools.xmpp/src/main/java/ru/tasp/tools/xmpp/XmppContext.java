package ru.tasp.tools.xmpp;

import org.jivesoftware.smack.AbstractConnectionClosedListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.offline.OfflineMessageManager;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.util.Calendar;
import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import ru.tasp.tools.Threads;
import ru.tasp.tools.bus.Bus;
import ru.tasp.tools.bus.Subscribe;
import ru.tasp.tools.logger.L;
import ru.tasp.tools.network.Network;
import ru.tasp.tools.network.NetworkChangeEvent;
import ru.tasp.tools.xmpp.accounts.XmppAccount;
import ru.tasp.tools.xmpp.accounts.XmppAccountManager;
import ru.tasp.tools.xmpp.events.XmppEventConnect;
import ru.tasp.tools.xmpp.events.XmppEventDisconnect;
import ru.tasp.tools.xmpp.receivers.XmppMessage;

/**
 * Created by the28awg on 26.10.15.
 */
public class XmppContext {

    private boolean connected = false;
    private boolean connecting = false;
    private boolean authenticated = false;
    private XmppAccount account;
    private XMPPTCPConnection connection;
    private PriorityBlockingQueue<XmppMessage> queue;
    private PriorityBlockingQueue<XmppMessage> sendQueue;

    public XmppContext(XmppAccount account) {
        this.account = account;
        this.queue = new PriorityBlockingQueue<XmppMessage>(10, new Comparator<XmppMessage>() {
            @Override
            public int compare(XmppMessage lhs, XmppMessage rhs) {
                return lhs.getCreationDate().compareTo(rhs.getCreationDate());
            }
        });
        this.sendQueue = new PriorityBlockingQueue<XmppMessage>(10, new Comparator<XmppMessage>() {
            @Override
            public int compare(XmppMessage lhs, XmppMessage rhs) {
                return lhs.getCreationDate().compareTo(rhs.getCreationDate());
            }
        });
    }

    public void connect() {
        Bus.get().register(this);
        account.setState(1);
        if (Network.isNetworkAvailable()) {
            checkConnect();
        }
    }

    public void disconnect() {
        Bus.get().unRegister(this);
        account.setState(0);
        checkDisconnect();
    }

    private void checkDisconnect() {
        if (isConnected()) {
            Threads.get().getExecutor().execute(new Disconnect(this));
            XmppAccountManager.get().getProvider().update(account);
            Bus.get().fire(new XmppEventDisconnect());
        }
    }

    @Subscribe(ui = true)
    private void network(NetworkChangeEvent networkChangeEvent) {
        if (Network.isNetworkAvailable()) {
            checkConnect();
        } else {
            checkDisconnect();
        }
    }
    public PriorityBlockingQueue<XmppMessage> getQueue() {
        return queue;
    }

    protected void send(XmppMessage message) {
        sendQueue.add(message);
        checkSend();
    }

    private void checkSend() {
        if (isAuthenticated()) {
            XmppMessage message;
            while ((message = sendQueue.poll()) != null) {
                Message stanza = new Message();
                stanza.setFrom(connection.getUser());
                try {
                    stanza.setTo(JidCreate.from(message.getFrom(), message.getAccount().getHost(), ""));
                } catch (XmppStringprepException e) {
                    e.printStackTrace();
                }
                stanza.setBody(message.getMessage());
                try {
                    connection.sendStanza(stanza);
                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void checkConnect() {
        if (!isConnected()) {
            if (!connecting) {
                connecting = true;
                Threads.get().getExecutor().execute(new Threads.Task<XMPPTCPConnection>(new Connect(this)) {
                    @Override
                    public void done(XMPPTCPConnection result) {
                        connecting = false;
                        connection = result;
                        account.setLastConnectionDate(Calendar.getInstance().getTime());
                        XmppAccountManager.get().getProvider().update(account);
                        StanzaFilter filter = MessageTypeFilter.NORMAL_OR_CHAT_OR_HEADLINE;
                        StanzaListener stanzaListener = new StanzaListener() {
                            public void processPacket(Stanza packet) {
                                Message message = (Message) packet;
                                String from;
                                if (message.getFrom().hasLocalpart()) {
                                    from = message.getFrom().getLocalpartOrNull().toString();
                                } else {
                                    from = message.getFrom().toString();
                                }
                                String body = message.getBody();
                                if (!XmppTools.isEmpty(body)) {
                                    queue.add(new XmppMessage(account, from, body));
                                }
                            }
                        };
                        connection.addAsyncStanzaListener(stanzaListener, filter);
                        OfflineMessageManager manager = new OfflineMessageManager(connection);
                        try {
                            if (manager.supportsFlexibleRetrieval()) {
                                if (manager.getMessageCount() != 0) {
                                    for (Message message : manager.getMessages()) {
                                        String from;
                                        if (message.getFrom().hasLocalpart()) {
                                            from = message.getFrom().getLocalpartOrNull().toString();
                                        } else {
                                            from = message.getFrom().toString();
                                        }
                                        String body = message.getBody();
                                        if (!XmppTools.isEmpty(body)) {
                                            queue.add(new XmppMessage(account, from, body));
                                        }
                                    }
                                }
                            }
                        } catch (SmackException.NoResponseException e) {
                            e.printStackTrace();
                        } catch (XMPPException.XMPPErrorException e) {
                            e.printStackTrace();
                        } catch (SmackException.NotConnectedException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                        checkSend();
                        Bus.get().fire(new XmppEventConnect());
                    }
                });
            }
        }
    }

    public XmppAccount getAccount() {
        return account;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    protected XMPPTCPConnection getConnection() {
        return connection;
    }

    private class Connect implements Callable<XMPPTCPConnection> {
        private CountDownLatch latch = new CountDownLatch(1);

        private XmppContext context;

        public Connect(XmppContext context) {
            this.context = context;
        }

        @Override
        public XMPPTCPConnection call() throws Exception {
            XMPPTCPConnectionConfiguration configuration = XMPPTCPConnectionConfiguration.builder()
                    .setHost(context.getAccount().getHost())
                    .setUsernameAndPassword(context.getAccount().getUsername(), context.getAccount().getPassword())
                    .setServiceName(JidCreate.domainBareFrom(context.getAccount().getHost()))
                    .setResource(context.getAccount().getResource())
                    .setPort(context.getAccount().getPort())
                    .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                    .setDebuggerEnabled(L.isTrace())
                    .build();
            connection = new XMPPTCPConnection(configuration);
            connection.addConnectionListener(new AbstractConnectionClosedListener() {
                @Override
                public void connectionTerminated() {
                    context.setConnected(false);
                    context.setAuthenticated(false);
                }

                @Override
                public void connected(XMPPConnection x) {
                    context.setConnected(true);
                    try {
                        SASLAuthentication.unBlacklistSASLMechanism("PLAIN");
                        SASLAuthentication.blacklistSASLMechanism("DIGEST-MD5");
                        Roster.getInstanceFor(connection).setRosterLoadedAtLogin(false);
                        Roster.getInstanceFor(connection).setSubscriptionMode(Roster.SubscriptionMode.accept_all);
                        connection.login();
                    } catch (XMPPException e) {
                        e.printStackTrace();
                    } catch (SmackException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void authenticated(XMPPConnection connection, boolean resumed) {
                    try {
                        connection.sendStanza(new Presence(Presence.Type.available, "Hello!", 0, Presence.Mode.available));
                    } catch (SmackException.NotConnectedException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    latch.countDown();
                    context.setAuthenticated(true);
                }
            });
            connection.connect();
            latch.await(1, TimeUnit.MINUTES);
            return connection;
        }
    }

    private class Disconnect implements Runnable {

        private XmppContext context;

        public Disconnect(XmppContext context) {
            this.context = context;
        }

        @Override
        public void run() {
            try {
                context.getConnection().disconnect(new Presence(Presence.Type.unavailable, "Bye!", 0, Presence.Mode.available));
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            }
        }
    }
}
