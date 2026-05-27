package com.cloudhub.platform.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.message.domain.entity.MessageChannel;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageChannelMapper extends BaseMapper<MessageChannel> {
}
