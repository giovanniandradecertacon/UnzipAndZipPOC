package br.com.certacon.poczipfiles.model;

import br.com.certacon.poczipfiles.utils.StatusZip;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.io.File;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ZipModel {
    @Id
    @GenericGenerator(name = "UUIDGenerator", strategy = "uuid2")
    @GeneratedValue(generator = "UUIDGenerator")
    @Column(name = "zip_id", nullable = false)
    private UUID id;

    @Column(name = "zip_status")
    @Enumerated(EnumType.STRING)
    private StatusZip zipStatus;

    @Column(name = "source_file")
    private File sourceFile;

}
