package com.cvt.io.controllers;

import com.cvt.io.model.Hash;
import com.cvt.io.model.Hashes;
import com.cvt.io.model.Transaction;
import com.cvt.io.storage.Indexable;
import com.cvt.io.storage.Persistable;
import com.cvt.io.storage.Tangle;
import com.cvt.io.utils.Pair;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Created by paul on 5/6/17.
 */
public interface HashesViewModel {
    boolean store(Tangle tangle) throws Exception;
    int size();
    boolean addHash(Hash theHash);
    Indexable getIndex();
    Set<Hash> getHashes();
    void delete(Tangle tangle) throws Exception;

    HashesViewModel next(Tangle tangle) throws Exception;
}
