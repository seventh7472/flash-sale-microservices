package com.flashsale.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashsale.common.constant.ResultCode;
import com.flashsale.common.exception.BusinessException;
import com.flashsale.product.entity.Product;
import com.flashsale.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductMapper productMapper;

    public Page<Product> page(int page, int size) {
        return productMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1)
                        .orderByDesc(Product::getCreateTime));
    }

    public Product getById(Long id) {
        return productMapper.selectById(id);
    }

    public void create(Product product) {
        if (product == null || product.getName() == null || product.getName().trim().isEmpty())
            throw new BusinessException(ResultCode.VALIDATE_FAILED, "商品名称不能为空");
        if (product.getOriginalPrice() == null || product.getOriginalPrice().compareTo(BigDecimal.ZERO) <= 0)
            throw new BusinessException(ResultCode.VALIDATE_FAILED, "商品价格必须大于0");
        product.setCreateTime(LocalDateTime.now());
        productMapper.insert(product);
    }

    public void update(Product product) {
        if (product == null || product.getId() == null)
            throw new BusinessException(ResultCode.VALIDATE_FAILED, "商品ID不能为空");
        if (product.getName() != null && product.getName().trim().isEmpty())
            throw new BusinessException(ResultCode.VALIDATE_FAILED, "商品名称不能为空");
        if (product.getOriginalPrice() != null && product.getOriginalPrice().compareTo(BigDecimal.ZERO) <= 0)
            throw new BusinessException(ResultCode.VALIDATE_FAILED, "商品价格必须大于0");
        productMapper.updateById(product);
    }

    public void delete(Long id) {
        if (id == null)
            throw new BusinessException(ResultCode.VALIDATE_FAILED, "商品ID不能为空");
        productMapper.deleteById(id);
    }
}