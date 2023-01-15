package com.system.urlshorteningservice.Abstraction;

public interface IUrlServices {
    public String getShortUrl(String longUrl) throws Exception;

    public long deleteLongUrl(String longUrl) throws Exception;

    public long updateUrl(String newLongUrl, String longUrl) throws Exception;

    public String mapShortURLToLongURL(String shortUrl);
}
