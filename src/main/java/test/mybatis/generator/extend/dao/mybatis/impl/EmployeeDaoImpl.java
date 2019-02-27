package test.mybatis.generator.extend.dao.mybatis.impl;

import java.lang.Integer;
import javax.annotation.Resource;
import org.springframework.stereotype.Repository;
import test.mybatis.generator.extend.dao.EmployeeDao;
import test.mybatis.generator.extend.dao.mybatis.mapper.EmployeeMapper;
import test.mybatis.generator.extend.dao.mybatis.vo.Employee;




@Repository
public class EmployeeDaoImpl implements EmployeeDao {

    @Resource
    private EmployeeMapper employeeMapper;

    @Override
    public void save(Employee employee) {
        employeeMapper.insert(employee);
    }

    @Override
    public Employee get(Integer id) {
        return employeeMapper.selectByPrimaryKey(id);
    }

    @Override
    public void update(Employee employee) {
        employeeMapper.updateByPrimaryKey(employee);
    }

    @Override
    public void delete(Integer id) {
        employeeMapper.deleteByPrimaryKey(id);
    }

    ////*******自定义开始********//
    //**********自定义结束*****////

}
