package br.com.certacon.poczipfiles.repository;

import br.com.certacon.poczipfiles.model.ZipModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ZipRepository extends JpaRepository<ZipModel, UUID> {
}
