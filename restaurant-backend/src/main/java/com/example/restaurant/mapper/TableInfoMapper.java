package com.example.restaurant.mapper;

import com.example.restaurant.entity.TableInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TableInfoMapper {
    List<TableInfo> findAll(@Param("status") String status);

    List<TableInfo> findPage(@Param("status") String status, @Param("offset") int offset, @Param("size") int size);

    long countAll(@Param("status") String status);

    TableInfo findById(@Param("tableId") Integer tableId);

    TableInfo lockById(@Param("tableId") Integer tableId);

    int insert(TableInfo tableInfo);

    int update(TableInfo tableInfo);

    int updateStatus(@Param("tableId") Integer tableId, @Param("status") String status);

    int delete(@Param("tableId") Integer tableId);
}
