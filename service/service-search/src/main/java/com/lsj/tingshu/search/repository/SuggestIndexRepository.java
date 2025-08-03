package com.lsj.tingshu.search.repository;

import com.lsj.tingshu.model.search.SuggestIndex;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuggestIndexRepository extends CrudRepository<SuggestIndex, Long> {

}
