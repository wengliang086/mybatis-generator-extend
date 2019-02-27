package test.mybatis.generator.extend.dao;

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
import test.mybatis.generator.extend.dao.mybatis.vo.Employee;

@RunWith(SpringJUnit4ClassRunner.class)
@Category(IntegrationTests.class)
@ContextConfiguration(locations = { "/integration_test/db.xml" })
public class EmployeeDaoTest {
    @Resource
    private EmployeeDao  employeeDao;
    
    @Resource
    private DataSource dataSource ;
    
    @Value("#{configProperties['database.name']}")
    private String databaseName;;
    
    @Before
    public void copyTable() throws SQLException{
      DatabaseUtil.dropTable(dataSource,"tbl_employee");
      DatabaseUtil.copyTable(dataSource, databaseName, "tbl_employee");
    }
        
    @After
    public  void dropTable() throws SQLException{
      DatabaseUtil.dropTable(dataSource,"tbl_employee");
    }
    
    @Test
    public void testCRUD(){
        Employee employee = ObjectBuilder.build(Employee.class,"id" );
        Assert.assertNull(employee.getId());
        
        Integer primaryKey = 1;
        
        Employee employee1 = employeeDao.get(primaryKey);               
        Assert.assertNull(employee1);
        
        employeeDao.save(employee);
         Assert.assertEquals(primaryKey,employee.getId());
        
        Employee employee2 = employeeDao.get(primaryKey);
        Assert.assertTrue(Util.equals(employee2,employee ));

        employee2.setEmail("email");
        employeeDao.update(employee2);
        Employee employee3 = employeeDao.get(primaryKey);
        Assert.assertTrue(Util.equals(employee2,employee3));
        
        employeeDao.delete(primaryKey);
        Employee employee4 = employeeDao.get(primaryKey);
        Assert.assertNull(employee4);
    }
    
    ////***自定义开始****/
    //***自定义结束****////
   

}

