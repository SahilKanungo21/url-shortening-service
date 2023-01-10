package com.system.urlshorteningservice.Documents;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Getter
@Setter
@ToString
@Table(name = "url", schema = "url-mapping")
public class URL implements Serializable {
    @Id
    long serialId;
    String longURL;
    String shortURL;
}
