package org.exh.nianhuawechatminiprogrambackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.dto.response.GoodsItem;
import org.exh.nianhuawechatminiprogrambackend.dto.response.GoodsQueryResponse;
import org.exh.nianhuawechatminiprogrambackend.entity.Goods;
import org.exh.nianhuawechatminiprogrambackend.enums.ResultCode;
import org.exh.nianhuawechatminiprogrambackend.exception.BusinessException;
import org.exh.nianhuawechatminiprogrambackend.mapper.GoodsMapper;
import org.exh.nianhuawechatminiprogrambackend.service.GoodsService;
import org.exh.nianhuawechatminiprogrambackend.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品服务实现类
 */
@Slf4j
@Service
public class GoodsServiceImpl implements GoodsService {

    @Autowired
    private GoodsMapper goodsMapper;

    @Override
    public GoodsQueryResponse queryGoods(String type, Integer pageNum, Integer pageSize) {
        log.info("商品查询开始，type={}, pageNum={}, pageSize={}", type, pageNum, pageSize);

        // 1. 参数处理
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        if (pageSize > 100) {
            pageSize = 100; // 限制最大分页大小
        }

        // 2. 构建查询条件
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<Goods>()
                .eq(Goods::getStatus, 1) // 只查询上架的商品
                .eq(Goods::getIsDeleted, 0) // 查询未删除的商品
                .orderByDesc(Goods::getCreatedAt); // 按创建时间倒序

        // 3. 根据type参数过滤
        if (type != null && !type.isEmpty() && !"all".equals(type)) {
            String category = convertTypeToCategory(type);
            if (category != null) {
                wrapper.eq(Goods::getCategory, category);
            }
        }

        // 4. 执行分页查询
        IPage<Goods> page = new Page<>(pageNum, pageSize);
        IPage<Goods> resultPage = goodsMapper.selectPage(page, wrapper);

        // 5. 转换结果
        List<GoodsItem> items = new ArrayList<>();
        for (Goods goods : resultPage.getRecords()) {
            GoodsItem item = convertToGoodsItem(goods);
            items.add(item);
        }

        // 6. 封装响应
        GoodsQueryResponse response = new GoodsQueryResponse();
        response.setItems(items);

        log.info("商品查询成功，共查询到{}条记录", items.size());
        return response;
    }

    /**
     * 将请求参数type转换为数据库category
     * 注意: 统一使用photos表示写真/摄影服务
     */
    private String convertTypeToCategory(String type) {
        // 接口参数type和数据库category保持一致
        // photos-写真/摄影, makeup-化妆, seat_package-座位商品包, sets-套餐
        return type; // 保持一致的命名
    }

    /**
     * 将数据库category转换为返回的type
     * 注意: 统一使用photos表示写真/摄影服务
     */
    private String convertCategoryToType(String category) {
        return category != null ? category : "";
    }

    /**
     * 将Goods实体转换为GoodsItem DTO
     * 价格从分转换为元（除以100）
     */
    private GoodsItem convertToGoodsItem(Goods goods) {
        GoodsItem item = new GoodsItem();
        item.setGoodId(goods.getId());
        item.setTitle(goods.getName());
        item.setDescription(goods.getDescription());
        item.setImageUrl(goods.getImageUrl());
        item.setType(convertCategoryToType(goods.getCategory()));
        item.setTag(goods.getTag());

        // 价格从分转换为元，保留两位小数
        if (goods.getPrice() != null) {
            double priceInYuan = goods.getPrice() / 100.0;
            item.setPrice(String.format("%.2f", priceInYuan));
        } else {
            item.setPrice("0.00");
        }

        return item;
    }
}
