package com.lsj.tingshu.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lsj.tingshu.model.account.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {



    int checkAndLockAmount(@Param("userId") Long userId, @Param("amount") BigDecimal amount);


    void minus(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    void unLock(@Param("userId") Long userId, @Param("amount") BigDecimal amount);



    void updateUserAmount(@Param("userId") String userId, @Param("amount") BigDecimal amount);



}
