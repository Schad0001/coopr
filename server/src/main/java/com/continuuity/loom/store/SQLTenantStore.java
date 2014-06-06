/*
 * Copyright 2012-2014, Continuuity, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.continuuity.loom.store;

import com.continuuity.loom.admin.Tenant;
import com.continuuity.loom.codec.json.JsonSerde;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Implementation of {@link com.continuuity.loom.store.TenantStore} using a SQL database as the persistent store.
 */
public class SQLTenantStore extends AbstractIdleService implements TenantStore {
  private static final Logger LOG  = LoggerFactory.getLogger(SQLTenantStore.class);
  private static final JsonSerde codec = new JsonSerde();

  private final DBConnectionPool dbConnectionPool;

  // for unit tests only.  Truncate is not supported in derby.
  public void clearData() throws SQLException {
    Connection conn = dbConnectionPool.getConnection();
    try {
      Statement stmt = conn.createStatement();
      try {
        stmt.execute("DELETE FROM tenants");
      } finally {
        stmt.close();
      }
    } finally {
      conn.close();
    }
  }

  @Inject
  SQLTenantStore(DBConnectionPool dbConnectionPool) throws SQLException, ClassNotFoundException {
    this.dbConnectionPool = dbConnectionPool;
  }

  public void initDerbyDB() throws SQLException {
    LOG.warn("Initializing Derby DB... Tables are not optimized for performance.");
  }

  private void createDerbyTable(String createString) throws SQLException {
    Connection conn = dbConnectionPool.getConnection();
    try {
      Statement statement = conn.createStatement();
      try {
        statement.executeUpdate(createString);
      } catch (SQLException e) {
        // code for the table already exists in derby.
        if (!e.getSQLState().equals("X0Y32")) {
          throw Throwables.propagate(e);
        }
      } finally {
        statement.close();
      }
    } finally {
      conn.close();
    }
  }

  @Override
  protected void startUp() throws Exception {
    if (dbConnectionPool.isEmbeddedDerbyDB()) {
      DBQueryHelper.createDerbyTable("CREATE TABLE tenants ( id BIGINT, name VARCHAR(255), workers INT, tenant BLOB )",
                                     dbConnectionPool);
    }
  }

  @Override
  protected void shutDown() throws Exception {
    // No-op
  }

  @Override
  public Tenant getTenant(long id) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement("SELECT tenant FROM tenants WHERE id=?");
        statement.setLong(1, id);
        try {
          return DBQueryHelper.getQueryItem(statement, Tenant.class);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  @Override
  public List<Tenant> getAllTenants() throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement("SELECT tenant FROM tenants");
        try {
          return DBQueryHelper.getQueryList(statement, Tenant.class);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void writeTenant(Tenant tenant) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      long tenantId = tenant.getId();
      try {
        PreparedStatement checkStatement = conn.prepareStatement("SELECT id FROM tenants WHERE id=?");
        checkStatement.setLong(1, tenantId);
        PreparedStatement writeStatement;
        try {
          ResultSet rs = checkStatement.executeQuery();
          try {
            if (rs.next()) {
              // cluster exists already, perform an update.
              writeStatement = conn.prepareStatement(
                "UPDATE tenants SET tenant=?, workers=? WHERE id=?");
              writeStatement.setBlob(1, new ByteArrayInputStream(codec.serialize(tenant, Tenant.class)));
              writeStatement.setInt(2, tenant.getWorkers());
              writeStatement.setLong(3, tenantId);
            } else {
              // cluster does not exist, perform an insert.
              writeStatement = conn.prepareStatement(
                "INSERT INTO tenants (id, workers, tenant) VALUES (?, ?, ?)");
              writeStatement.setLong(1, tenantId);
              writeStatement.setInt(2, tenant.getWorkers());
              writeStatement.setBlob(3, new ByteArrayInputStream(codec.serialize(tenant, Tenant.class)));
            }
          } finally {
            rs.close();
          }
          // perform the update or insert
          try {
            writeStatement.executeUpdate();
          } finally {
            writeStatement.close();
          }
        } finally {
          checkStatement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void deleteTenant(long id) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement("DELETE FROM tenants WHERE id=? ");
        statement.setLong(1, id);
        try {
          statement.executeUpdate();
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }
}
