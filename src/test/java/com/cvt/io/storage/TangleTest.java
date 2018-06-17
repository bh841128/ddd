package com.cvt.io.storage;

import com.cvt.io.controllers.TransactionViewModel;
import com.cvt.io.hash.Curl;
import com.cvt.io.hash.Sponge;
import com.cvt.io.hash.SpongeFactory;
import com.cvt.io.model.Hash;
import com.cvt.io.model.Transaction;
import com.cvt.io.storage.Indexable;
import com.cvt.io.storage.Tangle;
import com.cvt.io.storage.rocksDB.RocksDBPersistenceProvider;
import com.cvt.io.utils.Converter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.cvt.io.controllers.TransactionViewModelTest.getRandomTransactionTrits;

import java.util.Arrays;
import java.util.Random;
import java.util.Set;

/**
 * Created by paul on 3/3/17 for iri.
 */
public class TangleTest {
    private final TemporaryFolder dbFolder = new TemporaryFolder();
    private final TemporaryFolder logFolder = new TemporaryFolder();
    private Tangle tangle = new Tangle();

    @Before
    public void setUp() throws Exception {
        TemporaryFolder dbFolder = new TemporaryFolder(), logFolder = new TemporaryFolder();
        dbFolder.create();
        logFolder.create();
        RocksDBPersistenceProvider rocksDBPersistenceProvider;
        rocksDBPersistenceProvider = new RocksDBPersistenceProvider(dbFolder.getRoot().getAbsolutePath(),
                logFolder.getRoot().getAbsolutePath(),1000);
        tangle.addPersistenceProvider(rocksDBPersistenceProvider);
        tangle.init();
    }

    @After
    public void tearDown() throws Exception {
        tangle.shutdown();
    }

    @Test
    public void save() throws Exception {
        Transaction transaction = new Transaction();
        Random r = new Random();
        int[] hash = new int[Curl.HASH_LENGTH],
                trits = Arrays.stream(new int[TransactionViewModel.TRINARY_SIZE])
                        .map(i -> r.nextInt(3)-1).toArray();
        Sponge curl = SpongeFactory.create(SpongeFactory.Mode.CURLP81);
        curl.absorb(trits, 0, trits.length);
        curl.squeeze(hash, 0, Curl.HASH_LENGTH);
        transaction.bytes = Converter.allocateBytesForTrits(trits.length);
        Converter.bytes(trits, transaction.bytes);

        //assertTrue("Should be a new, unique transaction", !Tangle.instance().save(transaction).get());
    }

    @Test
    public void getKeysStartingWithValue() throws Exception {
        int[] trits = getRandomTransactionTrits();
        TransactionViewModel transactionViewModel = new TransactionViewModel(trits, Hash.calculate(SpongeFactory.Mode.CURLP81, trits));
        transactionViewModel.store(tangle);
        Set<Indexable> tag = tangle.keysStartingWith(Transaction.class, Arrays.copyOf(transactionViewModel.getTagValue().bytes(), 15));
        //Assert.assertNotEquals(tag.length, 0);
    }

    @Test
    public void get() throws Exception {
        /*
        Transaction transaction = new Transaction();
        Random r = new Random();
        int[] hash = new int[Curl.HASH_LENGTH],
                trits = Arrays.stream(new int[TransactionViewModel.TRINARY_SIZE])
                        .map(i -> r.nextInt(3)-1).toArray();
        Curl curl = new Curl();
        curl.absorb(trits, 0, trits.length);
        curl.squeeze(hash, 0, Curl.HASH_LENGTH);
        transaction.value = Converter.value(trits);
        byte[] byteHash = Converter.value(hash);

        transaction = (Transaction) Tangle.instance().load(Transaction.class, byteHash).get();
        assertNotNull(transaction);
        assertArrayEquals(transaction.hash, byteHash);
        */
    }

}