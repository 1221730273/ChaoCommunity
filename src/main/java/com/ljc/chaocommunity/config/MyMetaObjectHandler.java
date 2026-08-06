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


//TODO 以后可以新增评论区@ 被@的收到通知  新增拉黑功能 被别人拉黑不能评论别人的帖子 不能看别人拥有什么帖子 和 @别人 不能查看对方关注列表和粉丝列表 (双向的)


//TODO 驳回审核以后给通知

//TODO 丰富管理员举报功能 如删除用户帖子 封禁用户（伴随着删除用户资料头像）

//TODO 用户可以置顶自己评论区的评论

//TODO 以后系统公告也支持md格式支持上传图片

//TODO 以后引入了ES做搜索功能

//TODO 以后可以用户设置隐私设置不允许看自己关注了谁自己的粉丝

//TODO 打开对方关注列表可以看共同关注

//TODO 以后可以弄个封禁期限

//TODO 以后注册需要邮箱收验证码

//TODO 以后引入websocket 和 消息队列 新增评论推送 帖子更新推送

//TODO 后续可以把举报功能弄的更正式 比如举报理由 举报的图片

//TODO 用户可以有等级像b站那样 等级,经验  查找用户的时候可以根据等级由高到低查找

//TODO 后续给user数据库表增加是否封禁字段 配合springsecurity 实现用户封禁

//TODO 以后支持邮箱修改绑定现在不支持 以后支持接入qq登录

//TODO 评论区以后可以加表情和图片


//TODO 以后用户资料界面展示帖子的地方也需要分页查询 玩意某个用户发了一万个帖子 一下子查询太慢 可以搞 最热最新分类 分页

//TODO 以后设计禁言用户不能评论 不能发帖 不能自己的修改资料







