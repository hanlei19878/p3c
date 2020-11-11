package com.dcits.ensemble.dao;

import java.util.Date;
import java.util.List;

import com.dcits.ensemble.util.DateFormatUtil;
import com.dcits.galaxy.dal.mybatis.shard.Shard;
import com.dcits.orion.core.dao.BaseDao;
import net.sourceforge.pmd.lang.java.ast.ASTExtendsList;

/**
 * Created by Chengliang on 3/6 2017.
 */
public abstract class EnsBaseDao extends BaseDao {
    EnsBaseDao(){

    }

    private <T extends EnsBaseDbBean> void setDefaultValue(T record) {
        Date currentDate = new Date();
        record.setTranTimestamp(DateFormatUtil.formatDate(currentDate));
        record.setTranTime(DateFormatUtil.transForMilliSecond(currentDate));
    }
    //111111111
    public <T extends EnsBaseDbBean> int insert(T record) {
        setDefaultValue(record);
        return super.insert(record);
    }

    <T extends EnsBaseDbBean> int insert(T record, Shard shard) {
        setDefaultValue(record);
        return super.insert(record, shard);
    }

    protected int insertAddBatch(String sqlId, List<?> paramters) {
        return super.insertAddBatch(sqlId, (List) paramters);
    }

    public int insertAddBatch(String sqlId, List<?> paramters, Shard shard) {
        return super.insertAddBatch(sqlId, paramters, shard);
    }

    public <T extends EnsBaseDbBean> int updateByPrimaryKey(T record) {
        setDefaultValue(record);
        return super.updateByPrimaryKey(record);
    }

    public <T extends EnsBaseDbBean> int updateByPrimaryKey(T record, Shard shard) {
        setDefaultValue(record);
        return super.updateByPrimaryKey(record, shard);
    }

    public <T extends EnsBaseDbBean> int update(String sqlId, T record) {
        setDefaultValue(record);
        return super.update(sqlId, record);
    }

    public <T extends EnsBaseDbBean> int update(String sqlId, T record, Shard shard) {
        setDefaultValue(record);
        return super.update(sqlId, record, shard);
    }

    //modify by 汤成国 增加一组不设置默认交易时间戳的更新方法 20180329 start
    public <T extends EnsBaseDbBean> int updateByPrimaryKeyNDV(T record) {
        return super.updateByPrimaryKey(record);
    }

    public <T extends EnsBaseDbBean> int updateByPrimaryKeyNDV(T record, Shard shard) {
        return super.updateByPrimaryKey(record, shard);
    }

    public <T extends EnsBaseDbBean> int updateNDV(String sqlId, T record) {
        return super.update(sqlId, record);
    }

    public <T extends EnsBaseDbBean> int updateNDV(String sqlId, T record, Shard shard) {
        return super.update(sqlId, record, shard);
    }
    //modify by 汤成国 增加一组不设置默认交易时间戳的更新方法 20180329 end

    public <T> List<T> selectListNoShard(String sqlId, Object param)
    {
        return super.selectList(sqlId, param, null);
    }
}
