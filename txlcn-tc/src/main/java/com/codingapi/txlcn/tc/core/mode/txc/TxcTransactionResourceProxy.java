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
package com.codingapi.txlcn.tc.core.mode.txc;

import com.codingapi.txlcn.tc.core.TransactionResourceProxy;
import com.codingapi.txlcn.tc.core.context.BranchContext;
import com.codingapi.txlcn.tc.core.context.BranchSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Description:
 * Date: 2018/12/13
 *
 * @author ujued
 */
@Component("transaction_txc")
public class TxcTransactionResourceProxy implements TransactionResourceProxy {

    private final ConnectionHelper connectionHelper;

    private final BranchContext branchContext;

    @Autowired
    public TxcTransactionResourceProxy(ConnectionHelper connectionHelper, BranchContext branchContext) {
        this.connectionHelper = connectionHelper;
        this.branchContext = branchContext;
    }


    @Override
    public Connection proxyConnection(DataSource dataSource) throws Throwable {
        Connection connection = dataSource.getConnection();
        branchContext.linkDataSource(BranchSession.cur().getGroupId(), connection, dataSource);
        return connectionHelper.proxy(connection);
    }
}
