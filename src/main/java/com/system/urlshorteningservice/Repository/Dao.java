package com.system.urlshorteningservice.Repository;

import com.system.urlshorteningservice.Documents.URL;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Dao extends JpaRepository<URL, Long> {

}
