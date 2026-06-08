package com.example.restaurant.service;

import com.example.restaurant.common.PageResponse;
import com.example.restaurant.common.PageUtils;
import com.example.restaurant.entity.Dish;
import com.example.restaurant.entity.StockChangeLog;
import com.example.restaurant.mapper.DishMapper;
import com.example.restaurant.mapper.StockChangeLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class DishService {
    private final DishMapper dishMapper;
    private final StockChangeLogMapper stockChangeLogMapper;

    public DishService(DishMapper dishMapper, StockChangeLogMapper stockChangeLogMapper) {
        this.dishMapper = dishMapper;
        this.stockChangeLogMapper = stockChangeLogMapper;
    }

    public List<Dish> list(String keyword, Integer categoryId) {
        return dishMapper.findAll(keyword, categoryId);
    }

    public PageResponse<Dish> page(String keyword, Integer categoryId, Integer page, Integer size) {
        PageUtils.PageParams params = PageUtils.normalize(page, size);
        long total = dishMapper.countAll(keyword, categoryId);
        List<Dish> records = total == 0 ? List.of() : dishMapper.findPage(keyword, categoryId, params.getOffset(), params.getSize());
        return PageUtils.response(records, total, params);
    }

    public Dish create(Dish dish) {
        if (dish.getAvailable() == null) {
            dish.setAvailable(true);
        }
        dishMapper.insert(dish);
        return dish;
    }

    @Transactional
    public void update(Dish dish) {
        Dish before = dish.getStock() == null ? null : dishMapper.findById(dish.getDishId());
        dishMapper.update(dish);
        if (before != null && !Objects.equals(before.getStock(), dish.getStock())) {
            int delta = dish.getStock() - before.getStock();
            recordStockChange(dish.getDishId(), Math.abs(delta), delta > 0 ? "IN" : "OUT", "MANUAL_ADJUST");
        }
    }

    public void delete(Long id) {
        dishMapper.delete(id);
    }

    public List<Dish> lowStock(Integer threshold) {
        return dishMapper.lowStock(threshold == null ? 10 : threshold);
    }

    private void recordStockChange(Long dishId, Integer quantity, String changeType, String reason) {
        if (quantity == null || quantity <= 0) {
            return;
        }
        StockChangeLog log = new StockChangeLog();
        log.setDishId(dishId);
        log.setQuantity(quantity);
        log.setChangeType(changeType);
        log.setReason(reason);
        stockChangeLogMapper.insert(log);
    }
}
