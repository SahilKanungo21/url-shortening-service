package com.system.urlshorteningservice.Documents;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Data
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "url", schema = "url-mapping")
public class URL implements Serializable {
    @Id
    long serialId;
    String longURL;
    String shortURL;
}
