package com.lsj.tingshu.account.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsj.tingshu.account.mapper.UserAccountDetailMapper;
import com.lsj.tingshu.account.service.UserAccountDetailService;
import com.lsj.tingshu.model.account.UserAccountDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserAccountDetailServiceImpl extends ServiceImpl<UserAccountDetailMapper, UserAccountDetail> implements UserAccountDetailService {

}
