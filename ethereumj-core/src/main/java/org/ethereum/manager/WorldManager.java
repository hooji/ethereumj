package org.ethereum.manager;

import static org.ethereum.config.SystemProperties.CONFIG;

import java.util.*;

import org.ethereum.core.BlockchainImpl;
import org.ethereum.core.Transaction;
import org.ethereum.core.Wallet;
import org.ethereum.crypto.HashUtil;
import org.ethereum.db.RepositoryImpl;
import org.ethereum.facade.Blockchain;
import org.ethereum.facade.Repository;
import org.ethereum.listener.EthereumListener;
import org.ethereum.net.client.PeerClient;
import org.ethereum.net.peerdiscovery.PeerDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

/**
 * WorldManager is a singleton containing references to different parts of the system.
 * 
 * @author Roman Mandeleil 
 * Created on: 01/06/2014 10:44
 */
public class WorldManager {

    private static final Logger logger = LoggerFactory.getLogger("general");

	private Blockchain blockchain;
	private Repository repository;
	private Wallet wallet;

    private PeerClient activePeer;
    private PeerDiscovery peerDiscovery;
    
    private final Set<Transaction> pendingTransactions = Collections.synchronizedSet(new HashSet<Transaction>());

    
    private EthereumListener listener;
    
	private static final class WorldManagerHolder {
		private static final WorldManager instance = new WorldManager();
		static {
			instance.init();
		}
	}
    
	private WorldManager() {
		this.repository = new RepositoryImpl();
		this.blockchain = new BlockchainImpl(repository);
        this.peerDiscovery = new PeerDiscovery();
	}

    // used for testing
    public void reset() {
        this.repository = new RepositoryImpl();
        this.blockchain = new BlockchainImpl(repository);
    }

    public void init() {
    	this.wallet = new Wallet();
        byte[] cowAddr = HashUtil.sha3("cow".getBytes());
        wallet.importKey(cowAddr);

        String secret = CONFIG.coinbaseSecret();
        byte[] cbAddr = HashUtil.sha3(secret.getBytes());
        wallet.importKey(cbAddr);
    }
	
	public static WorldManager getInstance() {
		return WorldManagerHolder.instance;
	}
    
    public void addListener(EthereumListener listener) {
        this.listener = listener;
    }

    public void startPeerDiscovery() {
        if (!peerDiscovery.isStarted())
            peerDiscovery.start();
    }

    public void stopPeerDiscovery() {
        if (peerDiscovery.isStarted())
            peerDiscovery.stop();
    }

    public void addPendingTransactions(Set<Transaction> transactions){
        logger.info("Pending transaction list added: size: [{}]", transactions.size());

        if (listener != null)
            listener.onPendingTransactionsReceived(transactions);
        pendingTransactions.addAll(transactions);
    }

    public void clearPendingTransactions(List<Transaction> recivedTransactions){

        for (Transaction tx : recivedTransactions){
            logger.info("Clear transaction, hash: [{}]", Hex.toHexString(tx.getHash()));
            pendingTransactions.remove(tx);
        }
    }

    public PeerDiscovery getPeerDiscovery() {
    	return peerDiscovery;
    }

    public EthereumListener getListener() {
        return listener;
    }
    
    public void setWallet(Wallet wallet)  {
    	this.wallet = wallet;
    }

	public Repository getRepository() {
		return repository;
	}
	
	public Blockchain getBlockchain() {
		return blockchain;
	}
	
	public void loadBlockchain() {
		this.blockchain = repository.loadBlockchain();
	}

	public Wallet getWallet() {
		return wallet;
	}

    public void setActivePeer(PeerClient peer) {
        this.activePeer = peer;
    }

    public PeerClient getActivePeer() {
        return activePeer;
    }

    public Set<Transaction> getPendingTransactions() {
    	return pendingTransactions;
    }

	public boolean isBlockchainLoading(){
        return blockchain.getQueue().size() > 2;
    }

    public void close() {
        stopPeerDiscovery();
        repository.close();
        blockchain.close();
    }
}
