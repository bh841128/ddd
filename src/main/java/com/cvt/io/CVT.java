package com.cvt.io;

import com.cvt.io.conf.Configuration;
import com.cvt.io.controllers.*;
import com.cvt.io.hash.SpongeFactory;
import com.cvt.io.model.Hash;
import com.cvt.io.network.Node;
import com.cvt.io.network.TransactionRequester;
import com.cvt.io.network.UDPReceiver;
import com.cvt.io.network.replicator.Replicator;
import com.cvt.io.service.TipsManager;
import com.cvt.io.storage.FileExportProvider;
import com.cvt.io.storage.Indexable;
import com.cvt.io.storage.Persistable;
import com.cvt.io.storage.PersistenceProvider;
import com.cvt.io.storage.Tangle;
import com.cvt.io.storage.ZmqPublishProvider;
import com.cvt.io.storage.rocksDB.RocksDBPersistenceProvider;
import com.cvt.io.utils.Pair;
import com.cvt.io.zmq.MessageQ;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by paul on 5/19/17.
 */
public class CVT {
	private static final Logger log = LoggerFactory.getLogger(CVT.class);

	public final LedgerValidator ledgerValidator;
	public final Milestone milestone;
	public final Tangle tangle;
	public final TransactionValidator transactionValidator;
	public final TipsManager tipsManager;
	public final TransactionRequester transactionRequester;
	public final Node node;
	public final UDPReceiver udpReceiver;
	public final Replicator replicator;
	public final Configuration configuration;
	public final Hash coordinator;
	public final TipsViewModel tipsViewModel;
	public final MessageQ messageQ;

	public final boolean testnet;
	public final int maxPeers;
	public final int udpPort;
	public final int tcpPort;
	public final int maxTipSearchDepth;

	public CVT(Configuration configuration) {
		this.configuration = configuration;
		testnet = configuration.booling(Configuration.DefaultConfSettings.TESTNET);
		maxPeers = configuration.integer(Configuration.DefaultConfSettings.MAX_PEERS);
		udpPort = configuration.integer(Configuration.DefaultConfSettings.UDP_RECEIVER_PORT);
		tcpPort = configuration.integer(Configuration.DefaultConfSettings.TCP_RECEIVER_PORT);

		String snapshotFile = configuration.string(Configuration.DefaultConfSettings.SNAPSHOT_FILE);
		String snapshotSigFile = configuration.string(Configuration.DefaultConfSettings.SNAPSHOT_SIGNATURE_FILE);
		Snapshot initialSnapshot = Snapshot.init(snapshotFile, snapshotSigFile, testnet).clone();
		long snapshotTimestamp = configuration.longNum(Configuration.DefaultConfSettings.SNAPSHOT_TIME);
		int milestoneStartIndex = configuration.integer(Configuration.DefaultConfSettings.MILESTONE_START_INDEX);
		int numKeysMilestone = configuration.integer(Configuration.DefaultConfSettings.NUMBER_OF_KEYS_IN_A_MILESTONE);
		boolean dontValidateMilestoneSig = configuration
				.booling(Configuration.DefaultConfSettings.DONT_VALIDATE_TESTNET_MILESTONE_SIG);
		int transactionPacketSize = configuration.integer(Configuration.DefaultConfSettings.TRANSACTION_PACKET_SIZE);

		maxTipSearchDepth = configuration.integer(Configuration.DefaultConfSettings.MAX_DEPTH);
		if (testnet) {
			String coordinatorTrytes = configuration.string(Configuration.DefaultConfSettings.COORDINATOR);
			if (StringUtils.isNotEmpty(coordinatorTrytes)) {
				coordinator = new Hash(coordinatorTrytes);
			} else {
				log.warn("No coordinator address given for testnet. Defaulting to "
						+ Configuration.TESTNET_COORDINATOR_ADDRESS);
				coordinator = new Hash(Configuration.TESTNET_COORDINATOR_ADDRESS);
			}
		} else {
			coordinator = new Hash(Configuration.MAINNET_COORDINATOR_ADDRESS);
		}

		Account account = new Account();
		RocksDBPersistenceProvider rdb = new RocksDBPersistenceProvider(
				configuration.string(Configuration.DefaultConfSettings.ACCOUNTDB_PATH),
				configuration.string(Configuration.DefaultConfSettings.ACCOUNTDB_LOG_PATH),
				configuration.integer(Configuration.DefaultConfSettings.DB_CACHE_SIZE));
		rdb.init();
		try {
			account.initAccount(rdb, snapshotFile);
		} catch (Exception e) {
			log.error(e.getMessage());
			System.exit(0);
		}
		tangle = new Tangle();
		messageQ = new MessageQ(configuration.integer(Configuration.DefaultConfSettings.ZMQ_PORT),
				configuration.string(Configuration.DefaultConfSettings.ZMQ_IPC),
				configuration.integer(Configuration.DefaultConfSettings.ZMQ_THREADS),
				configuration.booling(Configuration.DefaultConfSettings.ZMQ_ENABLED));
		tipsViewModel = new TipsViewModel();
		transactionRequester = new TransactionRequester(tangle, messageQ);
		transactionValidator = new TransactionValidator(tangle, tipsViewModel, transactionRequester, messageQ,
				snapshotTimestamp);
		milestone = new Milestone(tangle, coordinator, initialSnapshot, transactionValidator, testnet, messageQ,
				numKeysMilestone, milestoneStartIndex, dontValidateMilestoneSig, account);
		node = new Node(configuration, tangle, transactionValidator, transactionRequester, tipsViewModel, milestone,
				messageQ);
		replicator = new Replicator(node, tcpPort, maxPeers, testnet, transactionPacketSize);
		udpReceiver = new UDPReceiver(udpPort, node,
				configuration.integer(Configuration.DefaultConfSettings.TRANSACTION_PACKET_SIZE));
		ledgerValidator = new LedgerValidator(tangle, milestone, transactionRequester, messageQ);
		tipsManager = new TipsManager(tangle, ledgerValidator, transactionValidator, tipsViewModel, milestone,
				maxTipSearchDepth, messageQ, testnet, milestoneStartIndex);
	}

