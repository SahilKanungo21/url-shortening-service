package com.system.urlshorteningservice.Controller;

import com.system.urlshorteningservice.Abstraction.IUrlServices;
import com.system.urlshorteningservice.Exceptions.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/url")
public class UrlController {

    private final IUrlServices iUrlServices;

    @Autowired
    UrlController(IUrlServices iUrlServices) {
        this.iUrlServices = iUrlServices;
    }

    @CrossOrigin("*")
    @GetMapping("/shortUrl/{longUrl}")
    public ResponseEntity<Object> getShortUrl(@PathVariable("longUrl") String longUrl) {
        try {
            return new ResponseEntity<>(iUrlServices.getShortUrl(longUrl), HttpStatus.OK);
        } catch (CustomException ex) {
            return new ResponseEntity<>(ex.getMessage(), ex.getHttpStatus());
        } catch (Exception ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @CrossOrigin("*")
    @DeleteMapping("/shortUrl/{longUrl}")
    public ResponseEntity<Object> deleteLongUrl(@PathVariable("longUrl") String longUrl) {
        try {
            return new ResponseEntity<>(iUrlServices.deleteLongUrl(longUrl), HttpStatus.OK);
        } catch (CustomException ex) {
            return new ResponseEntity<>(ex.getMessage(), ex.getHttpStatus());
        } catch (Exception ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @CrossOrigin("*")
    @PutMapping("/shortUrl/{changedUrl}/{existUrl}")
    public ResponseEntity<Object> updateLongUrl(@PathVariable("changedUrl") String newUrl,
                                                @PathVariable("existUrl") String oldUrl) {
        try {
            return new ResponseEntity<>(iUrlServices.updateUrl(newUrl, oldUrl), HttpStatus.OK);
        } catch (CustomException ex) {
            return new ResponseEntity<>(ex.getMessage(), ex.getHttpStatus());
        } catch (Exception ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}

