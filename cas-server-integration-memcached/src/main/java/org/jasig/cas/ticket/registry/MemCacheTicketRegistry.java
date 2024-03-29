package org.jasig.cas.ticket.registry;

import com.google.common.collect.Lists;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.MemcachedClientIF;
import org.jasig.cas.ticket.ServiceTicket;
import org.jasig.cas.ticket.Ticket;
import org.jasig.cas.ticket.TicketGrantingTicket;
import org.jasig.cas.ticket.registry.encrypt.AbstractCrypticTicketRegistry;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Key-value ticket registry implementation that stores tickets in memcached keyed on the ticket ID.
 *
 * @author Scott Battaglia
 * @author Marvin S. Addison
 * @since 3.3
 */
@Component("memcachedTicketRegistry")
public final class MemCacheTicketRegistry extends AbstractCrypticTicketRegistry implements DisposableBean {

    /**
     * Memcached client.
     */
    private MemcachedClientIF client;

    /**
     * TGT cache entry timeout in seconds.
     */
    private int tgtTimeout;

    /**
     * ST cache entry timeout in seconds.
     */
    private int stTimeout;

    /**
     * Instantiates a new Mem cache ticket registry.
     */
    public MemCacheTicketRegistry() {
    }

    /**
     * Creates a new instance that stores tickets in the given memcached hosts.
     *
     * @param hostnames                   Array of memcached hosts where each element is of the form host:port.
     * @param ticketGrantingTicketTimeOut TGT timeout in seconds.
     * @param serviceTicketTimeOut        ST timeout in seconds.
     */
    @Autowired
    public MemCacheTicketRegistry(@Value("${memcached.servers:}")
                                  final String[] hostnames,
                                  @Value("${tgt.maxTimeToLiveInSeconds:28800}")
                                  final int ticketGrantingTicketTimeOut,
                                  @Value("${st.timeToKillInSeconds:10}")
                                  final int serviceTicketTimeOut) {

        try {
            final List<String> hostNamesArray = Arrays.asList(hostnames);
            if (hostNamesArray.isEmpty()) {
                logger.debug("No memcached hosts are define. Client shall not be configured");
            } else {
                logger.info("Setting up Memcached Ticket Registry...");
                this.tgtTimeout = ticketGrantingTicketTimeOut;
                this.stTimeout = serviceTicketTimeOut;

                this.client = new MemcachedClient(AddrUtil.getAddresses(hostNamesArray));
            }
        } catch (final IOException e) {
            throw new IllegalArgumentException("Invalid memcached host specification.", e);
        }

    }

    /**
     * Creates a new instance using the given memcached client instance, which is presumably configured via
     * {@code net.spy.memcached.spring.MemcachedClientFactoryBean}.
     *
     * @param client                      Memcached client.
     * @param ticketGrantingTicketTimeOut TGT timeout in seconds.
     * @param serviceTicketTimeOut        ST timeout in seconds.
     */
    public MemCacheTicketRegistry(final MemcachedClientIF client, final int ticketGrantingTicketTimeOut,
                                  final int serviceTicketTimeOut) {
        this.tgtTimeout = ticketGrantingTicketTimeOut;
        this.stTimeout = serviceTicketTimeOut;
        this.client = client;
    }

    @Override
    protected void updateTicket(final Ticket ticketToUpdate) {
        if (this.client == null) {
            logger.debug("No memcached client is configured.");
            return;
        }

        final Ticket ticket = encodeTicket(ticketToUpdate);
        logger.debug("Updating ticket {}", ticket);
        try {
            if (!this.client.replace(ticket.getId(), getTimeout(ticketToUpdate), ticket).get()) {
                logger.error("Failed updating {}", ticket);
            }
        } catch (final InterruptedException e) {
            logger.warn("Interrupted while waiting for response to async replace operation for ticket {}. "
                + "Cannot determine whether update was successful.", ticket);
        } catch (final Exception e) {
            logger.error("Failed updating {}", ticket, e);
        }
    }

    @Override
    public void addTicket(final Ticket ticketToAdd) {
        if (this.client == null) {
            logger.debug("No memcached client is configured.");
            return;
        }

        final Ticket ticket = encodeTicket(ticketToAdd);
        logger.debug("Adding ticket {}", ticket);
        try {
            if (!this.client.add(ticket.getId(), getTimeout(ticketToAdd), ticket).get()) {
                logger.error("Failed adding {}", ticket);
            }
        } catch (final InterruptedException e) {
            logger.warn("Interrupted while waiting for response to async add operation for ticket {}."
                + "Cannot determine whether add was successful.", ticket);
        } catch (final Exception e) {
            logger.error("Failed adding {}", ticket, e);
        }
    }

    @Override
    public boolean deleteSingleTicket(final String ticketId) {
        try {
            Assert.notNull(this.client, "No memcached client is defined.");
            return this.client.delete(ticketId).get();
        } catch (final Exception e) {
            logger.error("Ticket not found or is already removed. Failed deleting {}", ticketId, e);
        }
        return false;
    }

    @Override
    public Ticket getTicket(final String ticketIdToGet) {
        if (this.client == null) {
            logger.debug("No memcached client is configured.");
            return null;
        }

        final String ticketId = encodeTicketId(ticketIdToGet);
        try {
            final Ticket t = (Ticket) this.client.get(ticketId);
            if (t != null) {
                final Ticket result = decodeTicket(t);
                return getProxiedTicketInstance(result);
            }
        } catch (final Exception e) {
            logger.error("Failed fetching {} ", ticketId, e);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * This operation is not supported.
     */
    @Override
    public Collection<Ticket> getTickets() {
        logger.warn("getTickets() is not supported by {}.", getClass().getSimpleName());
        return Lists.newArrayList();
    }

    /**
     * Destroy the client and shut down.
     *
     * @throws Exception the exception
     */
    @Override
    public void destroy() throws Exception {
        if (this.client == null) {
            return;
        }
        this.client.shutdown();
    }


    @Override
    protected boolean needsCallback() {
        return true;
    }

    /**
     * Gets the timeout value for the ticket.
     *
     * @param t the t
     * @return the timeout
     */
    private int getTimeout(final Ticket t) {
        if (t instanceof TicketGrantingTicket) {
            return this.tgtTimeout;
        } else if (t instanceof ServiceTicket) {
            return this.stTimeout;
        }
        throw new IllegalArgumentException("Invalid ticket type");
    }
}
