package be.bosa.commons.eid.client.impl;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactory;
import be.bosa.commons.eid.client.spi.Logger;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Created on 24/07/17.
 *
 * @author Reda.Housni-Alaoui
 */
public class CardTerminalsProxy extends CardTerminals {


    private final CardTerminals delegate;
    private final Logger logger;

    public CardTerminalsProxy(CardTerminals delegate, Logger logger) {
        super();
        requireNonNull(delegate);
        this.delegate = delegate;
        this.logger = logger;
    }

    @Override
    public synchronized List<CardTerminal> list(State state) throws CardException {
        return doList(state, true);
    }

    private synchronized List<CardTerminal> doList(
            State state, boolean retryAfterServiceOnFailure) throws CardException {
        try {
            return delegate.list(state);
        } catch (CardException e) {
            if (retryAfterServiceOnFailure) {
                Optional<Throwable> optional =
                        Optional.ofNullable(ExceptionUtils.getRootCause(e))
                                .filter(t -> t.getMessage().contains("SCARD_E_SERVICE_STOPPED")
                                        || t.getMessage().contains("SCARD_E_NO_SERVICE"));
                if (optional.isPresent()) {
                    logger.debug(
                            "Smart card service is on failure. Trying to reload the JDK SmartCard service.");
                    reloadSmartCardService();
                    return doList(state, false);
                }
            }
            throw e;
        }
    }

    /**
     * This is ugly. But necessary ... <br>
     * As long as https://bugs.openjdk.java.net/browse/JDK-8026326 is opened <br>
     * Inspiration found in https://stackoverflow.com/a/26470094/3761154
     */
    private synchronized void reloadSmartCardService() {
        try {
            Class pcscterminal = Class.forName("sun.security.smartcardio.PCSCTerminals");
            Field contextId = pcscterminal.getDeclaredField("contextId");
            contextId.setAccessible(true);

            if (contextId.getLong(pcscterminal) == 0L) {
                return;
            }

            // First get a new context value
            Class pcsc = Class.forName("sun.security.smartcardio.PCSC");
            Method SCardEstablishContext =
                    pcsc.getDeclaredMethod("SCardEstablishContext", Integer.TYPE);
            SCardEstablishContext.setAccessible(true);

            Field SCARD_SCOPE_USER = pcsc.getDeclaredField("SCARD_SCOPE_USER");
            SCARD_SCOPE_USER.setAccessible(true);

            long newId =
                    ((Long) SCardEstablishContext.invoke(pcsc, SCARD_SCOPE_USER.getInt(pcsc)));
            contextId.setLong(pcscterminal, newId);

            // Then clear the terminals in cache
            TerminalFactory factory = TerminalFactory.getDefault();
            CardTerminals terminals = factory.terminals();
            Field fieldTerminals = pcscterminal.getDeclaredField("terminals");
            fieldTerminals.setAccessible(true);
            Class classMap = Class.forName("java.util.Map");
            Method clearMap = classMap.getDeclaredMethod("clear");

            clearMap.invoke(fieldTerminals.get(terminals));
        } catch (ClassNotFoundException
                | IllegalAccessException
                | InvocationTargetException
                | NoSuchMethodException
                | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean waitForChange(long timeout) throws CardException {
        return delegate.waitForChange(timeout);
    }

    @Override
    public CardTerminal getTerminal(String name) {
        return delegate.getTerminal(name);
    }

    /** @return All currently connected SmartCard terminals */
    public static CardTerminals getCardTerminals(Logger logger) {
        TerminalFactory terminalFactory = TerminalFactory.getDefault();
        return new CardTerminalsProxy(terminalFactory.terminals(), logger);
    }
}
