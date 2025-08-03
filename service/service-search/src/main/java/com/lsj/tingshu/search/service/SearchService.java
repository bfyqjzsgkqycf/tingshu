package com.lsj.tingshu.search.service;

import com.lsj.tingshu.query.search.AlbumIndexQuery;
import com.lsj.tingshu.vo.search.AlbumSearchResponseVo;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SearchService {


    List<Map<String, Object>> channel(Long c1Id);

    AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery);

    Set<String> completeSuggest(String input);
}
