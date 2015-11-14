package ru.tasp.tools.xmpp.accounts.providers.preference;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Created by the28awg on 25.10.15.
 */
public class IdentifierGenerator {
    private SecureRandom random = new SecureRandom();

    public String nextIdentifier() {
        return new BigInteger(130, random).toString(32);
    }
}
