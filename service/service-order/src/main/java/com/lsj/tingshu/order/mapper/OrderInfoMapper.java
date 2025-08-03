package com.lsj.tingshu.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lsj.tingshu.model.order.OrderInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {


    @Select("SELECT \n" +
            "count(*)\n" +
            " FROM `order_info` \n" +
            " INNER JOIN order_detail\n" +
            " ON order_info.id=order_detail.order_id\n" +
            " WHERE  order_info.user_id=#{userId}   AND  order_info.item_type=#{itemType} AND order_detail.item_id=#{albumId}  AND   order_info.order_status='0901'")
    long getItemTypeAlbumOrderInfo(@Param("userId") Long userId, @Param("itemType") String itemType, @Param("albumId") Long albumId);


    @Select("SELECT   \n" +
            "\n" +
            "order_detail.item_id\n" +
            "\n" +
            "FROM  order_info\n" +
            "\n" +
            "INNER JOIN  order_detail\n" +
            "\n" +
            "ON  order_info.id=order_detail.order_id\n" +
            "\n" +
            "WHERE  order_info.user_id=#{userId}  AND order_info.order_status='0901' AND  order_info.item_type=#{itemType} ")
    List<Long> getItemTypeTrackOrderInfo(@Param("userId") Long userId, @Param("itemType") String itemType);


    @Select("SELECT \n" +
            "count(*)\n" +
            " FROM `order_info` \n" +
            " INNER JOIN order_detail\n" +
            " ON order_info.id=order_detail.order_id\n" +
            " WHERE  order_info.user_id=#{userId}   AND  order_info.item_type=#{itemType} AND order_detail.item_id=#{vipServiceId}  AND   order_info.order_status='0901'")
    long getItemTypeVipOrderInfo(@Param("userId") Long userId, @Param("itemType") String itemType, @Param("vipServiceId") Long vipServiceId);

    @Update("update   order_info  set   order_status=#{orderStatus}  WHERE  order_no=#{orderNo} and  user_id=#{userId}  and  order_status in('0901','0903')")
    void updateOrderStatus(@Param("orderNo") String orderNo, @Param("userId") Long userId, @Param("orderStatus") String orderStatus);



    @Update("update   order_info  set   order_status=#{exceptOrderStatus}  WHERE  order_no=#{orderNo} and  user_id=#{userId}  and  order_status ='0901'")
    void closeOrder(@Param("orderNo") String orderNo, @Param("userId") long userId, @Param("exceptOrderStatus") String exceptOrderStatus);

}
