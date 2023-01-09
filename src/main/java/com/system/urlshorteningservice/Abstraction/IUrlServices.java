package com.system.urlshorteningservice.Abstraction;

import com.system.urlshorteningservice.Documents.URL;
import org.apache.zookeeper.KeeperException;

public interface IUrlServices {
    public URL saveUrl(String longUrl) throws InterruptedException, KeeperException;

    public long deleteLongUrl(String longUrl);

    public URL updateUrl(String longUrl);
}
