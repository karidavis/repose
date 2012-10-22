package com.rackspace.papi.service.datastore.impl.redundant.impl;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.impl.redundant.data.Subscriber;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.UUID;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import org.slf4j.LoggerFactory;

public class RedundantCacheDatastoreManager implements DatastoreManager {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(RedundantCacheDatastoreManager.class);
    private static final String CACHE_NAME_PREFIX = "PAPI_REDUNDANT_";
    private final CacheManager cacheManagerInstance;
    private final String cacheName;
    private boolean available;
    private final Set<Subscriber> subscribers;
    private final String address;
    private final int port;
    private RedundantDatastoreImpl datastore;

    public RedundantCacheDatastoreManager(CacheManager cacheManagerInstance, Set<Subscriber> subscribers, String address, int port) {
        this.cacheManagerInstance = cacheManagerInstance;

        cacheName = CACHE_NAME_PREFIX + UUID.randomUUID().toString();
        this.subscribers = subscribers;
        this.address = address;
        this.port = port;

        init();
    }

    private void init() {
        final Cache cache = new Cache(cacheName, 20000, false, false, 5, 2);
        cacheManagerInstance.addCache(cache);

        available = true;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public void destroy() {
        available = false;

        cacheManagerInstance.removeCache(cacheName);
    }

    @Override
    public Datastore getDatastore() {
        try {
            synchronized (this) {
                if (datastore == null) {
                    datastore = new RedundantDatastoreImpl(subscribers, address, port, cacheManagerInstance.getCache(cacheName));
                    datastore.joinGroup();
                }
            }
            return datastore;
        } catch (UnknownHostException ex) {
            LOG.error("Error creating redundant datastore", ex);
        } catch (IOException ex) {
            LOG.error("Error creating redundant datastore", ex);
        }

        return null;
    }

    @Override
    public boolean isDistributed() {
        return true;
    }
}