package com.my.testgen.dao;

import java.sql.SQLException;
import javax.annotation.Resource;
import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import com.my.util.mybatis.test.util.DatabaseUtil;
import com.my.util.mybatis.test.util.IntegrationTests;
import com.my.util.mybatis.test.util.ObjectBuilder;
import com.my.util.mybatis.test.util.Util;
import com.my.testgen.dao.mybatis.vo.UserLoginInfo;

@RunWith(SpringJUnit4ClassRunner.class)
@Category(IntegrationTests.class)
@ContextConfiguration(locations = { "/integration_test/db.xml" })
public class UserLoginInfoDaoTest {
    @Resource
    private UserLoginInfoDao  userLoginInfoDao;
    
    @Resource
    private DataSource dataSource ;
    
 @Value("#{configProperties['database.name']}")
    private String databaseName;;
    
    @Before
    public void copyTable() throws SQLException{
      DatabaseUtil.dropTable(dataSource,"u_login_info");
      DatabaseUtil.copyTable(dataSource, databaseName, "u_login_info");
    }
        
    @After
    public  void dropTable() throws SQLException{
      DatabaseUtil.dropTable(dataSource,"u_login_info");
    }
    
    @Test
    public void testCRUD(){
        UserLoginInfo userLoginInfo = ObjectBuilder.build(UserLoginInfo.class,"uid" );
        Assert.assertNull(userLoginInfo.getUid());
        
        Long primaryKey = 1l;
        
        UserLoginInfo userLoginInfo1 = userLoginInfoDao.get(primaryKey);               
        Assert.assertNull(userLoginInfo1);
        
        userLoginInfoDao.save(userLoginInfo);
         Assert.assertEquals(primaryKey,userLoginInfo.getUid());
        
        UserLoginInfo userLoginInfo2 = userLoginInfoDao.get(primaryKey);
        Assert.assertTrue(Util.equals(userLoginInfo2,userLoginInfo ));

        userLoginInfo2.setAccount("account");
        userLoginInfoDao.update(userLoginInfo2);
        UserLoginInfo userLoginInfo3 = userLoginInfoDao.get(primaryKey);
        Assert.assertTrue(Util.equals(userLoginInfo2,userLoginInfo3));
        
        userLoginInfoDao.delete(primaryKey);
        UserLoginInfo userLoginInfo4 = userLoginInfoDao.get(primaryKey);
        Assert.assertNull(userLoginInfo4);
    }
    
    ////***自定义开始****/
    //***自定义结束****////
   

}
