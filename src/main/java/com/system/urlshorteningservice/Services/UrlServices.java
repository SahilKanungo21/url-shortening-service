package com.system.urlshorteningservice.Services;

import com.system.urlshorteningservice.Abstraction.IUrlServices;
import com.system.urlshorteningservice.Documents.URL;
import com.system.urlshorteningservice.Exceptions.CustomException;
import com.system.urlshorteningservice.Repository.Dao;
import com.system.urlshorteningservice.Repository.URLDao;
import com.system.urlshorteningservice.Utils.Constants;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class UrlServices implements IUrlServices {
    Logger LOGGER = LoggerFactory.getLogger(UrlServices.class);
    private final Dao dao;
    private final ZooKeeper zooKeeper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final URLDao urlDao;
    private static final char[] BASE_62_CHARS =
            "zslFQ0b3579AxC4DGJ1KLMdNrORT2UWXYaeIfhHikBmEn6gPo8ptuZvwScyVjq".toCharArray();

    @Autowired
    UrlServices(Dao dao, ZooKeeper zooKeeper, URLDao urlDao, RedisTemplate<String, Object> redisTemplate) throws Exception {
        this.dao = dao;
        this.zooKeeper = zooKeeper;
        this.urlDao = urlDao;
        this.redisTemplate = redisTemplate;
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

    /**
     * Get short Url from Db if exists
     * If not create a new Short Url .
     * what if long url exists , then fetch the long url from Redis ,
     * If not available on redis , then  fetch from Db
     * <p>
     * what if long url present in db but not in redis
     * then cache the long url from db .
     */

    private String getShortUrlFromCache(URL url) {
        if (Boolean.FALSE.equals(redisTemplate.hasKey(url.getLongURL()))) {
            redisTemplate.opsForValue().set(url.getLongURL(), url);
        }
        URL dataFromCache = (URL) redisTemplate.opsForValue().get(url.getLongURL());
        System.out.println(dataFromCache);
        assert dataFromCache != null;
        return dataFromCache.getShortURL();

    }

    @Cacheable("#longUrl")
    public String getShortUrl(String longUrl) throws InterruptedException, KeeperException {
        if (!urlDao.CheckIfLongURLExistsInDB(longUrl)) {
            long serialId = fetchCounterFromZK();
            String shortUrl = B62Encode(serialId);
            try {
                URL url = new URL();
                url.setLongURL(longUrl);
                url.setShortURL(shortUrl);
                url.setSerialId(serialId);

                URL savedUrl = dao.save(url);
                LOGGER.info(savedUrl + "successfully saved to MySql");
                return getShortUrlFromCache(savedUrl);
            } catch (CustomException ex) {
                LOGGER.error(ex.getMessage());
                throw new CustomException(ex.getMessage(), HttpStatus.BAD_REQUEST);
            }
        } else {
            // If url present in db but not present in 20% of available data in Redis
            // stmt1 : if present in redis , fetch from redis (store policy in redis is MFU 20% of unique url
            // stmt1 will be verified if delete policy is LRU
            // stmt2 : if not in redis , fetch from db and update the redis using MRU policy
            // fetch from db and added it MOST RECENTLY USED URL in redis
            // search in Redis Cache if Not present db call
            return fetchShortUrlIfExistsFromCache(longUrl);
        }
    }

    private String fetchShortUrlIfExistsFromCache(String longUrl) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(longUrl))) {
            URL cacheData = (URL) redisTemplate.opsForValue().get(longUrl);
            assert cacheData != null;
            LOGGER.info(longUrl + " fetched from Cache");
            return cacheData.getShortURL();
        }
        LOGGER.info(longUrl + " fetched from DB");
        return urlDao.getShortUrlFromDB(longUrl);
    }

    /**
     * What if the long url deleted from db but it still persists on cache
     * We have to delete the record from cache also.
     *
     * @param longUrl
     * @return
     * @throws CustomException
     */
    public long deleteLongUrl(String longUrl) {
        if (urlDao.CheckIfLongURLExistsInDB(longUrl)) {
            return urlDao.deleteRecordsAssociateWithLongURL(longUrl);
        } else {
            throw new CustomException(longUrl + " does not exists in Db", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * The new url updated in the db must be reflected to existing url in the cache
     *
     * @param newUrl
     * @param longUrl
     * @return
     * @throws CustomException
     */
    public long updateUrl(String newUrl, String longUrl) {
        if (urlDao.CheckIfLongURLExistsInDB(longUrl)) {
            return urlDao.updateLongURL(newUrl, longUrl);
        }
        throw new CustomException(longUrl + "does not exists in Db", HttpStatus.BAD_REQUEST);
    }

}
