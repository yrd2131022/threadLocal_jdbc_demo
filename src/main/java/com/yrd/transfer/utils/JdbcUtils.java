package com.yrd.transfer.utils;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * 
 * @ClassName:JdbcUtils
 * @Description:案例中我们可以看到， 在一些特定场景下，ThreadLocal方案有两个突出的优势： 1. 传递数据 ：
 *                         保存每个线程绑定的数据，在需要的地方可以直接获取, 避免参数直接传递带来的代码耦合问题 2. 线程隔离 ：
 *                         各线程之间的数据相互隔离却又具备并发性，避免同步方式带来的性能损失
 *
 * @author:Yrd
 * @date:2021-5-28 15:06:39
 *
 */

public class JdbcUtils {
	// ThreadLocal对象 : 将connection绑定在当前线程中
	private static final ThreadLocal<Connection> tl = new ThreadLocal<Connection>();

	// ================================c3p0=============================================
	// c3p0 数据库连接池对象属性
	private static final ComboPooledDataSource ds = new ComboPooledDataSource();

	// 获取连接
	/*
	 * 原本：直接从连接池中获取连接 现在：1.直接获取当前线程绑定的连接对象 2.如果连接对象是空的 2.1再去连接池中获取连接
	 * 2.2将此连接对象跟当前线程进行绑定
	 */
	public static Connection getConnection() throws SQLException {
		// 直接获取当前线程绑定的连接对象
		Connection connection = tl.get();
		// 如果连接对象是空的
		if (connection == null) {
			// 再去连接池中获取连接
			connection = ds.getConnection();
			// 将此连接对象跟当前线程进行绑定
			tl.set(connection);
		}
		return connection;
//        return ds.getConnection();
	}

	// =====================================c3p0 end
	// ===================================

	// =====================================druid========================================
	// druid 数据库连接池对象属性
	private static DataSource dsDruid;
	static {

		try {
			Properties pros = new Properties();
			InputStream resourceAsStream = ClassLoader.getSystemClassLoader().getResourceAsStream("druid.properties");
			pros.load(resourceAsStream);
			dsDruid = DruidDataSourceFactory.createDataSource(pros);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static Connection getConnectionDruid() throws SQLException {
		// 直接获取当前线程绑定的连接对象
		Connection connection = tl.get();
		// 如果连接对象是空的
		if (connection == null) {
			// 再去连接池中获取连接
			connection = dsDruid.getConnection();
			// 将此连接对象跟当前线程进行绑定
			tl.set(connection);
		}
		return connection;
//        return ds.getConnection();
	}

	// ============================druid
	// end================================================

	// 释放资源
	public static void release(AutoCloseable... ios) {
		for (AutoCloseable io : ios) {
			if (io != null) {
				try {
					io.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void commitAndClose(Connection conn) {
		try {
			if (conn != null) {
				// 提交事务
				conn.commit();
				// 解绑当前线程绑定的连接对象
				tl.remove();
				// 释放连接
				conn.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void rollbackAndClose(Connection conn) {
		try {
			if (conn != null) {
				// 回滚事务
				conn.rollback();
				// 解绑当前线程绑定的连接对象
				tl.remove();
				// 释放连接
				conn.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void commitAndCloseAndAutoCommit(Connection conn) {
		try {
			if (conn != null) {
				// 提交事务
				conn.commit();
				// 解绑当前线程绑定的连接对象
				tl.remove();
				// 关闭事务，开启自动提交数据，特别是在使用连接池的时候
				conn.setAutoCommit(true);
				
				// 释放连接
				conn.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void rollbackAndCloseAndAutoCommit(Connection conn) {
		try {
			if (conn != null) {
				// 回滚事务
				conn.rollback();
				// 解绑当前线程绑定的连接对象
				tl.remove();
				// 关闭事务，开启自动提交数据，特别是在使用连接池的时候
				conn.setAutoCommit(true);
				// 释放连接
				conn.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
