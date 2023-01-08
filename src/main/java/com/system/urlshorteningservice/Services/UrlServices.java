package com.system.urlshorteningservice.Services;

import com.system.urlshorteningservice.Abstraction.IUrlServices;
import com.system.urlshorteningservice.Documents.URL;
import org.springframework.stereotype.Service;

@Service
public class UrlServices implements IUrlServices {

    public URL saveUrl(String longUrl) {
        return null;
    }

    public long deleteLongUrl(String longUrl) {
        return 0;
    }

    public URL updateUrl(String longUrl) {
        return null;
    }
}
