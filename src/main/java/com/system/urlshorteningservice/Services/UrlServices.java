package com.system.urlshorteningservice.Services;

import com.system.urlshorteningservice.Abstraction.IUrlServices;
import com.system.urlshorteningservice.Documents.URL;
import com.system.urlshorteningservice.Repository.Dao;
import com.system.urlshorteningservice.Repository.URLDao;
import com.system.urlshorteningservice.Utils.Constants;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UrlServices implements IUrlServices {
    private final Dao dao;
    private final ZooKeeper zooKeeper;

    private final URLDao urlDao;
    private static final char[] BASE_62_CHARS =
            "zslFQ0b3579AxC4DGJ1KLMdNrORT2UWXYaeIfhHikBmEn6gPo8ptuZvwScyVjq".toCharArray();

    @Autowired
    UrlServices(Dao dao, ZooKeeper zooKeeper, URLDao urlDao) throws Exception {
        this.dao = dao;
        this.zooKeeper = zooKeeper;
        this.urlDao = urlDao;
    }

    private long fetchCounterFromZK() throws InterruptedException, KeeperException {
        if (zooKeeper.exists(Constants.COUNTER_NODE, false) == null) {
            zooKeeper.create(Constants.COUNTER_NODE, "0".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
        return zooKeeper.setData(Constants.COUNTER_NODE, "".getBytes(), -1).getVersion();
    }

    private String B62Encode(long serialId) {
        StringBuilder sb = new StringBuilder();
        do {
            int i = (int) (serialId % 62);
            sb.append(BASE_62_CHARS[i]);
            if (sb.length() == 7) {
                break;
            }
            serialId /= 62;
        } while (serialId > 0);
        while (sb.length() < 7) {
            sb.append('0');
        }
        return Constants.BASE_URL + "/" + sb.reverse();
    }

    public String saveUrl(String longUrl) throws Exception {
        if (!urlDao.CheckIfLongURLExists(longUrl)) {
            long serialId = fetchCounterFromZK();
            String shortUrl = B62Encode(serialId);
            try {
                URL url = new URL();
                url.setLongURL(longUrl);
                url.setShortURL(shortUrl);
                url.setSerialId(serialId);

                URL savedUrl = dao.save(url);
                System.out.println(savedUrl + "successfully saved to DB");
                return savedUrl.getShortURL();
            } catch (Exception ex) {
                throw new Exception(ex.getMessage());
            }
        } else {
            throw new Exception("Long Url already exists");
        }
    }

    public long deleteLongUrl(String longUrl) throws Exception {
        if (urlDao.CheckIfLongURLExists(longUrl)) {
            return urlDao.deleteRecordsAssociateWithLongURL(longUrl);
        } else {
            throw new Exception(longUrl + " does not exists in Db");
        }
    }

    public long updateUrl(String newUrl, String longUrl) throws Exception {
        if (urlDao.CheckIfLongURLExists(longUrl)) {
            return urlDao.updateLongURL(newUrl, longUrl);
        }
        throw new Exception(longUrl + "does not exists in Db");
    }

}
