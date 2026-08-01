package com.ljc.chaocommunity.config;


import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;


@Component
public class MyMetaObjectHandler implements MetaObjectHandler {


    /**
     * 插入时自动填充
     */
    @Override
    public void insertFill(MetaObject metaObject) {

        this.strictInsertFill(
                metaObject,
                "createTime",
                LocalDateTime.class,
                LocalDateTime.now()
        );


        this.strictInsertFill(
                metaObject,
                "updateTime",
                LocalDateTime.class,
                LocalDateTime.now()
        );
    }


    /**
     * 更新时自动填充
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(
                metaObject,
                "updateTime",
                LocalDateTime.class,
                LocalDateTime.now()
        );

    }
}


//TODO  前后端联调 让用户账户封禁 或者 限制发言 qq邮箱给管理员推送审核邮件


//TODO 以后可以创建专栏相关实体接口 帖子可以放在专栏下

//TODO 以后可以查看关注的人的动态



