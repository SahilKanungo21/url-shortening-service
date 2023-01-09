package com.system.urlshorteningservice.Documents;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Getter
@Setter
@ToString
@Table(name = "url", schema = "url-mapping")
public class URL {
    @Id
    long serialId;
    String longURL;
    String shortURL;
}
