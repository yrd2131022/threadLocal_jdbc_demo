package com.yrd.transfer.service;

import java.sql.Connection;

import com.yrd.transfer.dao.AccountDao;
import com.yrd.transfer.utils.JdbcUtils;

/*
 * 事务的使用注意点：
 * 1.service层和dao层的连接对象保持一致
 * 2.每个线程的connection 对象必须前后一致,线程隔离
 * 
 * 常规的解决方案(两步操作)：
 * 第一步：1.传参：将service层的connection对象直接传递到dao层
 * 第二步：2.加锁
 * 常规解决方案的弊端：
 * 1.提高代码耦合度
 * 2.降低程序性能
 */

public class AccountService {

	 public boolean transfer(String outUser, String inUser, int money) {
		 
	        AccountDao ad = new AccountDao();
	        
	        Connection connection=null;
	        try {
	        	connection = JdbcUtils.getConnection();
	        	//1.开启事务
	        	connection.setAutoCommit(false);
	        	
	        	
	            // 转出
	            ad.out(outUser, money);
	            
	            //算术异常：模拟转出成功，转入失败	            
				//int i = 1/0;
	            
	            // 转入
	            ad.in(inUser, money);
	            
	          //2.成功提交
//	            connection.commit();
//	            connection.close();
	            JdbcUtils.commitAndClose(connection);
	            
	        } catch (Exception e) {
	            e.printStackTrace();
	          //2.失败回滚
	           JdbcUtils.rollbackAndClose(connection);
	            return false;
	        }
	        return true;
	    }
}
