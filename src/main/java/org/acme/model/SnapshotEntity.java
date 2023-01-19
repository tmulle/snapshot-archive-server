package org.acme.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.Optional;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
@ToString
public class SnapshotEntity extends PanacheEntity {

    @Column(unique = true)
    private String fileName;
    private Long fileSize;

    @Column(unique = true)
    private String sha256Hash;

    @Temporal(TemporalType.TIMESTAMP)
    private Date uploadDate;


    public static Optional<SnapshotEntity> findByHashOptional(String sha256Hash) {
        return find("sha256Hash", sha256Hash).firstResultOptional();
    }

    public static void deleteByHash(String sha256Hash) {
        Optional<SnapshotEntity> byHash = findByHashOptional(sha256Hash);
        if (byHash.isPresent()) {
            byHash.get().delete();
        }
    }


}
