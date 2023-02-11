package com.system.urlshorteningservice.Controller;

import com.system.urlshorteningservice.Abstraction.IUrlServices;
import com.system.urlshorteningservice.Exceptions.CustomException;
import com.system.urlshorteningservice.Utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/url")
public class UrlController {

    Logger LOGGER = LoggerFactory.getLogger(UrlController.class);

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

    @GetMapping("/{shortUrl}")
    public RedirectView redirectUrl(@PathVariable("shortUrl") String id) {
        LOGGER.debug("Received shortened url to redirect: " + id);
        // Extract the ID from shortUrl
        String[] uniqueId = id.split("/") ;
        String redirectUrl = iUrlServices.mapShortURLToLongURL(Constants.BASE_URL+"/"+uniqueId[1]);
        RedirectView redirectView = new RedirectView();
        redirectView.setUrl("https//"+redirectUrl);
        return redirectView;
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