	public void init() throws Exception {
		initializeTangle();
		tangle.init();

		if (configuration.booling(Configuration.DefaultConfSettings.RESCAN_DB)) {
			rescan_db();
		}
		boolean revalidate = configuration.booling(Configuration.DefaultConfSettings.REVALIDATE);

		if (revalidate) {
			tangle.clearColumn(com.cvt.io.model.Milestone.class);
			tangle.clearColumn(com.cvt.io.model.StateDiff.class);
			tangle.clearMetadata(com.cvt.io.model.Transaction.class);
		}
		milestone.init(SpongeFactory.Mode.CURLP27, ledgerValidator, revalidate);
		transactionValidator.init(testnet, configuration.integer(Configuration.DefaultConfSettings.MWM));
		tipsManager.init();
		transactionRequester.init(configuration.doubling(Configuration.DefaultConfSettings.P_REMOVE_REQUEST.name()));
		udpReceiver.init();
		replicator.init();
		node.init();
	}

	private void rescan_db() throws Exception {
		// delete all transaction indexes
		tangle.clearColumn(com.cvt.io.model.Address.class);
		tangle.clearColumn(com.cvt.io.model.Bundle.class);
		tangle.clearColumn(com.cvt.io.model.Approvee.class);
		tangle.clearColumn(com.cvt.io.model.ObsoleteTag.class);
		tangle.clearColumn(com.cvt.io.model.Tag.class);
		tangle.clearColumn(com.cvt.io.model.Milestone.class);
		tangle.clearColumn(com.cvt.io.model.StateDiff.class);
		tangle.clearMetadata(com.cvt.io.model.Transaction.class);

		// rescan all tx & refill the columns
		TransactionViewModel tx = TransactionViewModel.first(tangle);
		int counter = 0;
		while (tx != null) {
			if (++counter % 10000 == 0) {
				log.info("Rescanned {} Transactions", counter);
			}
			List<Pair<Indexable, Persistable>> saveBatch = tx.getSaveBatch();
			saveBatch.remove(5);
			tangle.saveBatch(saveBatch);
			tx = tx.next(tangle);
		}
	}

	public void shutdown() throws Exception {
		milestone.shutDown();
		tipsManager.shutdown();
		node.shutdown();
		udpReceiver.shutdown();
		replicator.shutdown();
		transactionValidator.shutdown();
		tangle.shutdown();
		messageQ.shutdown();
	}

	private void initializeTangle() {
		String dbPath = configuration.string(Configuration.DefaultConfSettings.DB_PATH);
		if (testnet) {
			if (dbPath.isEmpty() || dbPath.equals("mainnetdb")) {
				// testnetusers must not use mainnetdb, overwrite it unless an explicit name is
				// set.
				configuration.put(Configuration.DefaultConfSettings.DB_PATH.name(), "testnetdb");
				configuration.put(Configuration.DefaultConfSettings.DB_LOG_PATH.name(), "testnetdb.log");
			}
		} else {
			if (dbPath.isEmpty() || dbPath.equals("testnetdb")) {
				// mainnetusers must not use testnetdb, overwrite it unless an explicit name is
				// set.
				configuration.put(Configuration.DefaultConfSettings.DB_PATH.name(), "mainnetdb");
				configuration.put(Configuration.DefaultConfSettings.DB_LOG_PATH.name(), "mainnetdb.log");
			}
		}
		switch (configuration.string(Configuration.DefaultConfSettings.MAIN_DB)) {
		case "rocksdb": {
			tangle.addPersistenceProvider(
					new RocksDBPersistenceProvider(configuration.string(Configuration.DefaultConfSettings.DB_PATH),
							configuration.string(Configuration.DefaultConfSettings.DB_LOG_PATH),
							configuration.integer(Configuration.DefaultConfSettings.DB_CACHE_SIZE)));
			break;
		}
		default: {
			throw new NotImplementedException("No such database type.");
		}
		}
		if (configuration.booling(Configuration.DefaultConfSettings.EXPORT)) {
			tangle.addPersistenceProvider(new FileExportProvider());
		}
		if (configuration.booling(Configuration.DefaultConfSettings.ZMQ_ENABLED)) {
			tangle.addPersistenceProvider(new ZmqPublishProvider(messageQ));
		}
	}
}
