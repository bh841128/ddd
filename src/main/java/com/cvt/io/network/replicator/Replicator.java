package com.cvt.io.network.replicator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cvt.io.network.Node;

public class Replicator {
    
    public static final int NUM_THREADS = 32;
    
    private static final Logger log = LoggerFactory.getLogger(Replicator.class);
    private final ReplicatorSinkPool replicatorSinkPool;
    private final int port;
    private ReplicatorSourcePool replicatorSourcePool;

    public Replicator(final Node node, int port, final int maxPeers, final boolean testnet, int transactionPacketSize) {
        this.port = port;
        replicatorSinkPool = new ReplicatorSinkPool(node, port, transactionPacketSize);
        replicatorSourcePool = new ReplicatorSourcePool(replicatorSinkPool, node, maxPeers, testnet);
    }

    public void init() {
        new Thread(replicatorSinkPool).start();
        new Thread(replicatorSourcePool.init(port)).start();
        log.info("Started ReplicatorSourcePool");
    }
    
    public void shutdown() throws InterruptedException {
        // TODO
        replicatorSourcePool.shutdown();
        replicatorSinkPool.shutdown();
    }
    
}
