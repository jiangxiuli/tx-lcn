/*
 * Copyright 2017-2019 CodingApi .
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingapi.txlcn.tc.core.mode.txc.analy;

import com.codingapi.txlcn.tc.core.context.BranchContext;
import com.codingapi.txlcn.tc.core.mode.txc.analy.def.TxcSqlExecutor;
import com.codingapi.txlcn.tc.core.mode.txc.analy.def.bean.*;
import com.codingapi.txlcn.tc.core.mode.txc.analy.util.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Description: TXC相关的数据表操作
 * <p>
 * Date: 2018/12/13
 *
 * @author ujued
 */
@Component
@Slf4j
public class TxcSqlExecutorImpl implements TxcSqlExecutor {

    private final BranchContext branchContext;

    private final QueryRunner queryRunner;

    @Autowired
    public TxcSqlExecutorImpl(BranchContext branchContext, QueryRunner queryRunner) {
        this.branchContext = branchContext;
        this.queryRunner = queryRunner;
    }

    @Override
    public List<ModifiedRecord> updateSqlPreviousData(Connection connection, UpdateImageParams updateImageParams)
            throws SQLException {
        Assert.notEmpty(updateImageParams.getPrimaryKeys(), "table must exists primary key(s).");
        // 前置镜像sql
        String beforeSql = SqlUtils.SELECT
                + String.join(SqlUtils.SQL_COMMA_SEPARATOR, updateImageParams.getColumns())
                + SqlUtils.SQL_COMMA_SEPARATOR
                + String.join(SqlUtils.SQL_COMMA_SEPARATOR, updateImageParams.getPrimaryKeys())
                + SqlUtils.FROM
                + String.join(SqlUtils.SQL_COMMA_SEPARATOR, updateImageParams.getTables())
                + SqlUtils.WHERE
                + updateImageParams.getWhereSql();
        return queryRunner.query(connection, beforeSql,
                new TxcModifiedRecordListHandler(updateImageParams.getPrimaryKeys(), updateImageParams.getColumns()));
    }

    @Override
    public List<ModifiedRecord> deleteSqlPreviousData(Connection connection, DeleteImageParams deleteImageParams)
            throws SQLException {
        String beforeSql = SqlUtils.SELECT + String.join(SqlUtils.SQL_COMMA_SEPARATOR, deleteImageParams.getColumns()) +
                SqlUtils.FROM +
                String.join(SqlUtils.SQL_COMMA_SEPARATOR, deleteImageParams.getTables()) +
                SqlUtils.WHERE +
                deleteImageParams.getSqlWhere();
        return queryRunner.query(connection, beforeSql,
                new TxcModifiedRecordListHandler(
                        deleteImageParams.getPrimaryKeys(),
                        deleteImageParams.getColumns()));
    }

    @Override
    public List<ModifiedRecord> selectSqlPreviousPrimaryKeys(Connection connection, SelectImageParams selectImageParams)
            throws SQLException {
        return queryRunner.query(connection, selectImageParams.getSql(),
                new TxcModifiedRecordListHandler(
                        selectImageParams.getPrimaryKeys(),
                        selectImageParams.getPrimaryKeys()));
    }

    @Override
    public void applyUndoLog(Map<DataSource, List<StatementInfo>> sMap) throws SQLException {
        for (Map.Entry<DataSource, List<StatementInfo>> entry : sMap.entrySet()) {
            Connection connection = null;
            try {
                connection = entry.getKey().getConnection();
                connection.setAutoCommit(false);
                for (StatementInfo statementInfo : entry.getValue()) {
                    log.debug("txc > Apply undo log. sql: {}, params: {}", statementInfo.getSql(), statementInfo.getParams());
                    queryRunner.update(connection, statementInfo.getSql(), statementInfo.getParams());
                }
                connection.commit();
            } catch (SQLException e) {
                if (connection != null) {
                    connection.rollback();
                }
                throw e;
            } finally {
                if (connection != null) {
                    connection.setAutoCommit(true);
                    DbUtils.close(connection);
                }
            }
        }

    }
}
