package com.system.urlshorteningservice.Abstraction;

import com.system.urlshorteningservice.Documents.URL;

public interface IUrlServices {
    public URL saveUrl(String longUrl);
    public long deleteLongUrl(String longUrl);
    public URL updateUrl(String longUrl);
}
