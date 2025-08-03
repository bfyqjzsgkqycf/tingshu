package com.lsj.tingshu.search.repository;

import com.lsj.tingshu.model.search.AlbumInfoIndex;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlbumInfoIndexRepository extends CrudRepository<AlbumInfoIndex, Long> {

}
