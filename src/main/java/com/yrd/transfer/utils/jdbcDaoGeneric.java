package com.yrd.transfer.utils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public class jdbcDaoGeneric<T> {
	
	 private Class<T> c =null;
	 
	 {
		 //获取当前jdbcDaoGeneric的子类继承的父类中的泛型
		 Type genericSuperclass = this.getClass().getGenericSuperclass();
		 ParameterizedType paramType = (ParameterizedType) genericSuperclass;
		 //获取父类的参数
		 Type[] actualTypeArguments = paramType.getActualTypeArguments();
		 //泛型的第一个参数
		 c = (Class<T>) actualTypeArguments[0];
		 
	 }
	
	/**
	 * 通用SQL更新操作，insert update delete
	 * @param sql 更新SQL语句，如果语句中有参数使用?
	 * @param params SQL语句中“?”对应的值
	 * @return 影响的行数:-1表示操作出错
	 */
	public int commonUpdate(String sql, List<Object> params){
		Connection conn = null;
		PreparedStatement pst = null;
		try {
			conn = JdbcUtils.getConnection();
			if(conn != null){
				pst = conn.prepareStatement(sql);
				if(params != null){
					for (int i = 0; i < params.size(); i++) {
						pst.setObject(i+1, params.get(i));
					}
				}
				return pst.executeUpdate();
			}else{
//				log.error("数据库["+ DbInitReader.getInstance().getDbName()+"]连接失败");
				return -1;
			}
		} catch (Exception e) {
//			log.error("方法：【commonUpdate】执行sql出错，sql:【"+sql+"】,参数：【"+params+"】",e);
			e.printStackTrace();
			//事务回滚
//			this.rollback(conn);
			return -1;
		} finally {
			//释放资源
			JdbcUtils.release(conn, pst, null);
		}
	}
	/*
	 * 获取特殊值的方法
	 */
	public <E> E getValue(String sql, List<Object> params){
		Connection conn = null;
		PreparedStatement pst = null;
		ResultSet rs=null;
		try {
			conn = JdbcUtils.getConnection();
			if(conn != null){
				pst = conn.prepareStatement(sql);
				if(params != null){
					for (int i = 0; i < params.size(); i++) {
						pst.setObject(i+1, params.get(i));
					}
				}
				rs = pst.executeQuery();
				if(rs.next()) {
					return (E) rs.getObject(1);
				}else {
					return null;
				}
			}else{
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			//释放资源
			JdbcUtils.release(null, pst, rs);
		}
	}

	/*
	 * .返回单个对象(表中的一条记录)
	 * .使用PreparedStatement实现针对不同表的通用的查询操作（分装到类中如：Customer.java）
	 * 
	 * String sql = "select id,name,email from customers where id = ?";
	 * Customer customer = queryForOneClass(Customer.class,sql,2);
	 * System.out.println(customer);
	 */
	public  T queryForOneClass( String sql, Object... args) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			// 1.获取连接
			conn = JdbcUtils.getConnection();
			// 2.预编译sql语句，返回prepareStatement的实例
			ps = conn.prepareStatement(sql);
			// 3.填充占位符
			for (int i = 0; i < args.length; i++) {
				ps.setObject(i + 1, args[i]);
			}
			// 4.执行
			rs = ps.executeQuery();
			// 5.获取结果集的元数据：ResultSetMeteData
			ResultSetMetaData rsmd = rs.getMetaData();
			// 通过ResultSetMeteData获取结果集中的列数。
			int columnCount = rsmd.getColumnCount();

			if (rs.next()) {
				T t = c.newInstance();// 通过反射获取实例对象
				// 处理结果集一行数据中的每一列
				for (int i = 0; i < columnCount; i++) {
					// 获取列值
					Object columnValue = rs.getObject(i + 1);

					// 获取列名
					String columnLabel = rsmd.getColumnLabel(i + 1);

					// 给对象的属性赋值：通过反射
					Field field = c.getDeclaredField(columnLabel);
					field.setAccessible(true);// 强制操作（private属性）
					field.set(t, columnValue);

				}
				return t;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			JdbcUtils.release(rs,ps,conn);
		}

		return null;
	}
	
	/*
	 * .返回多个对象(表中的多条记录)
	 * .使用PreparedStatement实现针对不同表的通用的查询操作（分装到类中如：Customer.java）
	 * 
	 * String sql = "select id,name,email from customers where id < ?";
	 * List<Customer> customerList = queryForOneClass(Customer.class,sql,2);
	 * System.out.println(customerList);
	 */
	public  List<T> queryForListClass( String sql, Object... args) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			// 1.获取数据库的连接
			conn = JdbcUtils.getConnection();
			// 2.预编译sql语句，返回prepareStatement的实例
			ps = conn.prepareStatement(sql);
			// 3.填充占位符
			for (int i = 0; i < args.length; i++) {
				ps.setObject(i + 1, args[i]);
			}
			// 4.执行
			rs = ps.executeQuery();
			
			// 5.获取结果集的元数据：ResultSetMeteData
			ResultSetMetaData rsmd = rs.getMetaData();
			// 通过ResultSetMeteData获取结果集中的列数。
			int columnCount = rsmd.getColumnCount();

			//创建集合对象
			ArrayList<T> list = new ArrayList<T>();
			
			while(rs.next()) {
				T t = c.newInstance();// 通过反射获取实例对象
				// 处理结果集一行数据中的每一列,给t对象指定的属性赋值
				for (int i = 0; i < columnCount; i++) {
					// 获取列值
					Object columnValue = rs.getObject(i + 1);

					// 获取列名
					String columnLabel = rsmd.getColumnLabel(i + 1);

					// 给对象的属性赋值：通过反射
					Field field = c.getDeclaredField(columnLabel);
					field.setAccessible(true);// 强制操作（private属性）
					field.set(t, columnValue);
				}
				list.add(t);
			}
			return list;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			JdbcUtils.release(rs,ps,conn);
		}

		return null;		
		
	}
	
	/*
	 * 1.批量处理  addBatch()/executeBatch()/clearBatch()
	 * 2.mysql服务器默认是关闭批处理的，我们需要通过一个参数，让mysql开启批处理的支持。
	 * .?rewriteBatchedStatements=true 写在配置文件（jdbc.properties）的url后面
	 * 3.使用跟新的mysql驱动，至少要mysql-connector-java-5.1.37-bin.jar以上。
	 */
	public static void batchInsert() {
		Connection conn = null;
		PreparedStatement ps = null;
		String sql = "insert into t_customer(cname) values(?)";
		try {
			long startTime =System.currentTimeMillis();
			
			// 1.获取数据库的连接
			conn = JdbcUtils.getConnection();
			
			//开启事务，设置不允许自动提交数据(进一步优化，提交数据也是要消耗时间的)
			conn.setAutoCommit(false);
			// 2.预编译sql语句，返回prepareStatement的实例
			ps = conn.prepareStatement(sql);
			// 3.填充占位符
			for (int i = 1; i <= 10000; i++) {
				ps.setObject(1, "name_"+i);
				
				//1."攒"sql
				ps.addBatch();
				
				if(i%500==0) {
					//2.执行batch
					ps.executeBatch();  //相当于提交事务
					
					//3.清空batch
					ps.clearBatch();
				}
			}
			//提交数据
			conn.commit();
			
			long endTime = System.currentTimeMillis();
			System.out.println("花费的时间为："+(endTime - startTime));
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			JdbcUtils.release(ps,conn);
		}
	}
	
	public static void main(String[] args) {
		batchInsert();
	}

}
