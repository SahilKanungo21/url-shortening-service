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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class UrlServices implements IUrlServices {
    Logger LOGGER = LoggerFactory.getLogger(UrlServices.class);

    private final Dao dao;
    private final ZooKeeper zooKeeper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final URLDao urlDao;

    // formatting all the data from cache
    public String cleanUpCache() {
        redisTemplate.execute((RedisCallback<String>) connection -> {
            connection.flushDb();
            return "Cache cleared successfully!";
        });
        return "";
    }

    private static final char[] BASE_62_CHARS =
            "zslFQ0b3579AxC4DGJ1KLMdNrORT2UWXYaeIfhHikBmEn6gPo8ptuZvwScyVjq".toCharArray();

    @Autowired
    UrlServices(Dao dao, ZooKeeper zooKeeper, URLDao urlDao, RedisTemplate<String, Object> redisTemplate) {
        this.dao = dao;
        this.zooKeeper = zooKeeper;
        this.urlDao = urlDao;
        this.redisTemplate = redisTemplate;
    }

    private long fetchCounterFromZK() throws InterruptedException, KeeperException {
        if (zooKeeper.exists(Constants.COUNTER_NODE, false) == null) {
            zooKeeper.create(Constants.COUNTER_NODE, "0".getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
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
     * Get short Url from Db if exists ,
     * If not create a new Short Url .
     * what if long url exists , then fetch the long url from Redis ,
     * If not available on redis , then  fetch from Db
     * <p>
     * what if long url present in db but not in redis
     * then cache the long url from db .
     */
    private void mostRecentlyUsedDataFromCache(Object key){
        redisTemplate.opsForList().rightPush("MRU-list", key);
        redisTemplate.opsForList().trim("MRU-list", 0, 1000);
        LOGGER.info("Most recently Used URLs are "+redisTemplate.opsForValue().get("MRU-list"));
    }

    /**
     * TODO : Replace the key from long url to short url
     * TODO : Clean up policy
     */

    private String getShortUrlFromCache(URL url) {
        if (Boolean.FALSE.equals(redisTemplate.hasKey(url.getLongURL()))) {
            redisTemplate.opsForValue().set(url.getShortURL(), url);
        }
        URL dataFromCache = (URL) redisTemplate.opsForValue().get(url.getLongURL());
        System.out.println(dataFromCache);
        assert dataFromCache != null;
        //mostRecentlyUsedDataFromCache(dataFromCache.getLongURL());
        return dataFromCache.getShortURL();

    }

    @Cacheable(cacheNames = "url-shorterner", key = "#longUrl")
    public String getShortUrl(String longUrl)
            throws InterruptedException, KeeperException {
        if (Boolean.FALSE.equals(redisTemplate.hasKey(longUrl))) {
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
            return fetchShortUrlFromCache(longUrl);
        }
    }

    private String fetchShortUrlFromCache(String longUrl) {
        URL cacheData = (URL) redisTemplate.opsForValue().get(longUrl);
        assert cacheData != null;
        LOGGER.info(longUrl + " fetched from Cache");
       // mostRecentlyUsedDataFromCache(cacheData.getLongURL());
        return cacheData.getShortURL();
    }

    private void deleteLongUrlFromCache(String longUrl) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(longUrl))) {
            try {
                redisTemplate.delete(longUrl);
                LOGGER.info(longUrl + " Deleted Successfully from Cache!!");
            } catch (CustomException ex) {
                LOGGER.error(longUrl + " deletion failed in Cache", ex);
                throw new CustomException(longUrl + " deletion failed in Cache", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }

    /**
     * What if the long url deleted from db, but it still persists on cache
     * We have to delete the record from cache also.
     */
    @CacheEvict(cacheNames = "url-shortener", key = "#longUrl")
    public long deleteLongUrl(String longUrl) {
        if (urlDao.CheckIfLongURLExistsInDB(longUrl)) {
            try {
                long noOfRecordsDeleted = urlDao.deleteRecordsAssociateWithLongURL(longUrl);
                LOGGER.info(noOfRecordsDeleted + " records deleted that are associated with "
                        + longUrl + "from DB");
                // delete the long url from redis cache also
                deleteLongUrlFromCache(longUrl);
                return noOfRecordsDeleted;
            } catch (CustomException ex) {
                LOGGER.error(longUrl + " deletion failed !!");
                throw new CustomException(longUrl + " can not be deleted .", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            // delete the same from redis cache
        } else {
            throw new CustomException(longUrl + " does not exists in Db", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * The new url updated in the db must be deleted from cache
     */
    public long updateUrl(String newUrl, String longUrl) {
        if (urlDao.CheckIfLongURLExistsInDB(longUrl)) {
            try {
                long noOfUpdatedRecords = urlDao.updateLongURL(newUrl, longUrl);
                LOGGER.info(noOfUpdatedRecords + " records updated that are associated with "
                        + longUrl + "from DB");
                deleteLongUrlFromCache(longUrl);
                LOGGER.info("deleted existingURL from Cache Successfully " + longUrl);
                return noOfUpdatedRecords;
            } catch (CustomException ex) {
                LOGGER.error(longUrl + " update failed !!");
                throw new CustomException(longUrl + " can not be updated .", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        throw new CustomException(longUrl + "does not exists in Db", HttpStatus.BAD_REQUEST);
    }

    public String mapShortURLToLongURL(String shortUrl) {
        // fetch the long url and check if that url present in redis or not
        String longurl = urlDao.getLongUrlFromDB(shortUrl);
        redisTemplate.hasKey(longurl);
        return longurl;
    }
}
