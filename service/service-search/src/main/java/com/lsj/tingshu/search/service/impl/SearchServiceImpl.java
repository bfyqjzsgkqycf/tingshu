package com.lsj.tingshu.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.TopHitsAggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.Suggester;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import co.elastic.clients.json.JsonData;
import com.alibaba.fastjson.JSONObject;
import com.lsj.tingshu.album.client.AlbumInfoFeignClient;
import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.model.album.BaseCategory3;
import com.lsj.tingshu.model.search.AlbumInfoIndex;
import com.lsj.tingshu.model.search.SuggestIndex;
import com.lsj.tingshu.query.search.AlbumIndexQuery;
import com.lsj.tingshu.search.service.SearchService;
import com.lsj.tingshu.vo.search.AlbumInfoIndexVo;
import com.lsj.tingshu.vo.search.AlbumSearchResponseVo;
import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class SearchServiceImpl implements SearchService {

    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Override
    @SneakyThrows
    public List<Map<String, Object>> channel(Long c1Id) {
        // 1.根据一级分类id查询热度最高的三级分类集合
        Result<List<BaseCategory3>> category3List = albumInfoFeignClient.findTopBaseCategory3List(c1Id);
        List<BaseCategory3> category3Data = category3List.getData();

        // 2.提取三级分类id, 并将其转为Elastic使用的FieldValue类型
        List<FieldValue> category3IdFieldValueList = category3Data.stream().map(category3 -> {
            String category3Id = category3.getId().toString();
            return FieldValue.of(category3Id);
        }).collect(Collectors.toList());

        // 3.将category3List转成map
        Map<Long, BaseCategory3> category3IdAndMap = category3Data.stream().collect(Collectors.toMap(BaseCategory3::getId, category3 -> category3));

        // 4.构建DSL语句
        SearchRequest searchRequest = buildChannelDsl(category3IdFieldValueList);

        // 5.执行DSL语句
        SearchResponse<AlbumInfoIndex> searchResponse = elasticsearchClient.search(searchRequest, AlbumInfoIndex.class);

        // 6.解析结果
        List<Map<String, Object>> channelResult = parseResult(searchResponse, category3IdAndMap);

        return channelResult;
    }

    @Override
    @SneakyThrows
    public AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery) {
        // 1.构建DSL语句
        SearchRequest searchRequest = buildSearchDsl(albumIndexQuery);
        // 2.执行DSL语句
        SearchResponse<AlbumInfoIndex> searchResponse = elasticsearchClient.search(searchRequest, AlbumInfoIndex.class);
        // 3.解析响应对象
        AlbumSearchResponseVo albumSearchResponseVo = parseSearchResponse(searchResponse);
        albumSearchResponseVo.setPageNo(albumIndexQuery.getPageNo());
        albumSearchResponseVo.setPageSize(albumIndexQuery.getPageSize());
        albumSearchResponseVo.setTotalPages((albumSearchResponseVo.getTotal() + albumIndexQuery.getPageSize() - 1) / albumIndexQuery.getPageSize());
        return albumSearchResponseVo;
    }

    @Override
    @SneakyThrows
    public Set<String> completeSuggest(String input) {

        // 构建 查询 解析

        Suggester suggester = buildSuggestDsl(input);
        SearchResponse<SuggestIndex> suggestInfo = elasticsearchClient.search(sqb -> sqb
                .index("suggestinfo").suggest(suggester), SuggestIndex.class);
        Set<String> parseResult = parseSuggestInfo(suggestInfo);

        // 自动补全十个 如果不足十个 从匹配查询补
        if (parseResult.size() < 10) {
            SearchResponse<SuggestIndex> searchResponse = elasticsearchClient.search(srb -> srb
                            .index("suggestinfo")
                            .query(qb -> qb
                                    .match(mb -> mb
                                            .field("title")
                                            .query(input)
                                    )
                            )
                    , SuggestIndex.class);
            for (Hit<SuggestIndex> hit : searchResponse.hits().hits()) {
                SuggestIndex suggestIndex = hit.source();
                if (parseResult.size() > 10) break;
                parseResult.add(suggestIndex.getTitle());
            }
        }

        return parseResult;
    }

    private Set<String> parseSuggestInfo(SearchResponse<SuggestIndex> suggestInfo) {
        HashSet<String> set = new HashSet<>();

        Map<String, List<Suggestion<SuggestIndex>>> suggestMap = suggestInfo.suggest();
        for (Map.Entry<String, List<Suggestion<SuggestIndex>>> suggestEntry : suggestMap.entrySet()) {
            List<Suggestion<SuggestIndex>> suggestValueList = suggestEntry.getValue();
            for (Suggestion<SuggestIndex> suggestValue : suggestValueList) {
                for (CompletionSuggestOption<SuggestIndex> suggestOption : suggestValue.completion().options()) {
                    String suggestTitle = suggestOption.source().getTitle();
                    set.add(suggestTitle);
                }
            }
        }

        return set;
    }

    private Suggester buildSuggestDsl(String input) {
        Suggester.Builder suggesterBuilder = new Suggester.Builder();
        suggesterBuilder
                .suggesters("suggestionKeyword", fsb -> fsb
                        .prefix(input)
                        .completion(csb -> csb
                                .field("keyword")
                                .skipDuplicates(true)
                        )
                )
                .suggesters("suggestionPinyin", fsb -> fsb
                        .prefix(input)
                        .completion(csb -> csb
                                .field("keywordPinyin")
                                .skipDuplicates(true)
                        )
                )
                .suggesters("suggestionSequence", fsb -> fsb
                        .prefix(input)
                        .completion(csb -> csb
                                .field("keywordSequence")
                                .skipDuplicates(true)
                        )
                );

        Suggester suggesterBuild = suggesterBuilder.build();
        System.out.println("suggest DSL: " + suggesterBuild.toString());
        return suggesterBuild;
    }

    private AlbumSearchResponseVo parseSearchResponse(SearchResponse<AlbumInfoIndex> searchResponse) {
        AlbumSearchResponseVo albumSearchResponseVo = new AlbumSearchResponseVo();
        albumSearchResponseVo.setTotal(searchResponse.hits().total().value());

        List<AlbumInfoIndexVo> AlbumInfoIndexVoList = searchResponse.hits().hits().stream().map(hit -> {
            AlbumInfoIndexVo albumInfoIndexVo = new AlbumInfoIndexVo();
            AlbumInfoIndex albumInfoIndex = hit.source();
            BeanUtils.copyProperties(albumInfoIndex, albumInfoIndexVo);

            List<String> albumTitle = hit.highlight().get("albumTitle");
            if (!StringUtils.isEmpty(albumTitle)) {
                albumInfoIndexVo.setAlbumTitle(albumTitle.get(0));
            }

            return albumInfoIndexVo;
        }).collect(Collectors.toList());
        albumSearchResponseVo.setList(AlbumInfoIndexVoList);

        return albumSearchResponseVo;
    }

    private SearchRequest buildSearchDsl(AlbumIndexQuery albumIndexQuery) {
        // 注意各种条件的携带状态
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        // 1.构建关键字条件
        if (!StringUtils.isEmpty(albumIndexQuery.getKeyword())) {
            boolQueryBuilder
                    .should(qb -> qb
                            .match(mqb -> mqb
                                    .field("albumTitle")
                                    .query(albumIndexQuery.getKeyword()
                                    )
                            )
                    )
                    .should(qb -> qb
                            .match(mqb -> mqb
                                    .field("albumIntro")
                                    .query(albumIndexQuery.getKeyword()
                                    )
                            )
                    );
        }
        // 2.构建分类条件
        if (!StringUtils.isEmpty(albumIndexQuery.getCategory1Id())) {
            boolQueryBuilder
                    .must(qb -> qb
                            .term(tqb -> tqb
                                    .field("category1Id")
                                    .value(albumIndexQuery.getCategory1Id())
                            )
                    );
        }
        if (!StringUtils.isEmpty(albumIndexQuery.getCategory2Id())) {
            boolQueryBuilder
                    .must(qb -> qb
                            .term(tqb -> tqb
                                    .field("category2Id")
                                    .value(albumIndexQuery.getCategory2Id())
                            )
                    );
        }
        if (!StringUtils.isEmpty(albumIndexQuery.getCategory3Id())) {
            boolQueryBuilder
                    .must(qb -> qb
                            .term(tqb -> tqb
                                    .field("category3Id")
                                    .value(albumIndexQuery.getCategory3Id())
                            )
                    );
        }
        // 3.构建标签条件
        if (albumIndexQuery.getAttributeList() != null && albumIndexQuery.getAttributeList().size() > 0) {
            List<String> attributeList = albumIndexQuery.getAttributeList();
            if (attributeList != null && attributeList.size() > 0) {
                for (String attribute : attributeList) {
                    String[] split = attribute.split(":");
                    String attributeId = split[0];
                    String attributeValueId = split[1];
                    boolQueryBuilder
                            .must(qb -> qb
                                    .nested(nqb -> nqb
                                            .path("attributeValueIndexList")
                                            .query(qbx -> qbx
                                                    .bool(bqb -> bqb
                                                            .must(mqb -> mqb
                                                                    .term(tqb -> tqb
                                                                            .field("attributeValueIndexList.attributeId")
                                                                            .value(attributeId)
                                                                    )
                                                            )
                                                            .must(mqb -> mqb
                                                                    .term(tqb -> tqb
                                                                            .field("attributeValueIndexList.attributeValueId")
                                                                            .value(attributeValueId)
                                                                    )
                                                            )
                                                    )
                                            )
                                    )
                            );
                }
            }
        }
        Query query = boolQueryBuilder.build()._toQuery();
        searchRequestBuilder.index("albuminfo").query(query); // 基础条件

        // 扩展条件
        // 4.构建分页条件
        Integer pageNo = albumIndexQuery.getPageNo();
        Integer pageSize = albumIndexQuery.getPageSize();
        searchRequestBuilder.from((pageNo - 1) * pageSize);
        searchRequestBuilder.size(pageSize);

        // 5.构建排序条件
        if (!StringUtils.isEmpty(albumIndexQuery.getOrder())) {
            String[] split = albumIndexQuery.getOrder().split(":");
            String order = split[0];
            SortOrder sort = SortOrder.valueOf(split[1]);
            switch (order) {
                case "1":
                    searchRequestBuilder.sort(s -> s.field(f -> f.field("hotScore").order(sort)));
                    break;
                case "2":
                    searchRequestBuilder.sort(s -> s.field(f -> f.field("playStatNum").order(sort)));
                    break;
                case "3":
                    searchRequestBuilder.sort(s -> s.field(f -> f.field("createTime").order(sort)));
            }
        }

        // 6.构建高亮条件
        if (!StringUtils.isEmpty(albumIndexQuery.getKeyword())) {
            searchRequestBuilder
                    .highlight(hlb -> hlb
                            .fields("albumTitle", hlfb -> hlfb
                                    .preTags("<font style='color:red'>")
                                    .postTags("</font>")
                            )
                    );
        }

        SearchRequest searchRequest = searchRequestBuilder.build();

        return searchRequest;
    }


    @SneakyThrows
    private SearchRequest buildChannelDsl(List<FieldValue> category3IdFieldValueList) {
        SearchRequest.Builder builder = new SearchRequest.Builder();
        SearchRequest searchRequestBuild = builder
                .index("albuminfo")
                .query(query -> query
                        .terms(tqb -> tqb
                                .field("category3Id")
                                .terms(tqfb -> tqfb
                                        .value(category3IdFieldValueList)
                                )
                        )
                )
                .aggregations("category3IdAgg", ab -> ab
                        .terms(tab -> tab
                                .field("category3Id")
                                .size(7)
                        )
                        .aggregations("subCategory3IdAgg", sab -> sab
                                .topHits(thab -> thab
                                        .sort(sob -> sob
                                                .field(fsb -> fsb
                                                        .field("hotScore")
                                                        .order(SortOrder.Desc)
                                                )
                                        )
                                        .size(6)
                                )
                        )
                ).build();

        System.out.println("channel的DSL: " + searchRequestBuild.toString());
        return searchRequestBuild;
    }

    private List<Map<String, Object>> parseResult(SearchResponse<AlbumInfoIndex> searchResponse, Map<Long, BaseCategory3> category3IdAndMap) {
        List<Map<String, Object>> channelList = Lists.newArrayList();
        // 1.获取第一层三级分类子聚合
        Aggregate category3IdAgg = searchResponse.aggregations().get("category3IdAgg");
        // 2.获取第一层桶集合
        List<LongTermsBucket> buckets = category3IdAgg.lterms().buckets().array();
        for (LongTermsBucket bucket : buckets) {
            HashMap<String, Object> map = new HashMap<>(); // 三级分类对象及所属六个热度最高的专辑
            ArrayList<AlbumInfoIndex> topHotScoreAlbumList = Lists.newArrayList(); // 专辑对象集合
            TopHitsAggregate topHitsAggregate = bucket.aggregations().get("subCategory3IdAgg").topHits();
            for (Hit<JsonData> hit : topHitsAggregate.hits().hits()) {
                JsonData source = hit.source();// 文档对象
                AlbumInfoIndex albumInfoIndex = JSONObject.parseObject(source.toString(), AlbumInfoIndex.class);
                topHotScoreAlbumList.add(albumInfoIndex);
            }
            map.put("baseCategory3", category3IdAndMap.get(bucket.key())); // 三级分类集合
            map.put("list", topHotScoreAlbumList); // 专辑对象集合
            channelList.add(map);
        }
        return channelList;
    }


}
